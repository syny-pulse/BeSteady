package com.besteady.ui.drill

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.besteady.databinding.DialogStopDrillBinding
import com.besteady.R
class StopDrillDialog : DialogFragment() {

    private var _binding: DialogStopDrillBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogStopDrillBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireActivity())
            .setView(binding.root)

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

        // In real app, this would save to database
        Toast.makeText(requireContext(), "Drill report submitted!", Toast.LENGTH_SHORT).show()

        // Navigate back to start drill fragment and reset drill
        val startDrillFragment = parentFragmentManager.findFragmentById(R.id.navigation_start_drill)
        if (startDrillFragment is StartDrillFragment) {
            startDrillFragment.resetDrill()
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}