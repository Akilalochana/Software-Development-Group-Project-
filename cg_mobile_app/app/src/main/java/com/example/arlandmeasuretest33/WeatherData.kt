package com.example.arlandmeasuretest33

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("forecast")
    fun getForecast(
        @Query("q") location: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
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