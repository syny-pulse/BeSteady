package com.besteady.ui.plan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class ScheduledDrillsAdapter(
    private val drills: List<ScheduledDrill>
) : RecyclerView.Adapter<ScheduledDrillsAdapter.DrillViewHolder>() {

    class DrillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvType: TextView = itemView.findViewById(R.id.tvDrillType)
        val tvTime: TextView = itemView.findViewById(R.id.tvDrillTime)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDrillDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scheduled_drill, parent, false)
        return DrillViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrillViewHolder, position: Int) {
        val drill = drills[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())

        holder.tvType.text = "Type: ${drill.type}"
        holder.tvTime.text = "Time: ${dateFormat.format(drill.scheduledTime)}"
        holder.tvDescription.text = drill.description.ifEmpty { "No description" }
    }

    override fun getItemCount(): Int = drills.size
}