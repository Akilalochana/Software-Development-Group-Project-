package com.example.arlandmeasuretest33

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arlandmeasuretest33.models.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class WateringCycleActivity : AppCompatActivity() {
    
    private lateinit var wateringDaysAdapter: WateringDaysAdapter
    private val openWeatherApiKey = "7545a2bc25e9b63cf4b8a41f7c22d03a"
    private lateinit var selectedLocation: String
    private lateinit var rainNotificationText: TextView
    private lateinit var loadingIndicator: android.widget.ProgressBar
    private var isRainingToday = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watering_cycle)
        
        // Get the saved location
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        selectedLocation = sharedPreferences.getString("SELECTED_LOCATION", "Colombo") ?: "Colombo"
        
        // Initialize UI elements
        findViewById<TextView>(R.id.locationText).text = selectedLocation
        rainNotificationText = findViewById(R.id.rainNotificationText)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        
        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            onBackPressed()
        }
        
        // Setup RecyclerView
        setupWateringDaysRecyclerView()
        
        // Fetch weather data for watering cycle
        fetchWeatherData()
    }
    
    private fun setupWateringDaysRecyclerView() {
        wateringDaysAdapter = WateringDaysAdapter(emptyList()) { dayIndex, isRainyDay ->
            onDaySelected(dayIndex, isRainyDay)
        }
        val recyclerView = findViewById<RecyclerView>(R.id.wateringForecastRecyclerView)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@WateringCycleActivity)
            adapter = wateringDaysAdapter
        }
    }
    

    
    private fun fetchWeatherData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val service = retrofit.create(WeatherService::class.java)
        val call = service.getForecast(
            location = selectedLocation,
            apiKey = openWeatherApiKey,
            units = "metric",
            count = 56 // Enough data for 7 days
        )
        
        call.enqueue(object : Callback<OpenWeatherResponse> {
            override fun onResponse(
                call: Call<OpenWeatherResponse>,
                response: Response<OpenWeatherResponse>
            ) {
                if (response.isSuccessful) {
                    val weatherData = response.body()
                    weatherData?.let {
                        processWeatherData(it)
                    }
                } else {
                    showError("Error fetching weather data: ${response.message()}")
                }
            }
            
            override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {
                showError("Failed to connect to weather service: ${t.message}")
            }
        })
    }
    
    private fun processWeatherData(weather: OpenWeatherResponse) {
        // Group forecasts by day to get one entry per day
        val dailyForecasts = weather.list
            .groupBy {
                val date = Date(it.dt * 1000)
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatter.format(date)
            }
            .values
            .map { it.maxBy { forecast -> forecast.main.temp_max } } // Take forecast with highest temp for each day
            .take(7) // Take exactly 7 days

        // Determine if it's raining today
        val todayForecast = dailyForecasts.firstOrNull()
        val isRainingToday = todayForecast?.weather?.firstOrNull()?.let { 
            it.main.equals("Rain", ignoreCase = true) || 
            it.description.contains("rain", ignoreCase = true)
        } ?: false
        
        this.isRainingToday = isRainingToday
        
        // Update UI with rain notification
        updateRainNotification(isRainingToday)
        
        // Create data for adapter with weather and rainy day info
        val forecastItems = dailyForecasts.mapIndexed { index, forecast ->
            val date = Date(forecast.dt * 1000)
            val isRainyDay = forecast.weather.firstOrNull()?.let { 
                it.main.equals("Rain", ignoreCase = true) || 
                it.description.contains("rain", ignoreCase = true)
            } ?: false
            
            WateringDayItem(
                date = date,
                temperature = forecast.main.temp,
                weatherIcon = forecast.weather.firstOrNull()?.icon ?: "01d",
                isRainyDay = isRainyDay
            )
        }
        
        // Update the adapter with new data
        wateringDaysAdapter.updateForecasts(forecastItems)
    }
    
    private fun updateRainNotification(isRaining: Boolean) {
        if (isRaining) {
            rainNotificationText.text = "Today is raining, so no need to water the plants."
        } else {
            rainNotificationText.text = "No rain today. Your plants need watering!"
        }
        
        // Hide loading indicator after weather data is processed
        loadingIndicator.visibility = View.GONE
    }
    
    private fun onDaySelected(dayIndex: Int, isRainyDay: Boolean) {
        if (dayIndex == 0) {
            // User clicked on today
            if (isRainyDay) {
                // It's raining today
                Toast.makeText(
                    this,
                    "Today is raining, so no need to water the plants.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // It's not raining today
                Toast.makeText(
                    this,
                    "No rain today. Remember to water your plants!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // User clicked on a future day
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, dayIndex)
            val dayName = dayFormat.format(calendar.time)
            
            if (isRainyDay) {
                Toast.makeText(
                    this,
                    "Rain is expected on $dayName. You might not need to water your plants.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "No rain expected on $dayName. Plan to water your plants.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

// Adapter for the 7-day watering forecast
class WateringDaysAdapter(
    private var forecasts: List<WateringDayItem>,
    private val onDayClicked: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<WateringDaysAdapter.ViewHolder>() {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayNameText: TextView = view.findViewById(R.id.dayNameText)
        val temperatureText: TextView = view.findViewById(R.id.temperatureText)
        val weatherIcon: android.widget.ImageView = view.findViewById(R.id.weatherIcon)
        val dayContainer: LinearLayout = view.findViewById(R.id.dayContainer)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_watering_day, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val forecast = forecasts[position]
        
        // Format day name (Today, Tomorrow, or day name)
        val dayText = when (position) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                dayFormat.format(forecast.date)
            }
        }
        
        holder.dayNameText.text = dayText
        holder.temperatureText.text = "${forecast.temperature.toInt()}°"
        
        // Set the weather icon
        val iconResource = WeatherIcons.getIconResource(forecast.weatherIcon)
        holder.weatherIcon.setImageResource(iconResource)
        
        // Set background color based on rainy day or not
        if (forecast.isRainyDay) {
            holder.dayContainer.setBackgroundColor(android.graphics.Color.parseColor("#B8E0B9")) // Green for rainy days
        } else {
            holder.dayContainer.setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD")) // Gray for non-rainy days
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onDayClicked(position, forecast.isRainyDay)
        }
    }
    
    override fun getItemCount() = forecasts.size
    
    fun updateForecasts(newForecasts: List<WateringDayItem>) {
        forecasts = newForecasts
        notifyDataSetChanged()
    }
}

// Data class for watering day items
data class WateringDayItem(
    val date: Date,
    val temperature: Float,
    val weatherIcon: String,
    val isRainyDay: Boolean
)
