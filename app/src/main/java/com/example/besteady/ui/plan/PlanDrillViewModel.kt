package com.example.besteady.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.besteady.data.DrillRecordRepository
import com.besteady.data.toDrillRecord
import kotlinx.coroutines.launch

class PlanDrillViewModel(
    private val repository: DrillRecordRepository
) : ViewModel() {

    fun scheduleDrill(drill: ScheduledDrill) {
        viewModelScope.launch {
            repository.insertDrill(drill.toDrillRecord())
        }
    }
}
