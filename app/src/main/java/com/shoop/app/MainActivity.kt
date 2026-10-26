package com.shoop.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AppViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results surfaced through connection state if scanning fails */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        requestBlePermissions()
        setContent { ShoopApp(viewModel) }
    }

    private fun requestBlePermissions() {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        permissionLauncher.launch(needed)
    }
}
