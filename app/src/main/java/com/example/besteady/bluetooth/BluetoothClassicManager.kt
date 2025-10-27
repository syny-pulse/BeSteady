package com.besteady.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothClassicManager(private val context: Context) {

    private val TAG = "BluetoothClassicManager"

    // SPP UUID for ESP32
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val TARGET_DEVICE_NAME = "FlameDetector_ESP32"

    private var bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedDevice: BluetoothDevice? = null

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Callbacks
    private var connectionCallback: ((Boolean, String?) -> Unit)? = null
    private var messageCallback: ((String) -> Unit)? = null

    companion object {
        private var instance: BluetoothClassicManager? = null
        
        fun getInstance(context: Context): BluetoothClassicManager {
            return instance ?: synchronized(this) {
                instance ?: BluetoothClassicManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Check if Bluetooth is supported and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Set callback for connection status changes
     */
    fun setConnectionCallback(callback: (Boolean, String?) -> Unit) {
        connectionCallback = callback
    }

    /**
     * Set callback for incoming messages
     */
    fun setMessageCallback(callback: (String) -> Unit) {
        messageCallback = callback
    }

    /**
     * Connect to a specific device
     */
    fun connectToDevice(device: BluetoothDevice) {
        ioScope.launch {
            try {
                // Check if already connected to this device
                if (bluetoothSocket?.isConnected == true && connectedDevice?.address == device.address) {
                    Log.d(TAG, "Already connected to ${getDeviceName(device)}")
                    connectionCallback?.invoke(true, "Already connected")
                    return@launch
                }

                // Disconnect existing connection
                disconnect()

                // Create socket and connect
                Log.d(TAG, "Connecting to ${getDeviceName(device)} (${device.address})...")
                
                bluetoothSocket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        device.createRfcommSocketToServiceRecord(SPP_UUID)
                    } else {
                        Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                        connectionCallback?.invoke(false, "Bluetooth permission not granted")
                        return@launch
                    }
                } else {
                    @Suppress("DEPRECATION")
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                }

                bluetoothSocket?.let { socket ->
                    // Connect
                    socket.connect()
                    connectedDevice = device
                    
                    val deviceName = getDeviceName(device)
                    Log.d(TAG, "Connected to $deviceName")
                    connectionCallback?.invoke(true, "Connected to $deviceName")
                    
                    // Start reading messages
                    startReading()
                }

            } catch (e: IOException) {
                Log.e(TAG, "Failed to connect: ${e.message}", e)
                connectionCallback?.invoke(false, "Connection failed: ${e.message}")
            } catch (e: SecurityException) {
                Log.e(TAG, "Bluetooth permission denied: ${e.message}", e)
                connectionCallback?.invoke(false, "Bluetooth permission denied")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during connection: ${e.message}", e)
                connectionCallback?.invoke(false, "Connection error: ${e.message}")
            }
        }
    }
    
    /**
     * Helper function to get device name safely
     */
    private fun getDeviceName(device: BluetoothDevice): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.name ?: device.address
            } else {
                device.address
            }
        } else {
            @Suppress("DEPRECATION")
            device.name ?: device.address
        }
    }

    /**
     * Connect to ESP32 device
     */
    fun connect() {
        if (!isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth not available or not enabled")
            connectionCallback?.invoke(false, "Bluetooth not enabled")
            return
        }

        ioScope.launch {
            try {
                // Find the target device
                val targetDevice = findTargetDevice()
                if (targetDevice == null) {
                    Log.e(TAG, "Target device not found: $TARGET_DEVICE_NAME")
                    connectionCallback?.invoke(false, "Device not found: $TARGET_DEVICE_NAME")
                    return@launch
                }

                // Check if already connected to this device
                if (bluetoothSocket?.isConnected == true && connectedDevice?.address == targetDevice.address) {
                    Log.d(TAG, "Already connected to ${targetDevice.name}")
                    return@launch
                }

                // Disconnect existing connection
                disconnect()

                // Create socket and connect
                Log.d(TAG, "Connecting to ${targetDevice.name} (${targetDevice.address})...")
                
                bluetoothSocket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        targetDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                    } else {
                        Log.e(TAG, "BLUETOOTH_CONNECT permission not granted")
                        connectionCallback?.invoke(false, "Bluetooth permission not granted")
                        return@launch
                    }
                } else {
                    @Suppress("DEPRECATION")
                    targetDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                }

                bluetoothSocket?.let { socket ->
                    // Connect
                    socket.connect()
                    connectedDevice = targetDevice
                    
                    Log.d(TAG, "Connected to ${targetDevice.name}")
                    connectionCallback?.invoke(true, "Connected to ${targetDevice.name}")
                    
                    // Start reading messages
                    startReading()
                }

            } catch (e: IOException) {
                Log.e(TAG, "Failed to connect: ${e.message}", e)
                connectionCallback?.invoke(false, "Connection failed: ${e.message}")
                attemptReconnect()
            } catch (e: SecurityException) {
                Log.e(TAG, "Bluetooth permission denied: ${e.message}", e)
                connectionCallback?.invoke(false, "Bluetooth permission denied")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during connection: ${e.message}", e)
                connectionCallback?.invoke(false, "Connection error: ${e.message}")
            }
        }
    }

    /**
     * Find the target ESP32 device
     */
    private suspend fun findTargetDevice(): BluetoothDevice? {
        return withContext(Dispatchers.IO) {
            try {
                if (!isBluetoothAvailable()) return@withContext null

                // Get bonded devices
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

                // Look for target device
                bondedDevices.find { 
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            it.name == TARGET_DEVICE_NAME
                        } else {
                            false
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        it.name == TARGET_DEVICE_NAME
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding device: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Start reading messages from the connected device
     */
    private fun startReading() {
        ioScope.launch {
            val socket = bluetoothSocket ?: return@launch
            val inputStream: InputStream = try {
                socket.inputStream
            } catch (e: IOException) {
                Log.e(TAG, "Failed to get input stream: ${e.message}", e)
                connectionCallback?.invoke(false, "Failed to get input stream")
                return@launch
            }

            val buffer = ByteArray(1024)
            var bytes: Int

            while (socket.isConnected) {
                try {
                    bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        val message = String(buffer, 0, bytes).trim()
                        Log.d(TAG, "Received message: $message")
                        messageCallback?.invoke(message)
                    }
                } catch (e: IOException) {
                    if (socket.isConnected) {
                        Log.e(TAG, "Error reading message: ${e.message}", e)
                    } else {
                        Log.d(TAG, "Connection closed")
                    }
                    connectionCallback?.invoke(false, "Connection lost")
                    attemptReconnect()
                    break
                }
            }
        }
    }

    /**
     * Send message to the connected device
     */
    fun sendMessage(message: String) {
        ioScope.launch {
            try {
                val socket = bluetoothSocket
                if (socket == null || !socket.isConnected) {
                    Log.e(TAG, "Not connected, cannot send message")
                    return@launch
                }

                val outputStream: OutputStream = socket.outputStream
                outputStream.write(message.toByteArray())
                outputStream.flush()
                Log.d(TAG, "Sent message: $message")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to send message: ${e.message}", e)
                connectionCallback?.invoke(false, "Failed to send message")
                attemptReconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error sending message: ${e.message}", e)
            }
        }
    }

    /**
     * Disconnect from the device
     */
    fun disconnect() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            connectedDevice = null
            Log.d(TAG, "Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting: ${e.message}", e)
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    /**
     * Get connected device name
     */
    fun getConnectedDeviceName(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                connectedDevice?.name
            } else {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            connectedDevice?.name
        }
    }

    /**
     * Attempt to reconnect with exponential backoff
     */
    private fun attemptReconnect() {
        ioScope.launch {
            var retryDelay = 1000L // Start with 1 second
            val maxDelay = 30000L // Max 30 seconds
            val retryAttempts = 5 // Try 5 times

            for (attempt in 1..retryAttempts) {
                delay(retryDelay)
                Log.d(TAG, "Reconnection attempt $attempt/$retryAttempts")
                
                try {
                    connect()
                    if (isConnected()) {
                        Log.d(TAG, "Reconnected successfully")
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Reconnection attempt $attempt failed: ${e.message}")
                }
                
                // Exponential backoff
                retryDelay = minOf(retryDelay * 2, maxDelay)
            }
            
            Log.e(TAG, "Failed to reconnect after $retryAttempts attempts")
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disconnect()
        ioScope.cancel()
        instance = null
    }
}
