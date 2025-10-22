package com.besteady.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.besteady.R
import java.text.SimpleDateFormat
import java.util.Locale

class DrillHistoryAdapter(
    private val historyList: List<DrillHistory>
) : RecyclerView.Adapter<DrillHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDrillDate)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDrillDuration)
        val tvSafeCount: TextView = itemView.findViewById(R.id.tvSafeCount)
        val tvFatalities: TextView = itemView.findViewById(R.id.tvFatalities)
        val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drill_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        holder.tvDate.text = dateFormat.format(history.date)
        holder.tvDuration.text = "Duration: ${history.duration}"
        holder.tvSafeCount.text = "Safe: ${history.safeCount} people"
        holder.tvFatalities.text = "Fatalities: ${history.fatalities}"
        holder.tvNotes.text = "Notes: ${history.notes}"
    }

    override fun getItemCount(): Int = historyList.size
}