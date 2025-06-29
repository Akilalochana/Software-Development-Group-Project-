package com.example.arlandmeasuretest33.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.arlandmeasuretest33.R
import com.example.arlandmeasuretest33.models.Garden
import java.text.SimpleDateFormat
import java.util.*

class ModernGardenAdapter(
    private val gardens: List<Garden>,
    private val onGardenClick: (Garden) -> Unit,
    private val onDeleteClick: (Garden) -> Unit
) : RecyclerView.Adapter<ModernGardenAdapter.GardenViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Array of background colors for plant icons
    private val plantColors = arrayOf(
        "#4CAF50", // Green
        "#FF9800", // Orange
        "#2196F3", // Blue
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#F44336", // Red
        "#009688"  // Teal
    )

    private val TAG = "ModernGardenAdapter"
    
    class GardenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gardenNameText: TextView = itemView.findViewById(R.id.gardenNameText)
        val gardenDescriptionText: TextView = itemView.findViewById(R.id.gardenDescriptionText)
        val creationDateText: TextView = itemView.findViewById(R.id.creationDateText)
        val gardenAreaText: TextView = itemView.findViewById(R.id.gardenAreaText)
        val primaryPlantText: TextView = itemView.findViewById(R.id.primaryPlantText)
        val plantInitialText: TextView = itemView.findViewById(R.id.plantInitialText)
        val plantIconBackground: CardView = itemView.findViewById(R.id.plantIconBackground)
        val plantImage: ImageView = itemView.findViewById(R.id.plantImage)
        val deleteGardenButton: ImageView = itemView.findViewById(R.id.deleteGardenButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GardenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_garden_modern, parent, false)
        return GardenViewHolder(view)
    }

    override fun onBindViewHolder(holder: GardenViewHolder, position: Int) {
        val garden = gardens[position]
        
        // Set garden name
        holder.gardenNameText.text = garden.name
        
        // Set garden description - if we don't have a description, use a default message
        val description = "Garden plot for growing plants"  // Default description
        holder.gardenDescriptionText.text = description
        
        // Set creation date
        holder.creationDateText.text = shortDateFormat.format(garden.createdDate)
        
        // Set garden area
        holder.gardenAreaText.text = String.format("%.3f sq.m", garden.areaSize)
        
        // Set primary plant info if available
        val primaryPlant = garden.plants.firstOrNull()
        if (primaryPlant != null) {
            holder.primaryPlantText.text = primaryPlant.name
            
            // Get first letter of plant name for the icon (as fallback)
            val initial = primaryPlant.name.firstOrNull()?.toString() ?: "P"
            holder.plantInitialText.text = initial
            
            // Set a color based on the plant name (just for visual variety)
            val colorIndex = Math.abs(primaryPlant.name.hashCode()) % plantColors.size
            holder.plantIconBackground.setCardBackgroundColor(android.graphics.Color.parseColor(plantColors[colorIndex]))
            
            // Load plant image from imageRef URL
            try {
                if (primaryPlant.imageRef.isNotEmpty()) {
                    // Show image and hide the text
                    holder.plantInitialText.visibility = View.GONE
                    
                    // Load image from URL using Glide
                    Log.d(TAG, "Loading plant image: ${primaryPlant.imageRef}")
                    Glide.with(holder.itemView.context)
                        .load(primaryPlant.imageRef)
                        .placeholder(R.drawable.aloe_vera) // Default placeholder while loading
                        .error(R.drawable.aloe_vera) // Error fallback
                        .centerCrop()
                        .into(holder.plantImage)
                } else {
                    // No image URL, show the initial and hide the image
                    holder.plantInitialText.visibility = View.VISIBLE
                    holder.plantImage.setImageResource(0) // Clear the image
                    Log.d(TAG, "No image URL for plant: ${primaryPlant.name}")
                }
            } catch (e: Exception) {
                // In case of error, show the initial
                Log.e(TAG, "Error loading plant image: ${e.message}")
                holder.plantInitialText.visibility = View.VISIBLE
            }
        } else {
            // No plants in this garden
            holder.primaryPlantText.text = "No plants yet"
            holder.plantInitialText.text = "+"
            holder.plantInitialText.visibility = View.VISIBLE
            holder.plantImage.setImageResource(0) // Clear any image
            holder.plantIconBackground.setCardBackgroundColor(android.graphics.Color.parseColor("#9E9E9E")) // Gray
        }
        
        // Set click listener for the entire item
        holder.itemView.setOnClickListener {
            onGardenClick(garden)
        }
        
        // Set click listener for the delete button
        holder.deleteGardenButton.setOnClickListener {
            onDeleteClick(garden)
        }
    }

    override fun getItemCount(): Int {
        return gardens.size
    }
}
