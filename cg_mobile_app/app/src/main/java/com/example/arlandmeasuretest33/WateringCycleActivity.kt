package com.example.arlandmeasuretest33

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class WateringCycleActivity : AppCompatActivity() {
    private val TAG = "WaterCycleDebug"
    private lateinit var db: FirebaseFirestore
    private lateinit var dayViews: Map<String, TextView>
    private val rainyDays = mutableSetOf<String>()
    private val wateringDays = mutableSetOf<String>()
    private val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val today = dateFormat.format(Date()).toUpperCase(Locale.ROOT).substring(0, 3)
    private val openWeatherApiKey = "7545a2bc25e9b63cf4b8d03a"  // Use your real API key
    private lateinit var selectedLocation: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watering_cycle)

        try {
            // Initialize Firestore
            db = FirebaseFirestore.getInstance()

            // Get the saved location
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            selectedLocation = sharedPreferences.getString("SELECTED_LOCATION", "Colombo") ?: "Colombo"

            // Initialize day views
            initDayViews()

            // Change title to "Non Watering Day" when it's a rainy day
            val tvWaterToday = findViewById<TextView>(R.id.tvWaterToday)

            // Setup back button
            findViewById<View>(R.id.backButton)?.setOnClickListener {
                finish()
            }

            // Get weather forecast to determine rainy days
            fetchWeatherData()

            // Load plants for this location
            loadPlantsForLocation(selectedLocation)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            Toast.makeText(this, "Error initializing watering cycle", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initDayViews() {
        // Map day abbreviations to TextView IDs
        dayViews = mapOf(
            "SUN" to findViewById(R.id.tvSun),
            "MON" to findViewById(R.id.tvMon),
            "TUE" to findViewById(R.id.tvTue),
            "WED" to findViewById(R.id.tvWed),
            "THU" to findViewById(R.id.tvThu),
            "FRI" to findViewById(R.id.tvFri),
            "SAT" to findViewById(R.id.tvSat)
        )

        // Set click listeners for all day views
        dayViews.forEach { (day, view) ->
            view.setOnClickListener {
                toggleWateringDay(day)
            }
        }
    }

    private fun toggleWateringDay(day: String) {
        if (rainyDays.contains(day)) {
            Toast.makeText(this, "Cannot water on rainy days", Toast.LENGTH_SHORT).show()
            return
        }

        if (wateringDays.contains(day)) {
            wateringDays.remove(day)
            dayViews[day]?.setBackgroundResource(R.drawable.day_inactive_background)
        } else {
            wateringDays.add(day)
            dayViews[day]?.setBackgroundResource(R.drawable.day_active_background)
        }

        // Save the updated watering days
        saveWateringDays()

        // Refresh plant watering status
        updatePlantsWateringStatus()
    }

    private fun saveWateringDays() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet("WATERING_DAYS", wateringDays)
        editor.apply()
    }

    private fun loadWateringDays() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        wateringDays.clear()
        wateringDays.addAll(sharedPreferences.getStringSet("WATERING_DAYS", setOf("SUN", "WED")) ?: setOf("SUN", "WED"))

        // Apply saved watering days to UI (considering rainy days)
        updateWateringDaysUI()
    }

    private fun updateWateringDaysUI() {
        // Reset all days to inactive
        dayViews.forEach { (_, view) ->
            view.setBackgroundResource(R.drawable.day_inactive_background)
        }

        // Set rainy days (should remain inactive but with a different look)
        rainyDays.forEach { day ->
            dayViews[day]?.let { view ->
                view.setBackgroundResource(R.drawable.day_inactive_background)
                // You could set a special rainy day background here if you have one
            }
        }

        // Set active watering days (excluding rainy days)
        wateringDays.forEach { day ->
            if (!rainyDays.contains(day)) {
                dayViews[day]?.setBackgroundResource(R.drawable.day_active_background)
            }
        }
    }

    private fun fetchWeatherData() {
        try {
            Log.d(TAG, "Fetching weather data for location: $selectedLocation")
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherService::class.java)
            val call = service.getForecast(
                location = selectedLocation,
                apiKey = openWeatherApiKey,
                units = "metric",
                count = 40  // 5 days of 3-hour forecasts
            )

            call.enqueue(object : Callback<OpenWeatherResponse> {
                override fun onResponse(
                    call: Call<OpenWeatherResponse>,
                    response: Response<OpenWeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val weatherResponse = response.body()
                        weatherResponse?.let { processWeatherData(it) }
                    } else {
                        Log.e(TAG, "Error fetching weather: ${response.code()}")
                        Toast.makeText(applicationContext, "Failed to get weather data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {
                    Log.e(TAG, "Weather API call failed: ${t.message}")
                    Toast.makeText(applicationContext, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchWeatherData: ${e.message}")
        }
    }

    private fun processWeatherData(weather: OpenWeatherResponse) {
        rainyDays.clear()

        // Group forecasts by day
        val dailyForecasts = weather.list
            .groupBy {
                val date = Date(it.dt * 1000)
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatter.format(date)
            }

        // Determine rainy days
        dailyForecasts.forEach { (dateStr, forecasts) ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(date).toUpperCase(Locale.ROOT).substring(0, 3)

            // Check if any forecast for the day has rain
            val isRainyDay = forecasts.any { forecast ->
                forecast.weather.any { weather ->
                    weather.main.contains("Rain", ignoreCase = true) ||
                            weather.description.contains("rain", ignoreCase = true)
                }
            }

            if (isRainyDay) {
                rainyDays.add(dayOfWeek)
                Log.d(TAG, "Rainy day detected: $dayOfWeek")

                // If it's today and it's rainy, update the title
                if (dayOfWeek == today) {
                    findViewById<TextView>(R.id.tvWaterToday).text = "Non Watering Day"
                }
            }
        }

        // If today isn't rainy, make sure title is correct
        if (!rainyDays.contains(today)) {
            findViewById<TextView>(R.id.tvWaterToday).text = "Water Today"
        }

        // Load saved watering days after determining rainy days
        loadWateringDays()

        // Update plants watering status based on weather
        updatePlantsWateringStatus()
    }

    private fun loadPlantsForLocation(location: String) {
        // Simulate location to district mapping (in a real app, you'd have a proper mapping)
        val district = when (location.toLowerCase(Locale.ROOT)) {
            "colombo" -> "Colombo"
            "kandy" -> "Kandy"
            "galle" -> "Galle"
            else -> "Colombo" // Default district
        }

        db.collection("districts").document(district).collection("crops")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val plantsList = mutableListOf<PlantInfo>()

                    for (document in documents) {
                        try {
                            val plantName = document.id

                            // Handle cost_per_unit field with proper type conversion
                            val costPerUnit = try {
                                when (val costValue = document.get("cost_per_unit")) {
                                    is Number -> costValue.toInt()
                                    is String -> costValue.toString().toIntOrNull() ?: 0
                                    else -> 0
                                }
                            } catch (e: Exception) {
                                0
                            }

                            val imageRef = document.getString("image") ?: ""

                            plantsList.add(PlantInfo(
                                name = plantName,
                                costPerUnit = costPerUnit,
                                imageRef = imageRef
                            ))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing plant document: ${e.message}")
                        }
                    }

                    createPlantCards(plantsList)
                } else {
                    Log.d(TAG, "No plants found for district: $district")
                    Toast.makeText(this, "No plants found for your location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting plants: ${e.message}")
                Toast.makeText(this, "Failed to load plants", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createPlantCards(plants: List<PlantInfo>) {
        // Get the container layout where we'll add plant cards
        val container = findViewById<LinearLayout>(R.id.plantsContainer)
        container.removeAllViews()

        // Create a card for each plant
        plants.take(4).forEach { plant ->
            val cardView = layoutInflater.inflate(R.layout.plant_watering_card, container, false) as CardView

            // Set plant name
            cardView.findViewById<TextView>(R.id.tvPlantName).text = plant.name

            // Load plant image
            val imageView = cardView.findViewById<ImageView>(R.id.ivPlant)
            if (plant.imageRef.isNotEmpty()) {
                Glide.with(this)
                    .load(plant.imageRef)
                    .placeholder(R.drawable.img_carrot)
                    .error(R.drawable.img_carrot)
                    .into(imageView)
            } else {
                imageView.setImageResource(getPlantImageResource(plant.name))
            }

            // Set up water/check button
            val waterButton = cardView.findViewById<ImageView>(R.id.ivWaterButton)
            val isWateringDay = isWateringDay()

            if (isWateringDay) {
                // It's a watering day - show water drop
                waterButton.setImageResource(R.drawable.ic_water_drop)
                waterButton.tag = "water"

                // Set click listener to toggle between water and check
                waterButton.setOnClickListener {
                    if (waterButton.tag == "water") {
                        waterButton.setImageResource(R.drawable.ic_check)
                        waterButton.tag = "check"
                        Toast.makeText(this, "${plant.name} watered!", Toast.LENGTH_SHORT).show()
                    } else {
                        waterButton.setImageResource(R.drawable.ic_water_drop)
                        waterButton.tag = "water"
                    }
                }

                // Hide non-watering info
                cardView.findViewById<TextView>(R.id.tvNonWateringReason).visibility = View.GONE
            } else {
                // It's a non-watering day - explain why
                waterButton.visibility = View.GONE

                // Show non-watering reason
                val reasonText = cardView.findViewById<TextView>(R.id.tvNonWateringReason)
                reasonText.visibility = View.VISIBLE

                if (rainyDays.contains(today)) {
                    reasonText.text = "Rainy day - No watering needed"
                } else {
                    reasonText.text = "Not a scheduled watering day"
                }
            }

            // Add the card to the container
            container.addView(cardView)
        }
    }

    private fun getPlantImageResource(plantName: String): Int {
        return when (plantName.toLowerCase(Locale.ROOT)) {
            "tomatoes", "tomato" -> R.drawable.img_tomato
            "beetroot" -> R.drawable.img_beetroot
            "potatoes", "potato" -> R.drawable.img_potato
            "pumpkin" -> R.drawable.img_pumpkin
            "carrot" -> R.drawable.img_carrot
            else -> R.drawable.img_carrot // Default image
        }
    }

    private fun isWateringDay(): Boolean {
        return wateringDays.contains(today) && !rainyDays.contains(today)
    }

    private fun updatePlantsWateringStatus() {
        // This will update the plant cards based on whether today is a watering day
        // Refresh the plants container
        val container = findViewById<LinearLayout>(R.id.plantsContainer)
        if (container.childCount > 0) {
            val isWateringDay = isWateringDay()

            for (i in 0 until container.childCount) {
                val cardView = container.getChildAt(i) as? CardView ?: continue
                val waterButton = cardView.findViewById<ImageView>(R.id.ivWaterButton)
                val reasonText = cardView.findViewById<TextView>(R.id.tvNonWateringReason)

                if (isWateringDay) {
                    // It's a watering day
                    waterButton.visibility = View.VISIBLE
                    waterButton.setImageResource(R.drawable.ic_water_drop)
                    waterButton.tag = "water"
                    reasonText.visibility = View.GONE
                } else {
                    // It's a non-watering day
                    waterButton.visibility = View.GONE
                    reasonText.visibility = View.VISIBLE

                    if (rainyDays.contains(today)) {
                        reasonText.text = "Rainy day - No watering needed"
                    } else {
                        reasonText.text = "Not a scheduled watering day"
                    }
                }
            }
        }
    }

    // Data class to hold plant information
    data class PlantInfo(
        val name: String,
        val costPerUnit: Int,
        val imageRef: String
    )
}