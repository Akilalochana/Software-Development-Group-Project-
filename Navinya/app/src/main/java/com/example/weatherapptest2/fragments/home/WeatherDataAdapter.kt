package com.example.weatherapptest2.fragments.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapptest2.data.CurrentLocation
import com.example.weatherapptest2.data.WeatherData
import com.example.weatherapptest2.databinding.ItemContainerCurrentLocationBinding

class WeatherDataAdapter (
    private val onLocationClicked: () -> Unit
) : RecyclerView.Adapter<WeatherDataAdapter.CurrentLocationViewHolder>(){




    private val weatherData = mutableListOf<WeatherData>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<WeatherData>){
        weatherData.clear()
        weatherData.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrentLocationViewHolder {
        return CurrentLocationViewHolder(
            ItemContainerCurrentLocationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CurrentLocationViewHolder, position: Int) {
        holder.bind(weatherData[position] as CurrentLocation)

    }

    override fun getItemCount(): Int {
        return weatherData.size
    }

    inner class CurrentLocationViewHolder(
        private val binding: ItemContainerCurrentLocationBinding
    ) : RecyclerView.ViewHolder(binding.root){
        fun bind(currentLocation: CurrentLocation){
            with(binding){
                textCurrentDate.text = currentLocation.date
                textCurrentLocation.text = currentLocation.location
                imageCurrentLocation.setOnClickListener{onLocationClicked()}
                textCurrentLocation.setOnClickListener{onLocationClicked()}
            }
        }
    }
}