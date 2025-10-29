package com.besteady.ui.drill

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.besteady.data.AppDatabase
import com.besteady.data.DrillRecord
import com.besteady.data.DrillRecordDao
import com.besteady.databinding.DialogStopDrillBinding
import com.besteady.R
import kotlinx.coroutines.launch

class StopDrillDialog : DialogFragment() {

    private var _binding: DialogStopDrillBinding? = null
    private val binding get() = _binding!!

    // Drill data from StartDrillFragment
    var drillStartTime: Long = 0
    var drillStopTime: Long = 0
    var drillDuration: Long = 0
    var emergencyCallTime: Long? = null
    var policeArrivalTime: Long? = null
    var evacuationTime: Long? = null
    var wasAutoStarted: Boolean = false

    private lateinit var drillRecordDao: DrillRecordDao

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogStopDrillBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireActivity())
            .setView(binding.root)

        // Initialize database
        val database = AppDatabase.getDatabase(requireContext())
        drillRecordDao = database.drillRecordDao()

        setupClickListeners()

        return builder.create()
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            submitDrillReport()
        }
    }

    private fun submitDrillReport() {
        val evacuationPoints = binding.etEvacuationPoints.text.toString().toIntOrNull() ?: 0
        val fatalities = binding.etFatalities.text.toString().toIntOrNull() ?: 0
        val additionalNotes = binding.etAdditionalNotes.text.toString()

        // Create DrillRecord
        val drillRecord = DrillRecord(
            startTime = drillStartTime,
            stopTime = drillStopTime,
            duration = drillDuration,
            emergencyCallTime = emergencyCallTime,
            policeArrivalTime = policeArrivalTime,
            evacuationTime = evacuationTime,
            evacuationPoints = evacuationPoints,
            fatalities = fatalities,
            additionalNotes = additionalNotes,
            wasAutoStarted = wasAutoStarted
        )

        // Save to database safely
        lifecycleScope.launch {
            try {
                drillRecordDao.insertDrill(drillRecord)
                context?.let {
                    Toast.makeText(it, "✅ Drill report saved successfully!", Toast.LENGTH_SHORT).show()
                }

                // Reset drill in parent fragment
                resetDrillInParentFragment()

                // Dismiss safely (avoids crash if fragment is detached)
                dismissAllowingStateLoss()
            } catch (e: Exception) {
                context?.let {
                    Toast.makeText(it, "❌ Error saving drill report: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun resetDrillInParentFragment() {
        val fragments = parentFragmentManager.fragments
        for (fragment in fragments) {
            if (fragment is StartDrillFragment) {
                fragment.resetDrill()
                break
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
