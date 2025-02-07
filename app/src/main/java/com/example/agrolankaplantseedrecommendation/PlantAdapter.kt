package com.example.agrolankaplantseedrecommendation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlantAdapter(private val plants: List<Plant>) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.plant_image)
        val nameTextView: TextView = view.findViewById(R.id.plant_name)
        val descriptionTextView: TextView = view.findViewById(R.id.plant_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.imageView.setImageResource(plant.imageResource)
        holder.nameTextView.text = plant.name
        holder.descriptionTextView.text = plant.description
    }

    override fun getItemCount() = plants.size
}