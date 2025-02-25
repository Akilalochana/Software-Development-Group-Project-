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
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {
    private lateinit var forecastAdapter: ForecastAdapter
    private val openWeatherApiKey = "7545a2bc25e9b63cf4b8a41f7c22d03a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            println("WeatherActivity: onCreate START")
            setContentView(R.layout.activity_weather)

            // Initialize views and setup
            setupRecyclerView()
            setupBackButton()

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
            println("WeatherActivity: fetchWeatherData START")
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(WeatherService::class.java)
            val call = service.getForecast(
                location = "Colombo",
                apiKey = openWeatherApiKey,
                units = "metric"
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

        // Load current weather icon
        Glide.with(this)
            .load("https://openweathermap.org/img/w/${currentWeather.weather[0].icon}.png")
            .into(findViewById(R.id.weatherIcon))

        // Group forecasts by day
        val dailyForecasts = weather.list.groupBy {
            val date = Date(it.dt * 1000)
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            formatter.format(date)
        }.map { it.value.first() }  // Take first forecast of each day

        // Update forecast list
        println("WeatherActivity: Updating forecast adapter with ${dailyForecasts.size} items")
        forecastAdapter.updateForecasts(dailyForecasts)
    }
}