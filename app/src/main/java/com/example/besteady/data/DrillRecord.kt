package com.besteady.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "drill_records")
data class DrillRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val stopTime: Long,
    val duration: Long, // Duration in milliseconds
    val emergencyCallTime: Long? = null, // Time to emergency call in milliseconds
    val policeArrivalTime: Long? = null, // Time to police arrival in milliseconds
    val evacuationTime: Long? = null, // Time to complete evacuation in milliseconds
    val evacuationPoints: Int = 0,
    val fatalities: Int = 0,
    val additionalNotes: String = "",
    val wasAutoStarted: Boolean = false // Whether drill was auto-started by ESP32
)

