package com.besteady.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.besteady.work.StartDrillWorker

object DrillScheduler {

    fun scheduleDrill(context: Context, delayMillis: Long, drillId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<StartDrillWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("drill_id" to drillId))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
