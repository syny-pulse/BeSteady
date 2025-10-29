
package com.besteady.data

import com.example.besteady.ui.plan.ScheduledDrill

fun ScheduledDrill.toDrillRecord(): DrillRecord {
    return DrillRecord(
        startTime = scheduledTime.time,
        stopTime = 0L, // not yet completed
        duration = 0L,
        additionalNotes = description,
        wasAutoStarted = (type == "Random")
    )
}
