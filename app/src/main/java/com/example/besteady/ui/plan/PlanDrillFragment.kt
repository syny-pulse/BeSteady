package com.example.besteady.ui.plan

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.besteady.databinding.FragmentPlanDrillBinding
import com.besteady.R
import java.util.Calendar
import java.util.Date

class PlanDrillFragment : Fragment() {

    private var _binding: FragmentPlanDrillBinding? = null
    private val binding get() = _binding!!

    private val scheduledDrills = mutableListOf<ScheduledDrill>()
    private lateinit var drillsAdapter: ScheduledDrillsAdapter

    private var selectedDateTime: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlanDrillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        updateDateTimeDisplay()
    }

    private fun setupRecyclerView() {
        drillsAdapter = ScheduledDrillsAdapter(scheduledDrills)
        binding.rvScheduledDrills.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = drillsAdapter
        }
    }

    private fun setupClickListeners() {
        // Use standard RadioGroup listener
        val onCheckedChangeListener =
            binding.radioGroupDrillType.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
                when (checkedId) {
                    R.id.radioScheduled -> binding.layoutScheduledTime.visibility = View.VISIBLE
                    R.id.radioRandom -> binding.layoutScheduledTime.visibility = View.GONE
                }
            }

        binding.btnSelectDateTime.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnScheduleDrill.setOnClickListener {
            scheduleDrill()
        }
    }

    private fun showDateTimePicker() {
        val currentDate = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        updateDateTimeDisplay()
                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    false
                ).show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeDisplay() {
        val dateTimeString = android.text.format.DateFormat
            .format("MMM dd, yyyy 'at' hh:mm a", selectedDateTime.time)
        binding.tvSelectedDateTime.text = dateTimeString
    }

    private fun scheduleDrill() {
        val isScheduled = binding.radioScheduled.isChecked
        val description = binding.etDrillDescription.text.toString()

        val drill = if (isScheduled) {
            ScheduledDrill(
                id = System.currentTimeMillis(),
                type = "Scheduled",
                scheduledTime = selectedDateTime.time,
                description = description
            )
        } else {
            ScheduledDrill(
                id = System.currentTimeMillis(),
                type = "Random",
                scheduledTime = getRandomTimeToday(),
                description = description
            )
        }

        scheduledDrills.add(drill)
        drillsAdapter.notifyItemInserted(scheduledDrills.size - 1)

        Toast.makeText(requireContext(), "Drill scheduled successfully!", Toast.LENGTH_SHORT).show()

        // Clear form
        binding.etDrillDescription.text?.clear()
    }

    private fun getRandomTimeToday(): Date {
        val calendar = Calendar.getInstance()
        val randomHour = (8..17).random()
        val randomMinute = (0..59).random()

        calendar.set(Calendar.HOUR_OF_DAY, randomHour)
        calendar.set(Calendar.MINUTE, randomMinute)
        calendar.set(Calendar.SECOND, 0)

        return calendar.time
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
