package com.besteady.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.besteady.databinding.FragmentHistoryBinding
import java.util.Date

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val drillHistory = mutableListOf<DrillHistory>()
    private lateinit var historyAdapter: DrillHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadSampleData()
    }

    private fun setupRecyclerView() {
        historyAdapter = DrillHistoryAdapter(drillHistory)
        binding.rvDrillHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun loadSampleData() {
        // Sample data for demonstration
        drillHistory.addAll(listOf(
            DrillHistory(
                id = 1,
                date = Date(System.currentTimeMillis() - 86400000), // Yesterday
                duration = "00:02:30",
                safeCount = 15,
                fatalities = 0,
                notes = "Regular monthly drill"
            ),
            DrillHistory(
                id = 2,
                date = Date(System.currentTimeMillis() - 172800000), // 2 days ago
                duration = "00:03:15",
                safeCount = 12,
                fatalities = 0,
                notes = "Unexpected drill test"
            ),
            DrillHistory(
                id = 3,
                date = Date(System.currentTimeMillis() - 259200000), // 3 days ago
                duration = "00:01:45",
                safeCount = 18,
                fatalities = 0,
                notes = "Quick response drill"
            )
        ))

        historyAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class DrillHistory(
    val id: Long,
    val date: Date,
    val duration: String,
    val safeCount: Int,
    val fatalities: Int,
    val notes: String
)