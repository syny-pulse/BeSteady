package com.besteady.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.besteady.R
import com.besteady.data.DrillRecord
import java.text.SimpleDateFormat
import java.util.*

class DrillHistoryAdapter(private val items: MutableList<DrillRecord>) :
    RecyclerView.Adapter<DrillHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDrillDate: TextView = view.findViewById(R.id.tvDrillDate)
        val tvDrillDuration: TextView = view.findViewById(R.id.tvDrillDuration)
        val tvEvacuationPoints: TextView = view.findViewById(R.id.tvEvacuationPoints)
        val tvFatalities: TextView = view.findViewById(R.id.tvFatalities)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drill_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        holder.tvDrillDate.text = formatter.format(Date(item.startTime))
        holder.tvDrillDuration.text = "Duration: ${formatDuration(item.duration)}"
        holder.tvEvacuationPoints.text = "Evacuation: ${item.evacuationPoints}"
        holder.tvFatalities.text = "Fatalities: ${item.fatalities}"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<DrillRecord>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
