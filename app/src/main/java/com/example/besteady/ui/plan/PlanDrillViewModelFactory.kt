package com.example.besteady.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.besteady.data.DrillRecordRepository

class PlanDrillViewModelFactory(
    private val repository: DrillRecordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanDrillViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlanDrillViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
