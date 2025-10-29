package com.besteady.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.besteady.data.AppDatabase
import com.besteady.data.DrillRecord
import com.besteady.databinding.FragmentHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Intent

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

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
        observeDrillHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = DrillHistoryAdapter(mutableListOf()) { selectedDrill ->
            // ✅ when an item is clicked, open a detail screen
            val intent = Intent(requireContext(), DrillDetailActivity::class.java)
            intent.putExtra("DRILL_ID", selectedDrill.id)
            startActivity(intent)
        }

        binding.rvDrillHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }


    private fun observeDrillHistory() {
        val dao = AppDatabase.getDatabase(requireContext()).drillRecordDao()

        lifecycleScope.launch {
            dao.getAllDrills().collectLatest { drills ->
                if (drills.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvDrillHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvDrillHistory.visibility = View.VISIBLE
                    historyAdapter.updateData(drills)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
