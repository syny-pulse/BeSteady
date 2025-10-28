package com.besteady.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.besteady.R
import java.text.SimpleDateFormat
import java.util.Locale

class DrillHistoryAdapter(private val items: List<DrillHistory>) :
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

        holder.tvDrillDate.text = formatter.format(item.date)
        holder.tvDrillDuration.text = "Duration: ${item.duration}"
        holder.tvEvacuationPoints.text = "Evacuation: ${item.safeCount}" // safeCount = evac points
        holder.tvFatalities.text = "Fatalities: ${item.fatalities}"
    }

    override fun getItemCount(): Int = items.size
}
