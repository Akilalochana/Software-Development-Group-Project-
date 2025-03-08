package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class PlantRecommendationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_recommendation)

        try {
            // Get data from intent if needed
            val selectedDistrict = intent.getStringExtra("SELECTED_DISTRICT") ?: ""
            val gardenName = intent.getStringExtra("GARDEN_NAME") ?: ""

            // Initialize all CardViews
            val cabbageCard = findViewById<CardView>(R.id.cabbageCard)
            val radishCard = findViewById<CardView>(R.id.beetrootCard)
            val carrotCard = findViewById<CardView>(R.id.carrotCard)
            val tomatoCard = findViewById<CardView>(R.id.bittermelonCard)
            val capsicumCard = findViewById<CardView>(R.id.wingedbeanCard)
            val brinjalCard = findViewById<CardView>(R.id.brinjalCard)
            val okraCard = findViewById<CardView>(R.id.red_spinach)
            // New plants
            val leeksCard = findViewById<CardView>(R.id.leeksCard)
            val potatoCard = findViewById<CardView>(R.id.potatoCard)
            val onionCard = findViewById<CardView>(R.id.onionCard)
            val maniocCard = findViewById<CardView>(R.id.maniocCard)
            val taroCard = findViewById<CardView>(R.id.taroCard)
            val eggplantCard = findViewById<CardView>(R.id.eggplantCard)
            val pumpkinCard = findViewById<CardView>(R.id.pumpkinCard)
            val knolkholCard = findViewById<CardView>(R.id.knolkholCard)
            val drumstick = findViewById<CardView>(R.id.drumstickCard)



            // Set click listeners for each plant card
            cabbageCard.setOnClickListener { startPricePrediction("cabbage") }
            radishCard.setOnClickListener { startPricePrediction("beetroot") }
            carrotCard.setOnClickListener { startPricePrediction("carrot") }
            tomatoCard.setOnClickListener { startPricePrediction("Bitter melon") }
            capsicumCard.setOnClickListener { startPricePrediction("Winged bean") }
            brinjalCard.setOnClickListener { startPricePrediction("brinjal") }
            okraCard.setOnClickListener { startPricePrediction("Red spinach") }
            // New plants click listeners
            leeksCard.setOnClickListener { startPricePrediction("leeks") }
            potatoCard.setOnClickListener { startPricePrediction("potato") }
            onionCard.setOnClickListener { startPricePrediction("onion") }
            taroCard.setOnClickListener { startPricePrediction("manioc") }
            maniocCard.setOnClickListener { startPricePrediction("taro") }
            eggplantCard.setOnClickListener { startPricePrediction("eggplant") }
            pumpkinCard.setOnClickListener { startPricePrediction("pumpkin") }
            knolkholCard.setOnClickListener { startPricePrediction("knolkhol") }
            drumstick.setOnClickListener { startPricePrediction("drumstick") }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//    private fun startARFeature(plantType: String) {
//        try {
//            val intent = Intent(this, MainActivity::class.java)  // Changed back to MainActivity
//            intent.putExtra("PLANT_TYPE", plantType)
//            intent.putExtra("START_AR", true)
//            startActivity(intent)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }

    private fun startPricePrediction(plantType: String) {
        try {
            val intent = Intent(this, PricePredictionActivity::class.java)
            intent.putExtra("PLANT_TYPE", plantType)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}