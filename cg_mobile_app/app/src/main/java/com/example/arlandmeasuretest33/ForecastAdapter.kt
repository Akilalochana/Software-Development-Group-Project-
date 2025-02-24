package com.example.arlandmeasuretest33

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ForecastAdapter : RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder>() {
    private var forecasts: List<DayForecast> = emptyList()

    class ForecastViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayText: TextView = view.findViewById(R.id.dayText)
        val forecastIcon: ImageView = view.findViewById(R.id.forecastIcon)
        val forecastTemp: TextView = view.findViewById(R.id.forecastTemp)
        val rainChanceText: TextView = view.findViewById(R.id.rainChanceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ForecastViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forecast, parent, false)
        return ForecastViewHolder(view)
    }

    override fun onBindViewHolder(holder: ForecastViewHolder, position: Int) {
        val forecast = forecasts[position]
        val date = Date(forecast.dt * 1000)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        // Set day name (Today for first item, then day names for rest)
        holder.dayText.text = when (position) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> dayFormat.format(date)
        }

        // Set temperature range
        holder.forecastTemp.text = "${forecast.main.temp_min.toInt()}° — ${forecast.main.temp_max.toInt()}°"

        // Set weather icon
        val iconUrl = "https://openweathermap.org/img/w/${forecast.weather[0].icon}.png"
        Glide.with(holder.itemView.context)
            .load(iconUrl)
            .into(holder.forecastIcon)

        // Set rain chance if available
        holder.rainChanceText.text = ""  // Remove placeholder text
    }

    override fun getItemCount() = forecasts.size

    fun updateForecasts(newForecasts: List<DayForecast>) {
        println("ForecastAdapter: Updating forecasts with ${newForecasts.size} items")
        forecasts = newForecasts
        notifyDataSetChanged()
    }
}