package com.example.arlandmeasuretest33.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.arlandmeasuretest33.R
import com.example.arlandmeasuretest33.models.PlantCategory
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore

class PlantCatalogAdapter(
    private var plantList: List<PlantCategory>,
    private val onItemClick: (PlantCategory) -> Unit
) : RecyclerView.Adapter<PlantCatalogAdapter.PlantViewHolder>() {
    private val TAG = "PlantCatalogAdapter"
    private val db = FirebaseFirestore.getInstance()

    class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plantImage: ImageView = itemView.findViewById(R.id.plantImage)
        val plantName: TextView = itemView.findViewById(R.id.plantName)
        val plantCategory: TextView = itemView.findViewById(R.id.plantCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_category, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plantList[position]
        
        // Use drawable resources instead of loading from database
        val drawableResource = getPlantDrawableResource(plant.name)
        holder.plantImage.setImageResource(drawableResource)
        
        holder.plantName.text = plant.name
        holder.plantCategory.text = plant.category
        
        holder.itemView.setOnClickListener {
            onItemClick(plant)
        }
    }

    private fun getPlantDrawableResource(plantName: String): Int {
        // Using hardcoded drawable images instead of database URLs as requested.
        // This ensures consistent image loading for the plant information page,
        // while the PlantInformationDetailActivity will still use database images.
        return when (plantName.lowercase()) {
            "bitter melon" -> R.drawable.img_bitter_melon
            "winged bean", "winged_bean" -> R.drawable.img_winged_bean
            "red spinach" -> R.drawable.img_red_spinach
            "beetroot" -> R.drawable.img_beetroot
            "brinjal" -> R.drawable.img_brinjal
            "carrots", "carrot" -> R.drawable.img_carrot
            "cabbage" -> R.drawable.img_cabbage
            "leeks", "leek" -> R.drawable.img_leeks
            "potato" -> R.drawable.img_potato
            "onion" -> R.drawable.img_onion
            "manioc" -> R.drawable.img_manioc
            "taro" -> R.drawable.img_taro
            "eggplant", "long purple eggplant" -> R.drawable.img_long_purple_eggplant
            "pumpkin" -> R.drawable.img_pumpkin
            "knolkhol", "knol khol" -> R.drawable.img_knol_khol
            "drumstick", "drumsticks" -> R.drawable.img_drumsticks
            "tomato" -> R.drawable.img_tomato
            "okra" -> R.drawable.img_okra
            "radish" -> R.drawable.img_radish
            "capsicum" -> R.drawable.img_capsicum
            else -> R.drawable.ic_plant // Default image for unknown plants
        }
    }

    // Keep these methods for reference but they won't be used for the main list view
    // Everything below is kept as reference only

    private fun loadPlantImage(plantName: String, district: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }

    private fun tryAllDistrictsForImage(plantName: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }
    
    private fun findPlantWithCaseInsensitiveSearch(plantName: String, district: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }
    
    private fun tryFallbackDistricts(plantName: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }
    
    private fun checkSpecialCasePlants(plantName: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }
    
    private fun loadImageWithGlide(imageUrl: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }

    private fun trySpecificDistrictsForProblemPlants(plantName: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }
    
    private fun checkCompletionAndFallback(districtsChecked: Int, totalDistricts: Int, imageFound: Boolean, plantName: String, imageView: ImageView) {
        // Method kept for reference but not used - using hardcoded drawables instead
    }

    override fun getItemCount(): Int = plantList.size

    fun updateList(newList: List<PlantCategory>) {
        plantList = newList
        notifyDataSetChanged()
    }
} 