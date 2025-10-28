package com.besteady

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.besteady.databinding.ActivityMainBinding
import androidx.lifecycle.ViewModelProvider
import com.besteady.bluetooth.BluetoothClassicViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    // Request Bluetooth permissions
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            it + Manifest.permission.POST_NOTIFICATIONS
        } else {
            it
        }
    }
    
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge: draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView = binding.bottomNavigation
        val navController = findNavController(R.id.nav_host_fragment)

        // No action bar - theme is NoActionBar and we skip setupActionBarWithNavController
        navView.setupWithNavController(navController)
        
        // Loader example usage (hidden by default)
        val loader = binding.appLoader
        // loader visibility can be controlled as needed

        // Setup Bluetooth FAB
        setupBluetoothButton()
        
        // Request Bluetooth permissions
        requestBluetoothPermissions()
    }
    
    private fun setupBluetoothButton() {
        binding.fabBluetooth.setOnClickListener {
            if (!hasPermissions()) {
                android.widget.Toast.makeText(this, "⚠️ Bluetooth permissions required", android.widget.Toast.LENGTH_LONG).show()
                requestBluetoothPermissions()
                return@setOnClickListener
            }
            
            val bluetoothManager = com.besteady.bluetooth.BluetoothClassicManager.getInstance(this)
            
            // Check if already connected
            if (bluetoothManager.isConnected()) {
                android.widget.Toast.makeText(this, "✅ Already connected to ${bluetoothManager.getConnectedDeviceName()}", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val dialog = com.besteady.bluetooth.BluetoothDeviceScanDialog(bluetoothManager) { device ->
                // Get device name safely
                val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        device.name ?: device.address
                    } else {
                        device.address
                    }
                } else {
                    @Suppress("DEPRECATION")
                    device.name ?: device.address
                }
                
                // Show connecting message
                android.widget.Toast.makeText(this, "🔌 Connecting to $deviceName...", android.widget.Toast.LENGTH_SHORT).show()
                
                // Set up connection callbacks
                bluetoothManager.setConnectionCallback { isConnected, message ->
                    runOnUiThread {
                        when {
                            isConnected -> {
                                android.widget.Toast.makeText(this, "✅ Connected successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            message?.contains("already", true) == true -> {
                                android.widget.Toast.makeText(this, "✅ Already connected", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            message?.contains("failed", true) == true -> {
                                android.widget.Toast.makeText(this, "❌ Connection failed: $message", android.widget.Toast.LENGTH_LONG).show()
                            }
                            message?.contains("denied", true) == true -> {
                                android.widget.Toast.makeText(this, "❌ Permission denied: $message", android.widget.Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                android.widget.Toast.makeText(this, "⚠️ $message", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                
                // Connect to selected device
                bluetoothManager.connectToDevice(device)
                
                // Also update ViewModel
                val viewModel = ViewModelProvider(this)[BluetoothClassicViewModel::class.java]
                viewModel.connect()
            }
            dialog.show(this)
        }
    }
    
    private fun requestBluetoothPermissions() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                bluetoothPermissions,
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions granted, initialize Bluetooth connection
            initializeBluetooth()
        }
    }
    
    private fun hasPermissions(): Boolean {
        return bluetoothPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions granted, initialize Bluetooth connection
                initializeBluetooth()
            } else {
                // Permissions denied
                // Handle gracefully - app can still function without Bluetooth
            }
        }
    }
    
    private fun initializeBluetooth() {
        // Get ViewModel instance
        val viewModel = ViewModelProvider(this)[BluetoothClassicViewModel::class.java]
        
        // Check if Bluetooth is available and connect
        if (viewModel.isBluetoothAvailable()) {
            viewModel.connect()
        }
    }
}