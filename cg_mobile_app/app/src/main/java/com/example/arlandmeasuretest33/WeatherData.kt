package com.example.arlandmeasuretest33

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface  WeatherService {
    @GET("forecast")
    fun getForecast(
        @Query("q") location: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 56
    ): Call<OpenWeatherResponse>
}

data class OpenWeatherResponse(
    val list: List<DayForecast>,
    val city: City
)

data class DayForecast(
    val dt: Long,
    val main: Temperature,
    val weather: List<Weather>
)

data class Temperature(
    val temp: Float,
    val temp_min: Float,
    val temp_max: Float
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

data class City(
    val name: String,
    val country: String
)

object WeatherIcons {
    fun getIconResource(iconCode: String): Int {
        return when (iconCode) {
            "01d" -> R.drawable.ic_weather_clear_day
            "01n" -> R.drawable.ic_weather_clear_night
            "02d" -> R.drawable.ic_weather_partly_cloudy_day
            "02n" -> R.drawable.ic_weather_partly_cloudy_night
            "03d", "03n" -> R.drawable.ic_weather_cloudy
            "04d", "04n" -> R.drawable.ic_weather_cloudy
            "09d", "09n" -> R.drawable.ic_weather_rain
            "10d", "10n" -> R.drawable.ic_weather_rain
            "11d", "11n" -> R.drawable.ic_weather_thunderstorm
            "13d", "13n" -> R.drawable.ic_weather_cloudy
            "50d", "50n" -> R.drawable.ic_weather_fog
            else -> R.drawable.ic_weather_clear_day
        }
    }
}