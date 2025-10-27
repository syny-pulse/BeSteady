package com.besteady.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.besteady.R
import com.google.android.material.button.MaterialButton
import java.util.Timer
import java.util.TimerTask

class BluetoothDeviceScanDialog(
    private val bluetoothManager: BluetoothClassicManager,
    private val onDeviceSelected: (BluetoothDevice) -> Unit
) {

    private val TAG = "BluetoothDeviceScanDialog"
    private lateinit var rvDevices: RecyclerView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnScan: MaterialButton
    private lateinit var tvScanStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: BluetoothDeviceAdapter
    private lateinit var dialog: AlertDialog
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private val SCAN_DURATION_MS = 10000L // 10 seconds
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    fun show(context: android.content.Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_bluetooth_devices, null)
        
        rvDevices = view.findViewById(R.id.rvBluetoothDevices)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnScan = view.findViewById(R.id.btnScan)
        tvScanStatus = view.findViewById(R.id.tvScanStatus)
        progressBar = view.findViewById(R.id.progressBar)

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .setOnDismissListener { stopScan(context) }
            .create()

        setupRecyclerView(context)
        setupClickListeners(context)

        // Load paired devices initially
        loadPairedDevices(context)
        
        dialog.show()
    }

    private fun setupRecyclerView(context: android.content.Context) {
        adapter = BluetoothDeviceAdapter(mutableListOf()) { device ->
            Log.d(TAG, "Device selected: ${getDeviceName(device)}")
            stopScan(context)
            dialog.dismiss()
            
            // Show connecting toast
            Toast.makeText(context, "Connecting to ${getDeviceName(device)}...", Toast.LENGTH_SHORT).show()
            
            // Call the callback
            onDeviceSelected(device)
        }

        rvDevices.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@BluetoothDeviceScanDialog.adapter
        }
    }

    private fun setupClickListeners(context: android.content.Context) {
        btnCancel.setOnClickListener {
            stopScan(context)
            dialog.dismiss()
        }

        btnScan.setOnClickListener {
            if (isScanning) {
                stopScan(context)
            } else {
                startScan(context)
            }
        }

        // Initial scan shows paired devices
        loadPairedDevices(context)
    }
    
    private fun loadPairedDevices(context: Context) {
        if (!hasPermissions(context)) {
            tvScanStatus.text = "⚠️ Permissions required"
            btnScan.isEnabled = false
            return
        }
        
        try {
            val bondedDevices: Set<BluetoothDevice> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter?.bondedDevices ?: emptySet()
                } else {
                    emptySet()
                }
            } else {
                @Suppress("DEPRECATION")
                bluetoothAdapter?.bondedDevices ?: emptySet()
            }

            discoveredDevices.clear()
            discoveredDevices.addAll(bondedDevices)
            
            if (discoveredDevices.isEmpty()) {
                tvScanStatus.text = "No devices found. Tap SCAN to discover nearby devices"
            } else {
                tvScanStatus.text = "Found ${discoveredDevices.size} device(s)"
                updateDeviceList()
            }
            
            btnScan.isEnabled = true
        } catch (e: SecurityException) {
            Log.e(TAG, "Error loading paired devices: ${e.message}", e)
            tvScanStatus.text = "❌ Permission denied"
            btnScan.isEnabled = false
        }
    }
    
    private fun startScan(context: Context) {
        if (!hasPermissions(context)) {
            tvScanStatus.text = "⚠️ Bluetooth permissions required"
            Toast.makeText(context, "Please grant Bluetooth permissions in Settings", Toast.LENGTH_LONG).show()
            return
        }
        
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            tvScanStatus.text = "❌ Bluetooth is not enabled"
            Toast.makeText(context, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }
        
        isScanning = true
        btnScan.text = "STOP SCAN"
        btnScan.isEnabled = true
        progressBar.visibility = View.VISIBLE
        tvScanStatus.text = "🔍 Scanning for devices...\n(This will take about 10 seconds)"
        
        // Register broadcast receiver for device discovery
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (ActivityCompat.checkSelfPermission(
                                    context ?: return,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            } else null
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        
                        device?.let {
                            if (!discoveredDevices.contains(it)) {
                                discoveredDevices.add(it)
                                handler.post {
                                    tvScanStatus.text = "Found: ${getDeviceName(it)}"
                                    updateDeviceList()
                                }
                            }
                        }
                    }
                    
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        handler.post {
                            tvScanStatus.text = "🔍 Discovering nearby devices..."
                            discoveredDevices.clear()
                        }
                    }
                    
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        handler.post {
                            context?.let {
                                onScanComplete(it)
                            }
                        }
                    }
                }
            }
        }
        
        context.registerReceiver(receiver, filter)
        
        // Start discovery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.startDiscovery()
            }
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter.startDiscovery()
        }
        
        // Auto-stop after SCAN_DURATION_MS
        handler.postDelayed({
            if (isScanning) {
                stopScan(context)
            }
            context.unregisterReceiver(receiver)
        }, SCAN_DURATION_MS)
        
        // Store receiver reference for cleanup
        bluetoothAdapter.startDiscovery()
    }
    
    private fun stopScan(context: Context) {
        if (!isScanning) return
        
        isScanning = false
        btnScan.text = "SCAN"
        progressBar.visibility = View.GONE
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            } else {
                @Suppress("DEPRECATION")
                bluetoothAdapter?.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
        
        tvScanStatus.text = "Scan stopped. Found ${discoveredDevices.size} device(s)"
    }
    
    private fun onScanComplete(context: Context) {
        isScanning = false
        btnScan.text = "SCAN"
        progressBar.visibility = View.GONE
        
        val statusText = if (discoveredDevices.isEmpty()) {
            "❌ No devices found nearby"
        } else {
            "✅ Found ${discoveredDevices.size} device(s)"
        }
        
        tvScanStatus.text = statusText
    }
    
    private fun updateDeviceList() {
        adapter.updateDevices(discoveredDevices.toList())
    }
    
    private fun getDeviceName(device: BluetoothDevice): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    dialog.context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name ?: device.address
            } else {
                device.address
            }
        } else {
            @Suppress("DEPRECATION")
            device.name ?: device.address
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ViewHolder and Adapter
    private class BluetoothDeviceAdapter(
        private var devices: List<BluetoothDevice>,
        private val onDeviceClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

        class DeviceViewHolder(
            private val view: View,
            private val onDeviceClick: (BluetoothDevice) -> Unit
        ) : RecyclerView.ViewHolder(view) {
            
            private val tvDeviceName: TextView = view.findViewById(R.id.tvDeviceName)
            private val tvDeviceAddress: TextView = view.findViewById(R.id.tvDeviceAddress)
            private var device: BluetoothDevice? = null

            init {
                view.setOnClickListener {
                    device?.let { onDeviceClick(it) }
                }
            }

            fun bind(d: BluetoothDevice) {
                device = d
                val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            view.context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        d.name ?: "Unknown Device"
                    } else {
                        "Unknown Device"
                    }
                } else {
                    @Suppress("DEPRECATION")
                    d.name ?: "Unknown Device"
                }
                
                tvDeviceName.text = name
                tvDeviceAddress.text = d.address
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth_device, parent, false)
            return DeviceViewHolder(view, onDeviceClick)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }

        override fun getItemCount(): Int = devices.size

        fun updateDevices(newDevices: List<BluetoothDevice>) {
            devices = newDevices
            notifyDataSetChanged()
        }
    }
}
