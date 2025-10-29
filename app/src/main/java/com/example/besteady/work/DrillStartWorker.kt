package com.besteady.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.besteady.bluetooth.BluetoothClassicManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StartDrillWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val drillId = inputData.getLong("drill_id", -1L)
            Log.d("StartDrillWorker", "🚨 Starting drill with ID: $drillId")

            // Connect to ESP32 (if available)
            val bluetoothManager = BluetoothClassicManager.getInstance(context)
            if (bluetoothManager.isConnected()) {
                bluetoothManager.sendMessage("START_DRILL") // your ESP32 protocol
                Log.d("StartDrillWorker", "✅ Drill command sent to ESP32")
            } else {
                Log.w("StartDrillWorker", "⚠️ ESP32 not connected, drill not started")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("StartDrillWorker", "❌ Error starting drill", e)
            Result.failure()
        }
    }
}
