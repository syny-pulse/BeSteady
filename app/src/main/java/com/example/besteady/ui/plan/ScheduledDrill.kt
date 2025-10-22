package com.example.besteady.ui.plan

import java.util.Date

data class ScheduledDrill(
    val id: Long,
    val type: String,
    val scheduledTime: Date,
    val description: String
)