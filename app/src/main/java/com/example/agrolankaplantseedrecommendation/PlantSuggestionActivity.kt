package com.example.agrolankaplantseedrecommendation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlantSuggestionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_suggestion)

        recyclerView = findViewById(R.id.plant_recycler_view)
        setupPlantList()
    }

    private fun setupPlantList() {
        val plants = listOf(
            Plant("Tomato", "Description about tomatoes", R.drawable.tomato),
            Plant("Chilis", "Description about chilis", R.drawable.chilis),
            Plant("Carrot", "Description about carrots", R.drawable.carrot),
            Plant("Potato", "Description about potatoes", R.drawable.potato)
        )

        recyclerView.adapter = PlantAdapter(plants)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
}

data class Plant(
    val name: String,
    val description: String,
    val imageResource: Int
)