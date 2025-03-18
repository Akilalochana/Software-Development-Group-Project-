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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

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

        if (district.isEmpty()) {
            Log.e(TAG, "District name is empty")
            showNoRecommendationsMessage()
            return
        }

        // Check if document exists first
        db.collection("districts").document(district)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (!documentSnapshot.exists()) {
                    Log.e(TAG, "District document '$district' doesn't exist in Firestore")
                    showNoRecommendationsMessage()
                    return@addOnSuccessListener
                }

                // Now query the crops collection
                db.collection("districts").document(district).collection("crops")
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            // Process plants from Firestore
                            val plantsList = mutableListOf<PlantInfo>()

                            for (document in documents) {
                                try {
                                    // Log the raw document data for debugging
                                    Log.d(TAG, "Document ${document.id} data: ${document.data}")

                                    val plantName = document.id  // The document ID is the plant name

                                    // Handle cost_per_unit field with proper type conversion
                                    val costPerUnit = try {
                                        when (val costValue = document.get("cost_per_unit")) {
                                            is Number -> costValue.toInt()
                                            is String -> costValue.toString().toIntOrNull() ?: 0
                                            else -> 0
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing cost for ${document.id}: ${e.message}")
                                        0
                                    }

                                    // Handle growth_cycle_duration field with proper type conversion
                                    val growthPeriod = try {
                                        when (val periodValue = document.get("growth_cycle_duration")) {
                                            is Number -> periodValue.toInt()
                                            is String -> periodValue.toString().toIntOrNull() ?: 0
                                            else -> 0
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error parsing growth period for ${document.id}: ${e.message}")
                                        0
                                    }

                                    val imageRef = document.getString("image") ?: ""
                                    Log.d(TAG, "Image URL for $plantName: $imageRef")

                                    plantsList.add(PlantInfo(
                                        name = plantName,
                                        costPerUnit = costPerUnit,
                                        growthPeriod = growthPeriod,
                                        imageRef = imageRef
                                    ))
                                    Log.d(TAG, "Added plant: $plantName with cost: $costPerUnit and growth period: $growthPeriod")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing document ${document.id}: ${e.message}")
                                    e.printStackTrace()
                                }
                            }

                            Log.d(TAG, "Firestore success: Found ${plantsList.size} plants")
                            if (plantsList.isNotEmpty()) {
                                createPlantCards(plantsList)
                            } else {
                                showNoRecommendationsMessage()
                            }
                        } else {
                            Log.d(TAG, "No plants found in Firestore for district: $district")
                            showNoRecommendationsMessage()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firestore error querying crops: ${e.message}")
                        e.printStackTrace()
                        showNoRecommendationsMessage()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Firestore error checking district: ${e.message}")
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
        val noImageText = cardView.findViewById<TextView>(R.id.noImageText)

        // Set text values
        costText.text = "Cost per unit : Rs.${plant.costPerUnit}"
        nameText.text = plant.name
        growthPeriodText.text = "Growth Period : ${plant.growthPeriod} days"

        // Load image from Firebase URL
        if (plant.imageRef.isNotEmpty()) {
            noImageText.visibility = View.GONE  // Hide "No image available" text
            plantImage.visibility = View.VISIBLE  // Show image view

            // Load image using Glide
            try {
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.img_carrot)  // Default placeholder while loading
                    .error(R.drawable.img_carrot)  // Error placeholder if loading fails

                Glide.with(this)
                    .load(plant.imageRef)
                    .apply(requestOptions)
                    .into(plantImage)

                Log.d(TAG, "Loading image from URL: ${plant.imageRef}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image for ${plant.name}: ${e.message}")
                plantImage.setImageResource(R.drawable.img_carrot)  // Fallback to default image
            }
        } else {
            // No image URL available, show the "No image available" text
            plantImage.visibility = View.GONE
            noImageText.visibility = View.VISIBLE
            Log.d(TAG, "No image URL available for ${plant.name}")
        }

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