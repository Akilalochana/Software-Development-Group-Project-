package com.example.arlandmeasuretest33.models

data class PlantCategory(
    val name: String,
    val category: String,
    val imageResource: Int,
    val district: String = "Ampara" // Default to Ampara district if none specified
) 