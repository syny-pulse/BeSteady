package com.besteady.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.besteady.databinding.SheetDrillDetailsBinding
import com.besteady.data.DrillRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrillDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: SheetDrillDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var record: RecordUi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        record = RecordUi(
            startTime = args.getLong(ARG_START_TIME),
            duration = args.getLong(ARG_DURATION),
            emergencyCallTime = args.getLong(ARG_EMERGENCY_CALL).takeIf { args.containsKey(ARG_EMERGENCY_CALL) },
            policeArrivalTime = args.getLong(ARG_POLICE_ARRIVAL).takeIf { args.containsKey(ARG_POLICE_ARRIVAL) },
            evacuationTime = args.getLong(ARG_EVACUATION_TIME).takeIf { args.containsKey(ARG_EVACUATION_TIME) },
            evacuationPoints = args.getInt(ARG_EVACUATION_POINTS),
            fatalities = args.getInt(ARG_FATALITIES),
            additionalNotes = args.getString(ARG_NOTES) ?: ""
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetDrillDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateFmt = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        val durationText = formatDuration(record.duration)

        binding.tvDate.text = dateFmt.format(Date(record.startTime))
        binding.tvDuration.text = durationText
        binding.tvEmergencyCall.text = record.emergencyCallTime?.let { formatDuration(it) } ?: "—"
        binding.tvPoliceArrival.text = record.policeArrivalTime?.let { formatDuration(it) } ?: "—"
        binding.tvEvacuation.text = record.evacuationTime?.let { formatDuration(it) } ?: "—"
        binding.tvEvacuationPoints.text = record.evacuationPoints.toString()
        binding.tvFatalities.text = record.fatalities.toString()
        binding.tvNotes.text = if (record.additionalNotes.isBlank()) "No additional notes" else record.additionalNotes
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    data class RecordUi(
        val startTime: Long,
        val duration: Long,
        val emergencyCallTime: Long?,
        val policeArrivalTime: Long?,
        val evacuationTime: Long?,
        val evacuationPoints: Int,
        val fatalities: Int,
        val additionalNotes: String
    )

    companion object {
        private const val ARG_START_TIME = "arg_start_time"
        private const val ARG_DURATION = "arg_duration"
        private const val ARG_EMERGENCY_CALL = "arg_emergency_call"
        private const val ARG_POLICE_ARRIVAL = "arg_police_arrival"
        private const val ARG_EVACUATION_TIME = "arg_evacuation_time"
        private const val ARG_EVACUATION_POINTS = "arg_evacuation_points"
        private const val ARG_FATALITIES = "arg_fatalities"
        private const val ARG_NOTES = "arg_notes"

        fun newInstance(record: com.besteady.data.DrillRecord): DrillDetailsBottomSheet {
            return DrillDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_START_TIME, record.startTime)
                    putLong(ARG_DURATION, record.duration)
                    record.emergencyCallTime?.let { putLong(ARG_EMERGENCY_CALL, it) }
                    record.policeArrivalTime?.let { putLong(ARG_POLICE_ARRIVAL, it) }
                    record.evacuationTime?.let { putLong(ARG_EVACUATION_TIME, it) }
                    putInt(ARG_EVACUATION_POINTS, record.evacuationPoints)
                    putInt(ARG_FATALITIES, record.fatalities)
                    putString(ARG_NOTES, record.additionalNotes)
                }
            }
        }
    }
}

