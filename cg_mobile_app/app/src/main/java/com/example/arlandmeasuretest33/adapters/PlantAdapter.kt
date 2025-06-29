package com.example.arlandmeasuretest33.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.arlandmeasuretest33.R
import com.example.arlandmeasuretest33.models.Plant

class PlantAdapter(private var plants: List<Plant>) :
    RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantIcon: ImageView = itemView.findViewById(R.id.plantIcon)
        val plantName: TextView = itemView.findViewById(R.id.plantName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_garden_plant, parent, false)
        return PlantViewHolder(view)
    }    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]

        // Set plant name
        holder.plantName.text = plant.name

        try {
            // Check if we have an imageRef and it's not empty
            if (plant.imageRef.isNotEmpty()) {
                // Use Glide to load the image from the URL
                com.bumptech.glide.Glide.with(holder.itemView.context)
                    .load(plant.imageRef)
                    .placeholder(R.mipmap.ic_launcher) // Placeholder while loading
                    .error(R.mipmap.ic_launcher) // Image to show on error
                    .into(holder.plantIcon)
                
                Log.d("PlantAdapter", "Loading image from URL: ${plant.imageRef}")
            } else {
                // No image reference, use a fallback based on plant name
                val iconResId = when {
                    plant.name.contains("aloe", ignoreCase = true) -> R.drawable.aloe_vera
                    plant.name.contains("carrot", ignoreCase = true) -> R.mipmap.ic_launcher
                    plant.name.contains("manioc", ignoreCase = true) -> R.mipmap.ic_launcher
                    plant.name.contains("tomato", ignoreCase = true) -> R.mipmap.ic_launcher
                    plant.name.contains("potato", ignoreCase = true) -> R.mipmap.ic_launcher
                    else -> R.mipmap.ic_launcher // Default icon
                }
                holder.plantIcon.setImageResource(iconResId)
            }
        } catch (e: Exception) {
            Log.e("PlantAdapter", "Error loading plant icon: ${e.message}")
            // In case of any error, use the app icon as a fallback
            try {
                holder.plantIcon.setImageResource(R.mipmap.ic_launcher)
            } catch (e: Exception) {
                Log.e("PlantAdapter", "Failed to set fallback icon: ${e.message}")
            }
        }
    }

    override fun getItemCount(): Int = plants.size

    fun updatePlants(newPlants: List<Plant>) {
        plants = newPlants
        notifyDataSetChanged()
    }
}
