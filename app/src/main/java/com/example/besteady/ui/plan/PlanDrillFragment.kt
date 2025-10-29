package com.example.besteady.ui.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.besteady.R
import com.besteady.databinding.FragmentPlanDrillBinding
import com.besteady.utils.DrillScheduler
import java.util.*

class PlanDrillFragment : Fragment() {

    private var _binding: FragmentPlanDrillBinding? = null
    private val binding get() = _binding!!

    private val scheduledDrills = mutableListOf<ScheduledDrill>()
    private lateinit var drillsAdapter: ScheduledDrillsAdapter
    private var selectedDateTime = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanDrillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupUI()
    }

    private fun setupRecyclerView() {
        drillsAdapter = ScheduledDrillsAdapter(scheduledDrills)
        binding.rvScheduledDrills.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScheduledDrills.adapter = drillsAdapter
    }

    private fun setupUI() {
        binding.radioGroupDrillType.setOnCheckedChangeListener { _, checkedId ->
            binding.layoutScheduledTime.visibility =
                if (checkedId == R.id.radioScheduled) View.VISIBLE else View.GONE
        }

        binding.btnSelectDateTime.setOnClickListener { showDatePicker() }

        binding.btnScheduleDrill.setOnClickListener { scheduleDrill() }

        updateDateTimeDisplay()
    }

    private fun showDatePicker() {
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Drill Date")
            .build()

        datePicker.addOnPositiveButtonClickListener { millis ->
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            selectedDateTime.set(Calendar.YEAR, cal.get(Calendar.YEAR))
            selectedDateTime.set(Calendar.MONTH, cal.get(Calendar.MONTH))
            selectedDateTime.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
            showTimePicker()
        }

        datePicker.show(parentFragmentManager, "date_picker")
    }

    private fun showTimePicker() {
        val timePicker = com.google.android.material.timepicker.MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_12H)
            .setTitleText("Select Drill Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedDateTime.set(Calendar.MINUTE, timePicker.minute)
            updateDateTimeDisplay()
        }

        timePicker.show(parentFragmentManager, "time_picker")
    }

    private fun updateDateTimeDisplay() {
        val dateTime = android.text.format.DateFormat.format("MMM dd, yyyy 'at' hh:mm a", selectedDateTime.time)
        binding.tvSelectedDateTime.text = dateTime
    }

    private fun scheduleDrill() {
        val description = binding.etDrillDescription.text.toString()
        val type = if (binding.radioScheduled.isChecked) "Scheduled" else "Random"

        val drillTime = if (type == "Scheduled") selectedDateTime.time else getRandomTimeToday()

        val drill = ScheduledDrill(
            id = System.currentTimeMillis(),
            type = type,
            scheduledTime = drillTime,
            description = description
        )

        scheduledDrills.add(drill)
        drillsAdapter.notifyItemInserted(scheduledDrills.size - 1)

        // ✅ Schedule the actual drill start
        val delay = drill.scheduledTime.time - System.currentTimeMillis()
        if (delay > 0) {
            DrillScheduler.scheduleDrill(requireContext(), delay, drill.id)
        }

        Toast.makeText(requireContext(), "Drill scheduled for ${binding.tvSelectedDateTime.text}", Toast.LENGTH_SHORT).show()
        binding.etDrillDescription.text?.clear()
    }

    private fun getRandomTimeToday(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, (8..17).random())
        cal.set(Calendar.MINUTE, (0..59).random())
        return cal.time
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
