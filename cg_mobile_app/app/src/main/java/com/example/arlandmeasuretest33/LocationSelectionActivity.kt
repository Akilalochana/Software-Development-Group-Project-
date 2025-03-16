package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LocationSelectionActivity : AppCompatActivity() {
    private lateinit var gardenNameInput: EditText
    private lateinit var locationSpinner: Spinner
    private lateinit var createButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d("LocationActivity", "Setting content view")
            setContentView(R.layout.activity_location_selection)

            Log.d("LocationActivity", "Initializing views")
            initializeViews()

            Log.d("LocationActivity", "Setting up spinner")
            setupLocationSpinner()

            Log.d("LocationActivity", "Setting up click listeners")
            setupClickListeners()
        } catch (e: Exception) {
            Log.e("LocationActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        gardenNameInput = findViewById(R.id.gardenNameInput)
        locationSpinner = findViewById(R.id.locationSpinner)
        createButton = findViewById(R.id.createButton)
    }

    private fun setupLocationSpinner() {
        val districts = arrayOf(
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo",
            "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara",
            "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar",
            "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya",
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,  // Changed from simple_spinner_dropdown_item
            districts
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = adapter

        // Add listener to update location features when an item is selected
        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDistrict = parent.getItemAtPosition(position).toString()
                updateLocationFeatures(selectedDistrict)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    // Add this new method to update the UI with location features
    private fun updateLocationFeatures(district: String) {
        val locationFeatures = getLocationFeatures(district)

        // Find the TextViews
        val climateValueTextView = findViewById<TextView>(R.id.climateValueTextView)
        val rainfallValueTextView = findViewById<TextView>(R.id.rainfallValueTextView)
        val soilTypeValueTextView = findViewById<TextView>(R.id.soilTypeValueTextView)
        val elevationValueTextView = findViewById<TextView>(R.id.elevationValueTextView)

        // Update the TextViews with the location feature data
        climateValueTextView.text = locationFeatures.climate
        rainfallValueTextView.text = locationFeatures.rainfall
        soilTypeValueTextView.text = locationFeatures.soilType
        elevationValueTextView.text = locationFeatures.elevation
    }

    private fun setupClickListeners() {
        createButton.setOnClickListener {
            val gardenName = gardenNameInput.text.toString()
            val selectedDistrict = locationSpinner.selectedItem?.toString() ?: ""

            if (gardenName.isBlank()) {
                Toast.makeText(this, "Please enter a garden name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDistrict.isBlank()) {
                Toast.makeText(this, "Please select a district", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Log the selected district for debugging
                Log.d("LocationActivity", "Selected district: $selectedDistrict")

                // Store the selected location and garden name
                val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putString("SELECTED_LOCATION", selectedDistrict)
                    putString("GARDEN_NAME", gardenName)
                    apply()
                }

                // Get location features based on selected district
                val locationFeatures = getLocationFeatures(selectedDistrict)

                // Log the location features for debugging
                Log.d("LocationActivity", "Climate: ${locationFeatures.climate}, Rainfall: ${locationFeatures.rainfall}")
                Log.d("LocationActivity", "Soil: ${locationFeatures.soilType}, Elevation: ${locationFeatures.elevation}")

                // Continue to plant recommendations
                val intent = Intent(this, PlantRecommendationActivity::class.java).apply {
                    putExtra("SELECTED_DISTRICT", selectedDistrict)
                    putExtra("GARDEN_NAME", gardenName)
                    putExtra("CLIMATE", locationFeatures.climate)
                    putExtra("RAINFALL", locationFeatures.rainfall)
                    putExtra("SOIL_TYPE", locationFeatures.soilType)
                    putExtra("ELEVATION", locationFeatures.elevation)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("LocationActivity", "Error navigating to recommendations", e)
                e.printStackTrace()
                Toast.makeText(this, "Error navigating to recommendations: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Class to hold location feature data
    data class LocationFeatures(
        val climate: String,
        val rainfall: String,
        val soilType: String,
        val elevation: String
    )

    // Get location features based on district
    private fun getLocationFeatures(district: String): LocationFeatures {
        return when (district) {
            "Colombo" -> LocationFeatures(
                "Tropical Monsoon",
                "1500-2500mm/year",
                "Red-Yellow Podzolic",
                "0-100m"
            )
            "Kandy" -> LocationFeatures(
                "Tropical Highlands",
                "1800-2500mm/year",
                "Red-Yellow Podzolic",
                "500-600m"
            )
            "Nuwara Eliya" -> LocationFeatures(
                "Cool Temperate",
                "2000-2500mm/year",
                "Mountain Regosols",
                "1800-2000m"
            )
            "Anuradhapura" -> LocationFeatures(
                "Tropical Dry",
                "1000-1500mm/year",
                "Reddish Brown Earth",
                "80-100m"
            )
            "Galle" -> LocationFeatures(
                "Tropical Wet",
                "2000-3000mm/year",
                "Red-Yellow Podzolic",
                "0-50m"
            )
            "Jaffna" -> LocationFeatures(
                "Arid",
                "800-1000mm/year",
                "Calcic Red-Yellow Latasols",
                "0-10m"
            )
            "Batticaloa" -> LocationFeatures(
                "Tropical Dry",
                "1500-2000mm/year",
                "Non-Calcic Brown",
                "0-30m"
            )
            "Trincomalee" -> LocationFeatures(
                "Tropical Dry",
                "1000-1700mm/year",
                "Red-Yellow Latasols",
                "0-50m"
            )
            "Matara" -> LocationFeatures(
                "Tropical Wet",
                "2000-2500mm/year",
                "Red-Yellow Podzolic",
                "0-100m"
            )
            "Badulla" -> LocationFeatures(
                "Tropical Highlands",
                "1700-2500mm/year",
                "Red-Yellow Podzolic",
                "600-900m"
            )
            // Default case - can be updated with more accurate data for other districts
            else -> LocationFeatures(
                "Tropical Monsoon",
                "1500-2000mm/year",
                "Red-Yellow Podzolic",
                "0-500m"
            )
        }
    }
}