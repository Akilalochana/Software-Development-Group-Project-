package com.example.arlandmeasuretest33


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.arlandmeasuretest33.R
import com.example.arlandmeasuretest33.BannerSlide

class BannerSlideAdapter(
    private val context: Context,
    private val slides: List<BannerSlide>
) : RecyclerView.Adapter<BannerSlideAdapter.SlideViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.banner_slide_item, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]

        // Set image
        holder.imageView.setImageResource(slide.imageResId)

        // Set texts
        holder.titleText.text = slide.title
        holder.descriptionText.text = slide.description

        // Setup button click if needed
        holder.actionButton.setOnClickListener {
            // Handle button click
        }
    }

    override fun getItemCount(): Int = slides.size

    class SlideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.slide_image)
        val titleText: TextView = itemView.findViewById(R.id.slide_title)
        val descriptionText: TextView = itemView.findViewById(R.id.slide_description)
        val actionButton: Button = itemView.findViewById(R.id.slide_button)
    }
}