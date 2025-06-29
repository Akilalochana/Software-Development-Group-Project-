package com.example.arlandmeasuretest33.adapters

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arlandmeasuretest33.R
import com.example.arlandmeasuretest33.models.Garden
import com.google.android.material.button.MaterialButton

class GardenAdapter(
    private var gardens: List<Garden>,
    private val onGardenClickListener: (Garden) -> Unit
) : RecyclerView.Adapter<GardenAdapter.GardenViewHolder>() {

    class GardenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gardenNameText: TextView = itemView.findViewById(R.id.gardenNameText)
        val gardenAreaText: TextView = itemView.findViewById(R.id.gardenAreaText)
        val creationDateText: TextView = itemView.findViewById(R.id.creationDateText)
        val plantsRecyclerView: RecyclerView = itemView.findViewById(R.id.plantsRecyclerView)
        val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.viewDetailsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GardenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_garden, parent, false)
        return GardenViewHolder(view)
    }

    override fun onBindViewHolder(holder: GardenViewHolder, position: Int) {
        val garden = gardens[position]

        // Set garden name
        holder.gardenNameText.text = garden.name

        // Set garden area
        holder.gardenAreaText.text = String.format("%.1f sq.m", garden.areaSize)

        // Set creation date in a user-friendly format
        val dateString = DateFormat.format("MMM dd, yyyy", garden.createdDate).toString()
        holder.creationDateText.text = dateString

        // Setup plants recycler view
        holder.plantsRecyclerView.layoutManager = LinearLayoutManager(
            holder.itemView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val plantAdapter = PlantAdapter(garden.plants)
        holder.plantsRecyclerView.adapter = plantAdapter

        // Set click listener for view details button
        holder.viewDetailsButton.setOnClickListener {
            onGardenClickListener(garden)
        }

        // Also make the entire card clickable
        holder.itemView.setOnClickListener {
            onGardenClickListener(garden)
        }
    }

    override fun getItemCount(): Int = gardens.size

    fun updateGardens(newGardens: List<Garden>) {
        gardens = newGardens
        notifyDataSetChanged()
    }
}
