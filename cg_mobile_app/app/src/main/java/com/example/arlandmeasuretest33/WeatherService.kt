package com.example.arlandmeasuretest33

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// WeatherService.kt
interface WeatherService {
    @GET("forecast.json")
    fun getCurrentWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("aqi") airQuality: String
    ): Call<WeatherResponse>
}