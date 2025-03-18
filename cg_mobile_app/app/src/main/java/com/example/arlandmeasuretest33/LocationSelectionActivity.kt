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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class LocationSelectionActivity : AppCompatActivity() {
    private lateinit var gardenNameInput: EditText
    private lateinit var locationSpinner: Spinner
    private lateinit var createButton: MaterialButton
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // TextViews for location features
    private lateinit var climateValueTextView: TextView
    private lateinit var rainfallValueTextView: TextView
    private lateinit var soilTypeValueTextView: TextView
    private lateinit var sunlightValueTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d("LocationActivity", "Setting content view")
            setContentView(R.layout.activity_location_selection)

            // Initialize Firebase Auth and Firestore
            auth = Firebase.auth
            db = FirebaseFirestore.getInstance()

            Log.d("LocationActivity", "Initializing views")
            initializeViews()

            // Authenticate first, then load data
            authenticateAndLoadData()

            Log.d("LocationActivity", "Setting up click listeners")
            setupClickListeners()
        } catch (e: Exception) {
            Log.e("LocationActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun authenticateAndLoadData() {
        // Only sign in if not already authenticated
        if (auth.currentUser == null) {
            // Show loading indicator
            Toast.makeText(this, "Connecting to database...", Toast.LENGTH_SHORT).show()

            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("LocationActivity", "Anonymous authentication successful")
                    // After authentication succeeds, load data
                    setupLocationSpinner()
                }
                .addOnFailureListener { e ->
                    Log.e("LocationActivity", "Authentication failed", e)
                    Toast.makeText(this, "Database authentication failed: ${e.message}. Please restart the app.", Toast.LENGTH_LONG).show()
                }
        } else {
            // Already authenticated, load data
            setupLocationSpinner()
        }
    }

    private fun initializeViews() {
        gardenNameInput = findViewById(R.id.gardenNameInput)
        locationSpinner = findViewById(R.id.locationSpinner)
        createButton = findViewById(R.id.createButton)

        // Initialize location feature TextViews
        climateValueTextView = findViewById(R.id.climateValueTextView)
        rainfallValueTextView = findViewById(R.id.rainfallValueTextView)
        soilTypeValueTextView = findViewById(R.id.soilTypeValueTextView)
        sunlightValueTextView = findViewById(R.id.sunlightValueTextView)
        // Set initial state
        setDataUnavailableState()
    }

    private fun setDataUnavailableState() {
        climateValueTextView.text = "Select a district"
        rainfallValueTextView.text = "Select a district"
        soilTypeValueTextView.text = "Select a district"
        sunlightValueTextView.text = "Select a district"
    }

    private fun setupLocationSpinner() {
        // Load districts from Firebase
        db.collection("districts").get()
            .addOnSuccessListener { documents ->
                val districts = documents.map { it.id }.sorted()

                if (districts.isEmpty()) {
                    Log.w("LocationActivity", "No districts found in Firebase")
                    Toast.makeText(this, "No districts found in database", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    districts
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                locationSpinner.adapter = adapter

                // Add listener to update location features when an item is selected
                locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedDistrict = parent.getItemAtPosition(position).toString()
                        fetchLocationFeaturesFromFirebase(selectedDistrict)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        // Do nothing
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationActivity", "Error loading districts from Firebase", e)
                Toast.makeText(this, "Error loading districts: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun fetchLocationFeaturesFromFirebase(district: String) {
        // Show loading state
        showLoadingState()

        // Try to get the LocationFeatures subcollection for the district
        db.collection("districts").document(district)
            .collection("LocationFeatures").limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Get first document from the LocationFeatures collection
                    val document = querySnapshot.documents[0]

                    // Get the fields
                    val climate = document.getString("climate") ?: "Not available"
                    val rainfall = document.getString("rainfall") ?: "Not available"
                    val soilType = document.getString("soiltype") ?: "Not available"
                    val sunlight = document.getString("sunlight") ?: "Not available"

                    // Update UI with retrieved data
                    updateLocationFeaturesUI(
                        LocationFeatures(climate, rainfall, soilType, sunlight)
                    )

                    Log.d("LocationActivity", "Loaded features for $district: $climate, $rainfall, $soilType, $sunlight")
                } else {
                    Log.d("LocationActivity", "No LocationFeatures found for district: $district")
                    // Try to fetch data directly from district document as a fallback
                    fetchFeaturesfromDistrictDocument(district)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationActivity", "Error fetching location data for $district", e)
                Toast.makeText(this, "Error loading location data: ${e.message}", Toast.LENGTH_SHORT).show()

                // Try alternative approach - data might be directly in district document
                fetchFeaturesfromDistrictDocument(district)
            }
    }

    private fun fetchFeaturesfromDistrictDocument(district: String) {
        db.collection("districts").document(district).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Check if fields are directly in the document
                    val climate = document.getString("climate")
                    val rainfall = document.getString("rainfall")
                    val soilType = document.getString("soiltype") ?: document.getString("soilType")
                    val sunlight = document.getString("sunlight") // Changed from elevation

                    if (climate != null || rainfall != null) {
                        // At least some data found directly in document
                        updateLocationFeaturesUI(
                            LocationFeatures(
                                climate ?: "Not available",
                                rainfall ?: "Not available",
                                soilType ?: "Not available",
                                sunlight ?: "Not available"
                            )
                        )
                    } else {
                        // No data found at all
                        setNoDataAvailableState(district)
                    }
                } else {
                    setNoDataAvailableState(district)
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationActivity", "Error fetching district document for $district", e)
                Toast.makeText(this, "Could not retrieve location data: ${e.message}", Toast.LENGTH_SHORT).show()
                setNoDataAvailableState(district)
            }
    }

    private fun setNoDataAvailableState(district: String) {
        updateLocationFeaturesUI(
            LocationFeatures(
                "No data available",
                "No data available",
                "No data available",
                "No data available"
            )
        )
        Toast.makeText(this, "No data available for $district in database", Toast.LENGTH_SHORT).show()
    }

    private fun showLoadingState() {
        climateValueTextView.text = "Loading..."
        rainfallValueTextView.text = "Loading..."
        soilTypeValueTextView.text = "Loading..."
        sunlightValueTextView.text = "Loading..." // Changed from elevationValueTextView
    }

    private fun updateLocationFeaturesUI(features: LocationFeatures) {
        // Update the TextViews with the location feature data
        climateValueTextView.text = features.climate
        rainfallValueTextView.text = features.rainfall
        soilTypeValueTextView.text = features.soilType
        sunlightValueTextView.text = features.sunlight // Changed from elevation
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

            // Check if we have actual data (not loading or error states)
            val climate = climateValueTextView.text.toString()
            if (climate == "Loading..." || climate == "No data available") {
                Toast.makeText(this, "Please wait for location data to load or select another district", Toast.LENGTH_SHORT).show()
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

                // Get current location features from TextViews
                val rainfall = rainfallValueTextView.text.toString()
                val soilType = soilTypeValueTextView.text.toString()
                val sunlight = sunlightValueTextView.text.toString() // Changed from elevation

                // Log the location features for debugging
                Log.d("LocationActivity", "Climate: $climate, Rainfall: $rainfall")
                Log.d("LocationActivity", "Soil: $soilType, Sunlight: $sunlight") // Changed from Elevation

                // Continue to plant recommendations
                val intent = Intent(this, PlantRecommendationActivity::class.java).apply {
                    putExtra("SELECTED_DISTRICT", selectedDistrict)
                    putExtra("GARDEN_NAME", gardenName)
                    putExtra("CLIMATE", climate)
                    putExtra("RAINFALL", rainfall)
                    putExtra("SOIL_TYPE", soilType)
                    putExtra("SUNLIGHT", sunlight) // Changed from ELEVATION
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
        val sunlight: String
    )
}