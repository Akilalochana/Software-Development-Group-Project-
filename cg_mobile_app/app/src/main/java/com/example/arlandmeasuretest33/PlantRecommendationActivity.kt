package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class PlantRecommendationActivity : AppCompatActivity() {
    private lateinit var gardenNameText: TextView
    private lateinit var districtNameText: TextView
    private lateinit var noRecommendationsText: TextView
    private val plantCardIds = mutableMapOf<String, Int>()
    private lateinit var db: FirebaseFirestore
    private val TAG = "PlantDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_recommendation)

        try {
            // Initialize Firestore
            db = FirebaseFirestore.getInstance()

            // Get data from intent
            val selectedDistrict = intent.getStringExtra("SELECTED_DISTRICT") ?: ""
            val gardenName = intent.getStringExtra("GARDEN_NAME") ?: ""

            // Debug log
            Log.d(TAG, "Selected district: '$selectedDistrict', Garden name: '$gardenName'")
            Toast.makeText(this, "Selected district: $selectedDistrict", Toast.LENGTH_SHORT).show()

            // Initialize header text views if they exist in your layout
            try {
                gardenNameText = findViewById(R.id.gardenNameText)
                districtNameText = findViewById(R.id.districtNameText)
                gardenNameText.text = gardenName
                districtNameText.text = selectedDistrict
                Log.d(TAG, "Successfully set header text views")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting header text views: ${e.message}")
                // If these views don't exist, just continue
            }

            try {
                noRecommendationsText = findViewById(R.id.noRecommendationsText)
                noRecommendationsText.visibility = View.GONE
                Log.d(TAG, "noRecommendationsText found and hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error finding or hiding noRecommendationsText: ${e.message}")
                // If this view doesn't exist, just continue
            }

            // Map plant names to card IDs
            initializePlantCardMap()

            // Hide all plant cards initially
            hideAllPlantCards()

            // Get recommended plants from Firestore
            getRecommendedPlantsFromFirestore(selectedDistrict)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error initializing plant recommendations", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializePlantCardMap() {
        // Map plant names to their corresponding card IDs
        plantCardIds["cabbage"] = R.id.cabbageCard
        plantCardIds["beetroot"] = R.id.beetrootCard
        plantCardIds["carrot"] = R.id.carrotCard
        plantCardIds["bitter melon"] = R.id.bittermelonCard
        plantCardIds["winged bean"] = R.id.wingedbeanCard
        plantCardIds["brinjal"] = R.id.brinjalCard
        plantCardIds["red spinach"] = R.id.red_spinach
        plantCardIds["leeks"] = R.id.leeksCard
        plantCardIds["potato"] = R.id.potatoCard
        plantCardIds["onion"] = R.id.onionCard
        plantCardIds["manioc"] = R.id.maniocCard
        plantCardIds["taro"] = R.id.taroCard
        plantCardIds["eggplant"] = R.id.eggplantCard
        plantCardIds["pumpkin"] = R.id.pumpkinCard
        plantCardIds["knolkhol"] = R.id.knolkholCard
        plantCardIds["drumstick"] = R.id.drumstickCard

        // Log all mappings to check
        for ((plant, cardId) in plantCardIds) {
            Log.d(TAG, "Plant '$plant' mapped to card ID $cardId")
        }
    }

    private fun hideAllPlantCards() {
        // Log before hiding
        Log.d(TAG, "Hiding all plant cards")

        // Hide all plant cards initially
        for ((plant, cardId) in plantCardIds) {
            try {
                val card = findViewById<CardView>(cardId)
                val isNull = (card == null)
                Log.d(TAG, "Card for '$plant' (ID: $cardId) exists: ${!isNull}")
                if (!isNull) {
                    card.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding or hiding card for '$plant': ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun getRecommendedPlantsFromFirestore(district: String) {
        // Show loading indicator if you have one
        Log.d(TAG, "Getting recommended plants from Firestore for district: '$district'")

        // First approach: Try to get from districts/{district}/crops collection
        db.collection("districts").document(district).collection("crops")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Got crops from district-specific collection
                    val recommendedPlants = documents.map { it.id.lowercase() }
                    Log.d(TAG, "Firestore success: Found ${recommendedPlants.size} plants: ${recommendedPlants.joinToString()}")
                    showRecommendedPlants(recommendedPlants)
                } else {
                    // Try fallback approach with a "districts_plants" collection
                    Log.d(TAG, "No plants found in Firestore, trying fallback approach")
                    getRecommendedPlantsAlternative(district)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore error: ${e.message}")
                e.printStackTrace()
                // Try fallback approach
                getRecommendedPlantsAlternative(district)
            }
    }

    private fun getRecommendedPlantsAlternative(district: String) {
        // Log for debugging
        Log.d(TAG, "Using fallback approach for district: '$district'")

        // Fallback: Using a hardcoded map if Firestore fails or is empty
        val districtPlantMap = mapOf(
            "Ampara" to listOf("carrot", "brinjal", "onion", "potato"),
            "Anuradhapura" to listOf("onion", "brinjal", "manioc", "drumstick"),
            "Badulla" to listOf("cabbage", "carrot", "leeks", "beetroot"),
            "Batticaloa" to listOf("brinjal", "bitter melon", "pumpkin"),
            "Colombo" to listOf("cabbage", "carrot", "beetroot", "red spinach"),
            "Galle" to listOf("red spinach", "winged bean", "brinjal"),
            "Gampaha" to listOf("cabbage", "winged bean", "beetroot"),
            "Hambantota" to listOf("onion", "brinjal", "pumpkin"),
            "Jaffna" to listOf("onion", "drumstick", "brinjal"),
            "Kalutara" to listOf("red spinach", "winged bean", "eggplant"),
            "Kandy" to listOf("cabbage", "carrot", "leeks", "potato"),
            "Kegalle" to listOf("red spinach", "winged bean", "taro"),
            "Kilinochchi" to listOf("onion", "brinjal", "pumpkin"),
            "Kurunegala" to listOf("manioc", "onion", "brinjal"),
            "Mannar" to listOf("onion", "brinjal", "drumstick"),
            "Matale" to listOf("carrot", "cabbage", "leeks"),
            "Matara" to listOf("red spinach", "winged bean", "brinjal"),
            "Monaragala" to listOf("brinjal", "pumpkin", "manioc"),
            "Mullaitivu" to listOf("onion", "brinjal", "pumpkin"),
            "Nuwara Eliya" to listOf("cabbage", "carrot", "leeks", "potato", "beetroot"),
            "Polonnaruwa" to listOf("brinjal", "onion", "manioc"),
            "Puttalam" to listOf("onion", "brinjal", "pumpkin"),
            "Ratnapura" to listOf("red spinach", "winged bean", "taro"),
            "Trincomalee" to listOf("brinjal", "bitter melon", "pumpkin"),
            "Vavuniya" to listOf("onion", "brinjal", "pumpkin")
        )

        // Log all available districts for comparison
        val availableDistricts = districtPlantMap.keys.joinToString(", ")
        Log.d(TAG, "Available districts: $availableDistricts")

        // Check if this district exists exactly in the map
        val exactMatch = districtPlantMap.containsKey(district)
        Log.d(TAG, "Exact district match found: $exactMatch")

        // Get plants for the district (lowercase everything for consistency)
        val recommendedPlants = districtPlantMap[district]?.map { it.lowercase() } ?: listOf()
        Log.d(TAG, "Plants found for '$district': ${recommendedPlants.joinToString(", ")}")

        // Show the plants
        showRecommendedPlants(recommendedPlants)
    }

    private fun showRecommendedPlants(plants: List<String>) {
        // Debug log
        Log.d(TAG, "Showing ${plants.size} plants: ${plants.joinToString(", ")}")

        // Hide loading indicator if you have one
        if (plants.isEmpty()) {
            try {
                noRecommendationsText.visibility = View.VISIBLE
                Log.d(TAG, "No plants to show, showing noRecommendationsText")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing noRecommendationsText: ${e.message}")
                Toast.makeText(this, "No recommended plants for this district", Toast.LENGTH_SHORT).show()
            }
            return
        }

        var visibleCardCount = 0

        // Show cards for recommended plants and set up click listeners
        for (plant in plants) {
            Log.d(TAG, "Looking for card for plant: '$plant'")
            val cardId = plantCardIds[plant]
            if (cardId != null) {
                try {
                    val card = findViewById<CardView>(cardId)
                    if (card != null) {
                        card.visibility = View.VISIBLE
                        card.setOnClickListener { startPricePrediction(plant) }
                        visibleCardCount++
                        Log.d(TAG, "Made visible: '$plant' with ID: $cardId")
                    } else {
                        Log.e(TAG, "Card not found for: '$plant' with ID: $cardId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error with card for '$plant': ${e.message}")
                    e.printStackTrace()
                }
            } else {
                Log.e(TAG, "No card ID mapping found for plant: '$plant'")
            }
        }

        Log.d(TAG, "Total visible cards: $visibleCardCount")

        if (visibleCardCount == 0) {
            try {
                noRecommendationsText.visibility = View.VISIBLE
                Log.d(TAG, "No cards visible, showing noRecommendationsText")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing noRecommendationsText: ${e.message}")
                Toast.makeText(this, "No recommended plants for this district", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                noRecommendationsText.visibility = View.GONE
                Log.d(TAG, "Cards visible, hiding noRecommendationsText")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding noRecommendationsText: ${e.message}")
            }
        }
    }

    private fun startPricePrediction(plantType: String) {
        try {
            Log.d(TAG, "Starting price prediction for: '$plantType'")
            val intent = Intent(this, PricePredictionActivity::class.java)
            intent.putExtra("PLANT_TYPE", plantType)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to price prediction: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error navigating to price prediction", Toast.LENGTH_SHORT).show()
        }
    }
}