// WeatherActivity.kt
package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ImageView

class WeatherActivity : AppCompatActivity() {
    private lateinit var forecastAdapter: ForecastAdapter
    private val openWeatherApiKey = "7545a2bc25e9b63cf4b8a41f7c22d03a"
    private lateinit var selectedLocation: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            println("WeatherActivity: onCreate START")
            setContentView(R.layout.activity_weather)

            // Get the saved location
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            selectedLocation = sharedPreferences.getString("SELECTED_LOCATION", "Colombo") ?: "Colombo"
            println("WeatherActivity: Selected location - $selectedLocation")

            // Initialize views and setup
            setupRecyclerView()
            setupBackButton()
            setupWateringCycleButton()

            // Fetch weather data immediately
            fetchWeatherData()
            println("WeatherActivity: onCreate END")
        } catch (e: Exception) {
            println("WeatherActivity: onCreate ERROR - ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        println("WeatherActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        println("WeatherActivity: onResume")
        // Optionally refresh weather data when resuming
        fetchWeatherData()
    }

    override fun onPause() {
        super.onPause()
        println("WeatherActivity: onPause")
    }

    override fun onStop() {
        super.onStop()
        println("WeatherActivity: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("WeatherActivity: onDestroy")
    }

    private fun setupBackButton() {
        try {
            findViewById<ImageButton>(R.id.backButton).setOnClickListener {
                println("WeatherActivity: Back button clicked")
                super.onBackPressed()
            }
        } catch (e: Exception) {
            println("WeatherActivity: setupBackButton ERROR - ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun setupWateringCycleButton() {
        try {
            findViewById<androidx.cardview.widget.CardView>(R.id.wateringCycleButton).setOnClickListener {
                println("WeatherActivity: Watering Cycle button clicked")
                val intent = Intent(this, WateringCycleActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            println("WeatherActivity: setupWateringCycleButton ERROR - ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        println("WeatherActivity: Hardware back button pressed")
        super.onBackPressed()
    }

    private fun setupRecyclerView() {
        try {
            println("WeatherActivity: Setting up RecyclerView")
            forecastAdapter = ForecastAdapter()
            val recyclerView = findViewById<RecyclerView>(R.id.forecastRecyclerView)
            recyclerView.apply {
                adapter = forecastAdapter
                layoutManager = LinearLayoutManager(this@WeatherActivity)
                // Make sure RecyclerView is visible
                visibility = View.VISIBLE
            }
            println("WeatherActivity: RecyclerView setup complete")
        } catch (e: Exception) {
            println("WeatherActivity: setupRecyclerView ERROR - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun fetchWeatherData() {
        try {
            println("WeatherActivity: fetchWeatherData START for location: $selectedLocation")
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherService::class.java)
            val call = service.getForecast(
                location = selectedLocation,
                apiKey = openWeatherApiKey,
                units = "metric",
                count = 56
            )

            println("WeatherActivity: Making API call to: ${call.request().url}")

            call.enqueue(object : Callback<OpenWeatherResponse> {
                override fun onResponse(
                    call: Call<OpenWeatherResponse>,
                    response: Response<OpenWeatherResponse>
                ) {
                    println("WeatherActivity: API response received - isSuccessful: ${response.isSuccessful}")
                    try {
                        if (!isFinishing && response.isSuccessful) {
                            val body = response.body()
                            println("WeatherActivity: Response body received - forecast count: ${body?.list?.size}")
                            body?.let { updateUI(it) }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            println("WeatherActivity: API error - ${response.code()} - $errorBody")
                            showError("Error: ${response.code()} - ${response.message()}")
                        }
                    } catch (e: Exception) {
                        println("WeatherActivity: onResponse ERROR - ${e.message}")
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<OpenWeatherResponse>, t: Throwable) {
                    println("WeatherActivity: API call failed - ${t.message}")
                    println("WeatherActivity: Stack trace - ${t.stackTraceToString()}")
                    showError("Error fetching weather data: ${t.message}")
                }
            })
        } catch (e: Exception) {
            println("WeatherActivity: fetchWeatherData ERROR - ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this@WeatherActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun updateUI(weather: OpenWeatherResponse) {
        println("WeatherActivity: updateUI called with ${weather.list.size} forecasts")

        val currentWeather = weather.list.first()

        // Update current weather
        findViewById<TextView>(R.id.cityNameText).text = weather.city.name
        findViewById<TextView>(R.id.temperatureText).text =
            "${currentWeather.main.temp.toInt()}°"
        findViewById<TextView>(R.id.conditionText).text =
            currentWeather.weather.first().description
        findViewById<TextView>(R.id.highLowText).text =
            "H:${currentWeather.main.temp_max.toInt()}° L:${currentWeather.main.temp_min.toInt()}°"

        // Update to use custom icon
        val iconResource = WeatherIcons.getIconResource(currentWeather.weather[0].icon)
        findViewById<ImageView>(R.id.weatherIcon).setImageResource(iconResource)

        // Process forecasts to get exactly 6 days
        val dailyForecasts = weather.list
            .groupBy {
                val date = Date(it.dt * 1000)
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatter.format(date)
            }
            .values
            .map { it.maxBy { forecast -> forecast.main.temp_max } } // Take the forecast with highest temp for each day
            .take(6) // Ensure we take exactly 6 days

        println("WeatherActivity: Processed ${dailyForecasts.size} daily forecasts")
        forecastAdapter.updateForecasts(dailyForecasts)
    }

}