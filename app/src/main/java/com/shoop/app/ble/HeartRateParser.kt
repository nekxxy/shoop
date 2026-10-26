package com.shoop.app.ble

/**
 * Parser for the Bluetooth SIG Heart Rate Measurement characteristic (0x2A37).
 *
 * Wire format (public Bluetooth SIG specification):
 *  byte 0      flags
 *              bit 0: HR value format (0 = uint8, 1 = uint16)
 *              bit 1-2: sensor contact status
 *              bit 3: energy expended present (uint16 follows HR)
 *              bit 4: RR-interval present (one or more uint16, 1/1024 s units)
 *  byte 1..    heart rate (uint8 or uint16 LE)
 *  ...         optional energy expended (uint16 LE)
 *  ...         optional RR intervals (n x uint16 LE, most recent last)
 */
object HeartRateParser {

    data class Measurement(
        val heartRateBpm: Int,
        val rrIntervalsMs: List<Int>,
    )

    fun parse(value: ByteArray): Measurement? {
        if (value.isEmpty()) return null
        val flags = value[0].toInt()
        val hrIsUint16 = flags and 0x01 != 0
        val hasEnergyExpended = flags and 0x08 != 0
        val hasRrIntervals = flags and 0x10 != 0

        var offset = 1
        val heartRate: Int
        if (hrIsUint16) {
            if (value.size < offset + 2) return null
            heartRate = (value[offset].toInt() and 0xFF) or
                ((value[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
        } else {
            if (value.size < offset + 1) return null
            heartRate = value[offset].toInt() and 0xFF
            offset += 1
        }

        if (hasEnergyExpended) {
            if (value.size < offset + 2) return null
            offset += 2
        }

        val rrIntervals = mutableListOf<Int>()
        if (hasRrIntervals) {
            while (value.size >= offset + 2) {
                val raw1024 = (value[offset].toInt() and 0xFF) or
                    ((value[offset + 1].toInt() and 0xFF) shl 8)
                // Convert 1/1024 s units to whole milliseconds.
                rrIntervals.add((raw1024 * 1000) / 1024)
                offset += 2
            }
        }

        if (heartRate <= 0) return null
        return Measurement(heartRate, rrIntervals)
    }
}
