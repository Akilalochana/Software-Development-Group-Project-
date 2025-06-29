package com.example.arlandmeasuretest33.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.arlandmeasuretest33.R
import com.example.arlandmeasuretest33.models.Plant
import java.text.SimpleDateFormat
import java.util.*

class PlantDetailAdapter(private val plants: List<Plant>) :
    RecyclerView.Adapter<PlantDetailAdapter.PlantDetailViewHolder>() {

    private val TAG = "PlantDetailAdapter"
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class PlantDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantImage: ImageView = itemView.findViewById(R.id.plantImage)
        val plantName: TextView = itemView.findViewById(R.id.plantNameText)
        val plantGrowthPeriod: TextView = itemView.findViewById(R.id.plantGrowthPeriodText)
        val plantExpectedYield: TextView = itemView.findViewById(R.id.plantExpectedYieldText)
        val plantDescription: TextView = itemView.findViewById(R.id.plantDescriptionText)
        val plantedDate: TextView = itemView.findViewById(R.id.plantedDateText)
        val harvestDate: TextView = itemView.findViewById(R.id.harvestDateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_detail, parent, false)
        return PlantDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantDetailViewHolder, position: Int) {
        val plant = plants[position]

        // Set plant name
        holder.plantName.text = plant.name

        // Set growth period
        Log.d(TAG, "Setting growth period for plant ${plant.name}: ${plant.growthPeriodDays} days")
        holder.plantGrowthPeriod.text = "Growth Period: ${plant.growthPeriodDays} days"

        // Set expected yield
        holder.plantExpectedYield.text = "Expected Yield: ${plant.expectedYieldPerPlant} units"

        // Set description
        holder.plantDescription.text = plant.description

        // Set planted date
        plant.plantedDate?.let {
            holder.plantedDate.text = "Planted: ${dateFormat.format(it)}"
        } ?: run {
            holder.plantedDate.text = "Planted: Not set"
        }

        // Set harvest date
        plant.harvestDate?.let {
            holder.harvestDate.text = "Harvest: ${dateFormat.format(it)}"
        } ?: run {
            // Calculate expected harvest date from planted date and growth period if available
            if (plant.plantedDate != null && plant.growthPeriodDays > 0) {
                val calendar = Calendar.getInstance()
                calendar.time = plant.plantedDate
                calendar.add(Calendar.DAY_OF_YEAR, plant.growthPeriodDays)
                holder.harvestDate.text = "Expected Harvest: ${dateFormat.format(calendar.time)}"
            } else {
                holder.harvestDate.text = "Harvest: Not set"
            }
        }

        // Load plant image
        try {
            if (plant.imageRef.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(plant.imageRef)
                    .placeholder(R.drawable.aloe_vera) // Use a placeholder while loading
                    .error(R.mipmap.ic_launcher) // Use a fallback if loading fails
                    .into(holder.plantImage)
                
                Log.d(TAG, "Loading image from URL: ${plant.imageRef}")
            } else {
                // Default image based on plant name
                val iconResId = when {
                    plant.name.contains("aloe", ignoreCase = true) -> R.drawable.aloe_vera
                    plant.name.contains("carrot", ignoreCase = true) -> R.mipmap.ic_launcher
                    plant.name.contains("manioc", ignoreCase = true) -> R.mipmap.ic_launcher
                    plant.name.contains("tomato", ignoreCase = true) -> R.mipmap.ic_launcher
                    plant.name.contains("potato", ignoreCase = true) -> R.mipmap.ic_launcher
                    else -> R.mipmap.ic_launcher // Default icon
                }
                holder.plantImage.setImageResource(iconResId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading plant image: ${e.message}")
            holder.plantImage.setImageResource(R.mipmap.ic_launcher)
        }
    }

    override fun getItemCount(): Int = plants.size
}
