package com.example.arlandmeasuretest33

// WeatherData.kt
data class WeatherResponse(
    val location: Location,
    val current: Current,
    val forecast: Forecast
)

data class Location(
    val name: String,
    val region: String,
    val country: String
)

data class Current(
    val tempC: Float,
    val condition: Condition,
    val windKph: Float,
    val precipMm: Float,
    val humidity: Int,
    val feelslikeC: Float,
    val uv: Int
)

data class Condition(
    val text: String,
    val icon: String
)

data class Forecast(
    val forecastday: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: Day,
    val astro: Astro
)

data class Day(
    val maxtempC: Float,
    val mintempC: Float
)

data class Astro(
    val sunrise: String,
    val sunset: String
)