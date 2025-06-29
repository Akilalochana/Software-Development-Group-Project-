package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
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
        
        // Initialize back button
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed()
        }

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

    private fun saveGardenDataToFirestore(gardenName: String, selectedDistrict: String) {
        // Get current user ID
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("LocationActivity", "User not authenticated, attempting to re-authenticate")
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("LocationActivity", "Re-authentication successful")
                    // Try saving again after successful authentication
                    saveGardenDataToFirestore(gardenName, selectedDistrict)
                }
                .addOnFailureListener { e ->
                    Log.e("LocationActivity", "Authentication failed", e)
                    Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            return
        }        // Log authentication state
        Log.d("LocationActivity", "User authentication state: ${auth.currentUser?.uid ?: "Not authenticated"}")

        // Check if current user is null
        if (currentUser == null) {
            Log.e("LocationActivity", "User not authenticated")
            Toast.makeText(this, "Authentication failed: User is not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        // Get current timestamp
        val timestamp = System.currentTimeMillis()

        // Create garden data document
        val gardenData = hashMapOf(
            "gardenName" to gardenName,
            "district" to selectedDistrict,
            "createdAt" to timestamp,
            "climate" to climateValueTextView.text.toString(),
            "rainfall" to rainfallValueTextView.text.toString(),
            "soilType" to soilTypeValueTextView.text.toString(),
            "sunlight" to sunlightValueTextView.text.toString()
        )

        // Add to user_gardens subcollection under user document
        // Use gardenName as the document ID instead of auto-generated ID
        db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(gardenName) // Use gardenName as document ID
            .set(gardenData) // Use set() instead of add()
            .addOnSuccessListener {
                Log.d("LocationActivity", "Garden saved to Firestore with ID: $gardenName")
                Toast.makeText(this, "Garden saved successfully", Toast.LENGTH_SHORT).show()

                // Continue to plant recommendations
                val intent = Intent(this, PlantRecommendationActivity::class.java).apply {
                    putExtra("SELECTED_DISTRICT", selectedDistrict)
                    putExtra("GARDEN_NAME", gardenName)
                    putExtra("CLIMATE", climateValueTextView.text.toString())
                    putExtra("RAINFALL", rainfallValueTextView.text.toString())
                    putExtra("SOIL_TYPE", soilTypeValueTextView.text.toString())
                    putExtra("SUNLIGHT", sunlightValueTextView.text.toString())
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("LocationActivity", "Error saving garden data", e)
                Toast.makeText(this, "Error saving garden data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

                // Store the selected location and garden name in SharedPreferences
                val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putString("SELECTED_LOCATION", selectedDistrict)
                    putString("GARDEN_NAME", gardenName)
                    apply()
                }

                // Save garden data to Firestore
                saveGardenDataToFirestore(gardenName, selectedDistrict)

                // Navigation to PlantRecommendationActivity now happens in saveGardenDataToFirestore
                // on success to prevent navigation before data is saved

            } catch (e: Exception) {
                Log.e("LocationActivity", "Error processing garden data", e)
                e.printStackTrace()
                Toast.makeText(this, "Error processing garden data: ${e.message}", Toast.LENGTH_SHORT).show()
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