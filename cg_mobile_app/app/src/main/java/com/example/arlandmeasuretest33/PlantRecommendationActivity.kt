package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import androidx.cardview.widget.CardView

class PlantRecommendationActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private val TAG = "PlantDebug"
    private lateinit var gridLayout: GridLayout
    private lateinit var noRecommendationsText: TextView
    private lateinit var headerTitleText: TextView
    private lateinit var backButton: ImageButton

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

            // Initialize UI elements
            gridLayout = findViewById(R.id.gridLayout)
            headerTitleText = findViewById(R.id.headerTitleText)
            headerTitleText.text = "$gardenName - $selectedDistrict"
            backButton = findViewById(R.id.backButton)

            // Set back button click listener
            backButton.setOnClickListener {
                finish()
            }

            try {
                noRecommendationsText = findViewById(R.id.noRecommendationsText)
                noRecommendationsText.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "Error finding noRecommendationsText: ${e.message}")
            }

            // Get recommended plants from Firestore
            getRecommendedPlantsFromFirestore(selectedDistrict)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error initializing plant recommendations", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getRecommendedPlantsFromFirestore(district: String) {
        Log.d(TAG, "Getting recommended crops from Firestore for district: '$district'")

        // Corrected path: districts/{district}/crops instead of districts/{district}/plants
        db.collection("districts").document(district).collection("crops")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Process plants from Firestore
                    val plantsList = mutableListOf<PlantInfo>()

                    for (document in documents) {
                        val plantName = document.id  // The document ID is the plant name
                        val costPerUnit = document.getDouble("cost_per_unit")?.toInt() ?: 0
                        val growthPeriod = document.getLong("growth_cycle_duration")?.toInt() ?: 0
                        val imageRef = document.getString("image") ?: ""

                        plantsList.add(PlantInfo(
                            name = plantName,
                            costPerUnit = costPerUnit,
                            growthPeriod = growthPeriod,
                            imageRef = imageRef
                        ))
                    }

                    Log.d(TAG, "Firestore success: Found ${plantsList.size} plants")
                    createPlantCards(plantsList)
                } else {
                    Log.d(TAG, "No plants found in Firestore for district: $district")
                    showNoRecommendationsMessage()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore error: ${e.message}")
                e.printStackTrace()
                showNoRecommendationsMessage()
            }
    }
    private fun createPlantCards(plants: List<PlantInfo>) {
        if (plants.isEmpty()) {
            showNoRecommendationsMessage()
            return
        }

        // Clear existing views in the grid layout
        gridLayout.removeAllViews()

        // Create and add plant cards dynamically
        for (plant in plants) {
            val cardView = createPlantCard(plant)
            gridLayout.addView(cardView)
        }
    }

    private fun createPlantCard(plant: PlantInfo): CardView {
        val inflater = LayoutInflater.from(this)
        val cardView = inflater.inflate(R.layout.plant_card_item, gridLayout, false) as CardView

        // Set plant details
        val plantImage = cardView.findViewById<ImageView>(R.id.plantImage)
        val costText = cardView.findViewById<TextView>(R.id.costText)
        val nameText = cardView.findViewById<TextView>(R.id.nameText)
        val growthPeriodText = cardView.findViewById<TextView>(R.id.growthPeriodText)
        val selectButton = cardView.findViewById<Button>(R.id.selectButton)

        // Set image resource based on plant name or use a default image
        // In a real app, you'd load images from a URL using Glide or Picasso
        val imageResId = getImageResourceForPlant(plant.name)
        plantImage.setImageResource(imageResId)

        // Set text values
        costText.text = "Cost per unit : Rs.${plant.costPerUnit}"
        nameText.text = plant.name
        growthPeriodText.text = "Growth Period : ${plant.growthPeriod} days"

        // Set button click listener
        selectButton.setOnClickListener {
            startPricePrediction(plant.name)
        }

        // Set layout params for the card
        val layoutParams = GridLayout.LayoutParams()
        layoutParams.width = 0
        layoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        layoutParams.setMargins(8, 8, 8, 8)
        cardView.layoutParams = layoutParams

        return cardView
    }

    private fun getImageResourceForPlant(plantName: String): Int {
        // Map plant names to drawable resources
        return when (plantName.lowercase()) {
            "cabbage" -> R.drawable.img_cabbage
            "beetroot" -> R.drawable.img_beetroot
            "carrot" -> R.drawable.img_carrot
            "bitter melon" -> R.drawable.img_bitter_melon
            "winged bean" -> R.drawable.img_winged_bean
            "brinjal" -> R.drawable.img_brinjal
            "red spinach" -> R.drawable.img_red_spinach
            "leeks" -> R.drawable.img_leeks
            "potato" -> R.drawable.img_potato
            "onion" -> R.drawable.img_onion
            "manioc" -> R.drawable.img_manioc
            "taro" -> R.drawable.img_taro
//            "eggplant" -> R.drawable.img_eggplant
            "pumpkin" -> R.drawable.img_pumpkin
//            "knolkhol" -> R.drawable.img_knolkhol
//            "drumstick" -> R.drawable.img_drumstick
            else -> R.drawable.img_carrot // Default image
        }
    }

    private fun showNoRecommendationsMessage() {
        try {
            if (this::noRecommendationsText.isInitialized) {
                noRecommendationsText.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "No recommended plants for this district", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing no recommendations message: ${e.message}")
            Toast.makeText(this, "No recommended plants for this district", Toast.LENGTH_SHORT).show()
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

    // Data class to hold plant information
    data class PlantInfo(
        val name: String,
        val costPerUnit: Int,
        val growthPeriod: Int,
        val imageRef: String
    )
}