package com.besteady.ui.history

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.besteady.data.AppDatabase
import com.besteady.data.DrillRecord
import com.besteady.databinding.ActivityDrillDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DrillDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrillDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrillDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drillId = intent.getLongExtra("DRILL_ID", -1)
        if (drillId != -1L) {
            loadDrillDetails(drillId)
        }
    }

    private fun loadDrillDetails(drillId: Long) {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(this@DrillDetailActivity).drillRecordDao()
            val record = dao.getDrillById(drillId)
            record?.let { showDetails(it) }
        }
    }

    private fun showDetails(record: DrillRecord) {
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        binding.tvDrillDate.text = "Date: ${formatter.format(Date(record.startTime))}"
        binding.tvDuration.text = "Duration: ${record.duration / 1000}s"
        binding.tvEvacuationPoints.text = "Evacuation Points: ${record.evacuationPoints}"
        binding.tvFatalities.text = "Fatalities: ${record.fatalities}"
        binding.tvNotes.text = "Notes: ${record.additionalNotes}"
    }
}
