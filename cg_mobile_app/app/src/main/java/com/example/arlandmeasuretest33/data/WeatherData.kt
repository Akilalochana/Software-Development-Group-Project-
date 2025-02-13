package com.example.arlandmeasuretest33.data


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class WeatherData

data class CurrentLocation(
    val date: String = getCurrentDate(),
    val location: String = "Choose your location",
    val latitude: Double? = null,
    val longitude: Double? = null
) : WeatherData()

private fun getCurrentDate(): String{
    val currentDate = Date()
    val formatter = SimpleDateFormat("d MMMM yyyy" , Locale.getDefault())
    return "Today, ${formatter.format(currentDate)}"
}