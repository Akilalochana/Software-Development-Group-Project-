package com.example.arlandmeasuretest33

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PdfPageAdapter(private val bitmaps: List<Bitmap>) : RecyclerView.Adapter<PdfPageAdapter.PdfViewHolder>() {

    class PdfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.pdfImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.imageView.setImageBitmap(bitmaps[position])
    }

    override fun getItemCount(): Int = bitmaps.size
}