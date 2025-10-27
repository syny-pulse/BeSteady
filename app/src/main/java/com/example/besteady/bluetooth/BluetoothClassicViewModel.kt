package com.besteady.bluetooth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FireEventType {
    FIRE_DETECTED,
    FIRE_CLEARED,
    PING,
    UNKNOWN
}

data class FireEvent(
    val type: FireEventType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class BluetoothClassicViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BluetoothClassicVM"
    
    // Bluetooth Manager
    private val bluetoothManager = BluetoothClassicManager.getInstance(application)

    // Connection State
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Incoming Messages
    private val _fireEvents = MutableStateFlow<List<FireEvent>>(emptyList())
    val fireEvents: StateFlow<List<FireEvent>> = _fireEvents.asStateFlow()

    // Latest Fire Event
    private val _latestFireEvent = MutableStateFlow<FireEvent?>(null)
    val latestFireEvent: StateFlow<FireEvent?> = _latestFireEvent.asStateFlow()

    init {
        setupCallbacks()
    }

    private fun setupCallbacks() {
        // Connection callback
        bluetoothManager.setConnectionCallback { isConnected, message ->
            viewModelScope.launch {
                _connectionState.value = when {
                    isConnected -> {
                        Log.d(TAG, "Connected to ESP32")
                        ConnectionState.Connected
                    }
                    message != null -> {
                        Log.e(TAG, "Connection error: $message")
                        ConnectionState.Error(message)
                    }
                    else -> {
                        Log.d(TAG, "Disconnected from ESP32")
                        ConnectionState.Disconnected
                    }
                }
            }
        }

        // Message callback
        bluetoothManager.setMessageCallback { message ->
            viewModelScope.launch {
                val fireEvent = parseMessage(message)
                handleFireEvent(fireEvent)
            }
        }
    }

    /**
     * Connect to ESP32 device
     */
    fun connect() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            bluetoothManager.connect()
        }
    }

    /**
     * Disconnect from ESP32 device
     */
    fun disconnect() {
        bluetoothManager.disconnect()
    }

    /**
     * Send message to ESP32
     */
    fun sendMessage(message: String) {
        bluetoothManager.sendMessage(message)
    }

    /**
     * Check if Bluetooth is available
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothManager.isBluetoothAvailable()
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return bluetoothManager.isConnected()
    }

    /**
     * Parse incoming message to FireEvent
     */
    private fun parseMessage(message: String): FireEvent {
        return when {
            message.contains("🚨", ignoreCase = true) && message.contains("Fire detected", ignoreCase = true) -> {
                Log.d(TAG, "Fire detected message received")
                FireEvent(FireEventType.FIRE_DETECTED, message)
            }
            message.contains("✅", ignoreCase = true) && message.contains("Fire cleared", ignoreCase = true) -> {
                Log.d(TAG, "Fire cleared message received")
                FireEvent(FireEventType.FIRE_CLEARED, message)
            }
            message.contains("PING", ignoreCase = true) -> {
                Log.d(TAG, "Ping message received")
                FireEvent(FireEventType.PING, message)
            }
            else -> {
                Log.d(TAG, "Unknown message: $message")
                FireEvent(FireEventType.UNKNOWN, message)
            }
        }
    }

    /**
     * Handle fire events
     */
    private fun handleFireEvent(event: FireEvent) {
        _latestFireEvent.value = event
        _fireEvents.value = _fireEvents.value + event

        // Keep only last 50 events
        if (_fireEvents.value.size > 50) {
            _fireEvents.value = _fireEvents.value.takeLast(50)
        }

        when (event.type) {
            FireEventType.FIRE_DETECTED -> {
                Log.d(TAG, "🔥 FIRE DETECTED! Auto-starting drill...")
                // Event will be observed by StartDrillFragment to auto-start drill
            }
            FireEventType.FIRE_CLEARED -> {
                Log.d(TAG, "✅ FIRE CLEARED")
                // Event will be observed by StartDrillFragment to stop buzzer
            }
            FireEventType.PING -> {
                Log.d(TAG, "📡 PING received (connection healthy)")
            }
            FireEventType.UNKNOWN -> {
                Log.d(TAG, "❓ Unknown message: ${event.message}")
            }
        }
    }

    /**
     * Clear fire events
     */
    fun clearEvents() {
        _fireEvents.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        // Don't cleanup - maintain persistent connection
        // bluetoothManager.cleanup()
    }
}
