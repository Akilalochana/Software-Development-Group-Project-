// WeatherActivity.kt
package com.example.arlandmeasuretest33

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {
    private lateinit var cityNameText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var conditionText: TextView
    private lateinit var highLowText: TextView
    private lateinit var uvIndexText: TextView
    private lateinit var sunriseText: TextView
    private lateinit var windText: TextView
    private lateinit var rainfallText: TextView
    private lateinit var feelsLikeText: TextView
    private lateinit var humidityText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        initializeViews()
        fetchWeatherData()
    }

    private fun initializeViews() {
        cityNameText = findViewById(R.id.cityNameText)
        temperatureText = findViewById(R.id.temperatureText)
        conditionText = findViewById(R.id.conditionText)
        highLowText = findViewById(R.id.highLowText)
        uvIndexText = findViewById(R.id.uvIndexValue)
        sunriseText = findViewById(R.id.sunriseValue)
        windText = findViewById(R.id.windValue)
        rainfallText = findViewById(R.id.rainfallValue)
        feelsLikeText = findViewById(R.id.feelsLikeValue)
        humidityText = findViewById(R.id.humidityValue)
    }

    private fun fetchWeatherData() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.weatherapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)
        val call = service.getCurrentWeather(
            "YOUR_API_KEY",
            "Colombo",
            "no"
        )

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { updateUI(it) }
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                // Handle error
            }
        })
    }

    private fun updateUI(weather: WeatherResponse) {
        cityNameText.text = weather.location.name
        temperatureText.text = "${weather.current.tempC.toInt()}°"
        conditionText.text = weather.current.condition.text
        highLowText.text = "H:${weather.forecast.forecastday[0].day.maxtempC.toInt()}° L:${weather.forecast.forecastday[0].day.mintempC.toInt()}°"
        uvIndexText.text = weather.current.uv.toString()
        sunriseText.text = weather.forecast.forecastday[0].astro.sunrise
        windText.text = "${weather.current.windKph.toInt()} km/h"
        rainfallText.text = "${weather.current.precipMm} mm"
        feelsLikeText.text = "${weather.current.feelslikeC.toInt()}%"
        humidityText.text = "${weather.current.humidity}%"
    }
}