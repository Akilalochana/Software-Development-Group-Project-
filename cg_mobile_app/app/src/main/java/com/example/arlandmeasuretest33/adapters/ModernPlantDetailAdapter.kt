package com.example.arlandmeasuretest33.adapters

import android.graphics.Color
import android.text.SpannableString
import android.text.style.UnderlineSpan
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
import com.example.arlandmeasuretest33.models.Plant
import java.text.SimpleDateFormat
import java.util.*

class ModernPlantDetailAdapter(
    private val plants: List<Plant>,
    private val onDeletePlant: (Plant) -> Unit
) : RecyclerView.Adapter<ModernPlantDetailAdapter.PlantDetailViewHolder>() {

    private val TAG = "ModernPlantDetailAdapter"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Array of background colors for plant icons
    private val plantColors = arrayOf(
        "#4CAF50", // Green (used for most plants)
        "#FF9800", // Orange
        "#2196F3", // Blue
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#F44336", // Red
        "#009688"  // Teal
    )

    class PlantDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantInitialText: TextView = itemView.findViewById(R.id.plantInitialText)
        val plantIconBackground: CardView = itemView.findViewById(R.id.plantIconBackground)
        val plantImageView: ImageView = itemView.findViewById(R.id.plantImageView)
        val plantNameText: TextView = itemView.findViewById(R.id.plantNameText)
        val plantGrowthPeriodBadge: TextView = itemView.findViewById(R.id.plantGrowthPeriodBadge)
        val plantExpectedYieldText: TextView = itemView.findViewById(R.id.plantExpectedYieldText)
        val plantDescriptionText: TextView = itemView.findViewById(R.id.plantDescriptionText)
        val readMoreText: TextView = itemView.findViewById(R.id.readMoreText)
        val plantedDateText: TextView = itemView.findViewById(R.id.plantedDateText)
        val harvestDateText: TextView = itemView.findViewById(R.id.harvestDateText)
        val deletePlantButton: View = itemView.findViewById(R.id.deletePlantButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_detail_modern, parent, false)
        return PlantDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantDetailViewHolder, position: Int) {
        val plant = plants[position]

        // Set plant name
        holder.plantNameText.text = plant.name
        
        // Set plant initial for the icon
        val initial = plant.name.firstOrNull()?.toString() ?: "P"
        holder.plantInitialText.text = initial
        
        // Set up delete button
        holder.deletePlantButton.setOnClickListener {
            Log.d(TAG, "Delete button clicked for plant: ${plant.name} (id: ${plant.id})")
            onDeletePlant(plant)
        }
        
        // Set consistent colors based on plant type like in the mockup
        val colorToUse = when {
            plant.name.equals("carrot", ignoreCase = true) -> "#4CAF50" // Green  
            plant.name.equals("lettuce", ignoreCase = true) -> "#4CAF50" // Green
            plant.name.equals("tomato", ignoreCase = true) -> "#4CAF50" // Green
            plant.name.equals("bell pepper", ignoreCase = true) -> "#4CAF50" // Green
            else -> {
                // Fallback to a hash-based color for variety with other plants
                val colorIndex = Math.abs(plant.name.hashCode()) % plantColors.size
                plantColors[colorIndex]
            }
        }
        holder.plantIconBackground.setCardBackgroundColor(Color.parseColor(colorToUse))
        
        // Load plant image from Firebase if available
        if (!plant.imageRef.isNullOrEmpty()) {
            try {
                Log.d(TAG, "Loading image for ${plant.name} from URL: ${plant.imageRef}")
                
                // Normalize the URL if needed (some URLs might be stored without http/https prefix)
                val imageUrl = if (plant.imageRef.startsWith("http")) {
                    plant.imageRef
                } else if (plant.imageRef.startsWith("//")) {
                    "https:${plant.imageRef}"
                } else if (plant.imageRef.startsWith("www.")) {
                    "https://${plant.imageRef}"
                } else {
                    plant.imageRef // Use as-is, might be a Firebase Storage path
                }
                
                // Use a simpler Glide implementation for fewer issues
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_garden) // Show during loading
                    .error(R.drawable.ic_garden) // Show if error
                    .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.drawable.Drawable>() {
                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable,
                            transition: com.bumptech.glide.request.transition.Transition<in android.graphics.drawable.Drawable>?
                        ) {
                            // Image loaded successfully
                            holder.plantImageView.setImageDrawable(resource)
                            holder.plantImageView.visibility = View.VISIBLE
                            holder.plantInitialText.visibility = View.GONE
                            Log.d(TAG, "Successfully loaded image for ${plant.name}")
                        }
                        
                        override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                            // This is called when the target is cleared
                            holder.plantImageView.visibility = View.GONE
                            holder.plantInitialText.visibility = View.VISIBLE
                        }
                        
                        override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                            // This is called if the load fails
                            Log.e(TAG, "Failed to load image for ${plant.name}")
                            holder.plantImageView.visibility = View.GONE
                            holder.plantInitialText.visibility = View.VISIBLE
                        }
                    })
                
                // Initially hide the initial text until we know if the image loads
                holder.plantInitialText.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up image loading for ${plant.name}: ${e.message}")
                // Show initial text if image fails to load
                holder.plantImageView.visibility = View.GONE
                holder.plantInitialText.visibility = View.VISIBLE
            }
        } else {
            // No image URL, use the initial text
            holder.plantImageView.visibility = View.GONE
            holder.plantInitialText.visibility = View.VISIBLE
            Log.d(TAG, "No image URL available for ${plant.name}")
        }

        // Set growth period badge
        Log.d(TAG, "Setting growth period for plant ${plant.name}: ${plant.growthPeriodDays} days")
        holder.plantGrowthPeriodBadge.text = "${plant.growthPeriodDays} days"

        // Set expected yield with kg suffix
        holder.plantExpectedYieldText.text = "${plant.expectedYieldPerPlant} g"

        // Set description (truncated if too long)
        val description = plant.description
        if (description.length > 120) {
            holder.plantDescriptionText.text = description.substring(0, 120) + "..."
            holder.readMoreText.visibility = View.VISIBLE
            
            // Make "Read more" clickable and underlined
            val spannableString = SpannableString("Read more")
            spannableString.setSpan(UnderlineSpan(), 0, spannableString.length, 0)
            holder.readMoreText.text = spannableString
            
            holder.readMoreText.setOnClickListener {
                // Show full description when clicked
                holder.plantDescriptionText.text = description
                holder.readMoreText.visibility = View.GONE
            }
        } else {
            holder.plantDescriptionText.text = description
            holder.readMoreText.visibility = View.GONE
        }

        // Set planting date (showing as dateAdded in Garden details)
        val plantedDate = plant.plantedDate
        if (plantedDate != null) {
            holder.plantedDateText.text = dateFormat.format(plantedDate)
        } else {
            // Try to get the current date if planted date is not available
            val currentDate = Date()
            holder.plantedDateText.text = dateFormat.format(currentDate)
            Log.d(TAG, "No planted date for ${plant.name}, using current date")
        }

        // Set harvest date - calculate by adding growthPeriod to dateAdded (plantedDate)
        val effectivePlantedDate = plantedDate ?: Date() // Use current date if plantedDate is null
        
        if (plant.growthPeriodDays > 0) {
            // Calculate harvest date by adding growth period to planted date
            val calendar = Calendar.getInstance()
            calendar.time = effectivePlantedDate
            calendar.add(Calendar.DAY_OF_YEAR, plant.growthPeriodDays)
            holder.harvestDateText.text = dateFormat.format(calendar.time)
            Log.d(TAG, "Calculated harvest date for ${plant.name}: ${dateFormat.format(calendar.time)} " +
                      "(planted: ${dateFormat.format(effectivePlantedDate)}, growth: ${plant.growthPeriodDays} days)")
        } else {
            // If no growth period is set, just use a default of 90 days
            val calendar = Calendar.getInstance()
            calendar.time = effectivePlantedDate
            calendar.add(Calendar.DAY_OF_YEAR, 90) // Default 90-day growth period
            holder.harvestDateText.text = dateFormat.format(calendar.time)
            Log.d(TAG, "Using default growth period (90 days) for ${plant.name}")
        }
    }

    override fun getItemCount(): Int {
        return plants.size
    }
}



