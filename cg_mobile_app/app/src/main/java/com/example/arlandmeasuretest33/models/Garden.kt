package com.example.arlandmeasuretest33.models

import java.util.Date

data class Garden(
    val id: String = "",  // Firebase document ID
    val name: String = "",
    val createdDate: Date = Date(),
    val areaSize: Double = 0.0,
    val plants: MutableList<Plant> = mutableListOf()
)

data class Plant(
    val id: String = "",  // Firebase document ID
    val name: String = "",
    val plantedDate: Date? = null,
    val harvestDate: Date? = null,
    val growthPeriodDays: Int = 0,
    val imageRef: String = "",  // Reference to the plant image
    val description: String = "",
    val expectedYieldPerPlant: Int = 0,
    val fertilizer: Int = 0,
    val costPerUnit: Int = 0
)

