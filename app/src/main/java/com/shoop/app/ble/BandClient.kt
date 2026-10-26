package com.shoop.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * BLE connection to a heart-rate broadcasting band (WHOOP with HR broadcast
 * enabled, or any band/chest strap exposing the standard Heart Rate Service).
 *
 * Uses only public Bluetooth SIG services:
 *   Heart Rate Service 0x180D -> Heart Rate Measurement 0x2A37 (notify)
 *   Battery Service    0x180F -> Battery Level 0x2A19 (read)
 */
@SuppressLint("MissingPermission") // permissions are requested in MainActivity before use
class BandClient(private val context: Context) {

    sealed interface ConnectionState {
        data object Idle : ConnectionState
        data object Scanning : ConnectionState
        data object Connecting : ConnectionState
        data class Connected(val deviceName: String) : ConnectionState
        data class Streaming(val deviceName: String) : ConnectionState
        data class Error(val message: String) : ConnectionState
    }

    data class DiscoveredDevice(val name: String, val address: String, val rssi: Int)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connection: StateFlow<ConnectionState> = _connection

    private val _discovered = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discovered: StateFlow<List<DiscoveredDevice>> = _discovered

    private val _heartRate = MutableStateFlow<HeartRateParser.Measurement?>(null)
    val heartRate: StateFlow<HeartRateParser.Measurement?> = _heartRate

    private val _battery = MutableStateFlow<Int?>(null)
    val battery: StateFlow<Int?> = _battery

    private var gatt: BluetoothGatt? = null
    private var hrCharacteristic: BluetoothGattCharacteristic? = null
    private var batteryCharacteristic: BluetoothGattCharacteristic? = null

    private val bluetoothAdapter get() =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    // ---------- scanning ----------

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: return
            val current = _discovered.value.toMutableList()
            val idx = current.indexOfFirst { it.address == result.device.address }
            val entry = DiscoveredDevice(name, result.device.address, result.rssi)
            if (idx >= 0) current[idx] = entry else current.add(entry)
            _discovered.value = current.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            _connection.value = ConnectionState.Error("Scan failed (code $errorCode). Is Bluetooth on?")
        }
    }

    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connection.value = ConnectionState.Error("Bluetooth is off or unavailable")
            return
        }
        _discovered.value = emptyList()
        _connection.value = ConnectionState.Scanning
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(null, settings, scanCallback)
        scope.launch {
            delay(30_000)
            if (_connection.value == ConnectionState.Scanning) stopScan()
        }
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connection.value == ConnectionState.Scanning) _connection.value = ConnectionState.Idle
    }

    // ---------- connection ----------

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connection.value = ConnectionState.Connecting
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connection.value = ConnectionState.Idle
                    _heartRate.value = null
                    gatt?.close()
                    gatt = null
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            hrCharacteristic = null
            batteryCharacteristic = null
            for (service in g.services) {
                for (ch in service.characteristics) {
                    when (ch.uuid) {
                        HR_MEASUREMENT_UUID -> hrCharacteristic = ch
                        BATTERY_LEVEL_UUID -> batteryCharacteristic = ch
                    }
                }
            }
            val hr = hrCharacteristic
            if (hr == null) {
                _connection.value = ConnectionState.Error(
                    "No Heart Rate Service on this device. " +
                        "Enable HR broadcast in your band's official app first."
                )
                g.disconnect()
                return
            }
            _connection.value = ConnectionState.Connected(g.device.name ?: "band")
            g.setCharacteristicNotification(hr, true)
            val ccc = hr.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (ccc != null) {
                // GATT operations must be serialized: the battery read is issued
                // from onDescriptorWrite once the bus is free, not here.
                if (Build.VERSION.SDK_INT >= 33) {
                    g.writeDescriptor(ccc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    ccc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(ccc)
                }
            } else {
                readBatteryLevel(g)
            }
            _connection.value = ConnectionState.Streaming(g.device.name ?: "band")
        }

        @Deprecated("API < 33 callback")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            handleHr(characteristic.value)
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) = handleHr(value)

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == CLIENT_CHARACTERISTIC_CONFIG_UUID) readBatteryLevel(g)
        }

        /** Battery level is optional: -1 means the band does not expose the standard Battery Service. */
        private fun readBatteryLevel(g: BluetoothGatt) {
            val ch = batteryCharacteristic
            if (ch == null) {
                _battery.value = -1
                return
            }
            val started = g.readCharacteristic(ch)
            if (!started) _battery.value = -1
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            if (characteristic.uuid == BATTERY_LEVEL_UUID) {
                _battery.value =
                    if (status == BluetoothGatt.GATT_SUCCESS && value.isNotEmpty()) value[0].toInt() and 0xFF else -1
            }
        }

        @Deprecated("API < 33 callback")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (Build.VERSION.SDK_INT < 33 && characteristic.uuid == BATTERY_LEVEL_UUID) {
                @Suppress("DEPRECATION")
                characteristic.value?.firstOrNull()?.let { _battery.value = it.toInt() and 0xFF }
            }
        }
    }

    private fun handleHr(value: ByteArray?) {
        val parsed = value?.let { HeartRateParser.parse(it) } ?: return
        _heartRate.value = parsed
    }

    fun connect(address: String) {
        stopScan()
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            _connection.value = ConnectionState.Error("Device $address not found")
            return
        }
        _connection.value = ConnectionState.Connecting
        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    companion object {
        val HR_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_UUID: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
