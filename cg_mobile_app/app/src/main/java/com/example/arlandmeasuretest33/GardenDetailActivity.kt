package com.example.arlandmeasuretest33

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.arlandmeasuretest33.adapters.ModernPlantDetailAdapter
import com.example.arlandmeasuretest33.models.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class GardenDetailActivity : AppCompatActivity() {

    private lateinit var gardenNameTextView: TextView
    private lateinit var gardenCreatedDateTextView: TextView
    private lateinit var gardenAreaTextView: TextView
    private lateinit var gardenLocationTextView: TextView
    private lateinit var soilTypeTextView: TextView
    private lateinit var climateTextView: TextView
    private lateinit var rainfallTextView: TextView
    private lateinit var sunlightTextView: TextView
    private lateinit var plantsRecyclerView: RecyclerView
    private lateinit var backButton: View
    private lateinit var progressBar: View

    private val TAG = "GardenDetailActivity"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_garden_detail_modern)
        
        // Initialize views
        gardenNameTextView = findViewById(R.id.gardenNameTextView)
        gardenCreatedDateTextView = findViewById(R.id.gardenCreatedDateTextView)
        gardenAreaTextView = findViewById(R.id.gardenAreaTextView)
        gardenLocationTextView = findViewById(R.id.gardenLocationTextView)
        soilTypeTextView = findViewById(R.id.soilTypeTextView)
        climateTextView = findViewById(R.id.climateTextView)
        rainfallTextView = findViewById(R.id.rainfallTextView)
        sunlightTextView = findViewById(R.id.sunlightTextView)
        plantsRecyclerView = findViewById(R.id.plantsRecyclerView)
        backButton = findViewById(R.id.backButton)
        progressBar = findViewById(R.id.progressBar)
        
        // Set up RecyclerView
        plantsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Set up back button
        backButton.setOnClickListener {
            finish()
        }
        
        // Check if user is authenticated - this could matter for permission issues
        if (auth.currentUser == null) {
            Log.w(TAG, "User is not authenticated, this may cause permission issues")
            // Show warning to user that they need to be authenticated
            Toast.makeText(this, "You are not logged in. Some features may be restricted.", Toast.LENGTH_SHORT).show()
        } else {
            val currentUserId = auth.currentUser?.uid ?: "unknown"
            Log.d(TAG, "User is authenticated as: $currentUserId")
        }
        
        // Get garden info from intent
        val gardenId = intent.getStringExtra("GARDEN_ID")
        var gardenName = intent.getStringExtra("GARDEN_NAME") // Get the name too
        val isLegacyPath = intent.getBooleanExtra("IS_LEGACY_PATH", false)
        
        // If garden name is not provided, use the ID as the name
        if (gardenName == null || gardenName.isEmpty()) {
            gardenName = gardenId
            Log.d(TAG, "Garden name not provided, using ID as name: $gardenName")
        }
        
        // Add detailed logging to help diagnose garden ID issues
        Log.d(TAG, "Garden info received - ID: '$gardenId', Name: '$gardenName', Legacy path: $isLegacyPath")
        Log.d(TAG, "All intent extras: ${intent.extras?.keySet()?.joinToString { "$it: ${intent.extras?.get(it)}" }}")
        
        // Log info about authenticated user
        Log.d(TAG, "Current auth user: ${auth.currentUser?.uid ?: "none"}")
        Log.d(TAG, "Current auth email: ${auth.currentUser?.email ?: "none"}")
        
        if (isLegacyPath) {
            Log.d(TAG, "This is a legacy path garden, may have permission issues: $gardenId")
        }
        
        if (gardenId != null) {
            // Always try the most robust approach
            Log.d(TAG, "Starting garden details loading with fallback strategies")
            
            // For all gardens, try using both ID and name to maximize chance of finding the garden
            if (gardenName != null && gardenName != gardenId) {
                Log.d(TAG, "Will try both ID and name to find garden")
                loadGardenDetailsWithFallback(gardenId, gardenName, isLegacyPath)
            } else {
                // For legacy gardens especially, we want to use all our robust fallback methods
                if (isLegacyPath) {
                    loadLegacyGardenDetails(gardenId)
                } else {
                    // For standard paths, try standard loading first
                    loadGardenDetails(gardenId, isLegacyPath)
                }
            }
        } else {
            // Silently handle missing garden ID
            Log.w(TAG, "Garden ID not found")
            finish()
        }
    }
    
    private fun loadGardenDetails(gardenId: String, isLegacyPath: Boolean) {
        showProgress(true)
        
        if (isLegacyPath) {
            loadLegacyGardenDetails(gardenId)
        } else {
            loadStandardGardenDetails(gardenId)
        }
    }
    
    private fun loadStandardGardenDetails(gardenId: String) {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val userId = currentUser.uid
        
        // Get garden details
        db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(gardenId)
            .get()
            .addOnSuccessListener { gardenDoc ->
                if (gardenDoc.exists()) {
                    // Set garden details
                    val gardenName = gardenDoc.id
                    val createdDate = gardenDoc.getTimestamp("createdDate")?.toDate() ?: Date()
                    val areaSize = gardenDoc.getDouble("areaSize") ?: gardenDoc.getDouble("area") ?: 0.0
                    val district = gardenDoc.getString("district") ?: "Not specified"
                    val soilType = gardenDoc.getString("soilType") ?: "Not specified"
                    val climate = gardenDoc.getString("climate") ?: "Not specified"
                    val rainfall = gardenDoc.getString("rainfall") ?: "Not specified"
                    val sunlight = gardenDoc.getString("sunlight") ?: "Not specified"
                      updateUI(gardenName, createdDate, areaSize, district, soilType, climate, rainfall, sunlight)
                    
                    // Get plants in this garden
                    loadPlantsForStandardGarden(userId, gardenId)
                } else {
                    // Silently handle missing garden details
                    Log.w(TAG, "Garden details not found")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading garden details", e)
                // Silently handle error loading garden details
                showProgress(false)
            }
    }
      private fun loadLegacyGardenDetails(gardenId: String) {
        Log.d(TAG, "Attempting to load legacy garden details for: $gardenId")

        // Show some basic information immediately so the screen isn't empty
        showBasicGardenInfo(gardenId)
        
        // First try direct access
        tryLoadLegacyGardenWithFallback(gardenId)
        
        // If direct access fails, fallback handlers in tryLoadLegacyGardenWithFallback will handle it
    }
    
    /**
     * Attempts to load legacy garden data with fallback options to handle permission issues
     */
    private fun tryLoadLegacyGardenWithFallback(gardenId: String) {
        Log.d(TAG, "Trying to load legacy garden: $gardenId - first attempt")
        
        // Debug: List available gardens to check what's there
        Log.d(TAG, "Listing all available gardens for debugging")
        db.collection("user_gardens")
            .limit(10) // Only retrieve a few for debugging
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "Found ${snapshot.documents.size} gardens in user_gardens collection")
                for (doc in snapshot.documents) {
                    Log.d(TAG, "Available garden: ${doc.id}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to list gardens: ${e.message}")
            }
        
        try {
            // Try to access as the authenticated user first
            db.collection("user_gardens")
                .document(gardenId)
                .get()
                .addOnSuccessListener { gardenDoc ->
                    showProgress(false)
                    if (gardenDoc.exists()) {
                        Log.d(TAG, "Legacy garden document exists, attempting to parse")
                        displayLegacyGardenData(gardenDoc, gardenId)                    } else {
                        Log.w(TAG, "Garden document doesn't exist for legacy path: user_gardens/$gardenId")
                        
                        // Try verifying where the garden might exist (for debugging)
                        verifyGardenExistence(gardenId)
                        
                        // Step 1: Try the extensive ID variations we defined (includes all the previous variations plus more)
                        Log.d(TAG, "Starting comprehensive garden search with ID variations")
                        findGardenWithIDVariations(gardenId)
                        
                        // Step 2: If that fails, the variation search will call findGardenByFieldValue
                        // which searches by name field value
                        
                        // Step 3: If all else fails, we'll fall back to the potential matches search
                        // This will happen in the completionHandler of findGardenWithIDVariations
                    }
                }                .addOnFailureListener { e ->
                    showProgress(false)
                    Log.e(TAG, "Error loading legacy garden details: ${e.message}")
                    
                    if (e.message?.contains("PERMISSION_DENIED") == true) {
                        Log.w(TAG, "Permission denied for garden: $gardenId - trying without auth")
                        
                        // Try a different approach - if user is not the owner of this garden
                        // Use the name as the garden ID and attempt to just display plants if we have permission
                        displayPublicLegacyGardenView(gardenId)
                    } else {
                        // For non-permission errors, show toast
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            showProgress(false)
            Log.e(TAG, "Exception when setting up Firestore access", e)
            // Already showing basic info
        }
    }
    
    /**
     * Displays a public view of a legacy garden with limited information
     */
    private fun displayPublicLegacyGardenView(gardenId: String) {
        // When we have permission issues, we can still try to load public plant data if available
        showProgress(false)
        
        // We'll just show the garden name, which we already have
        updateUI(
            gardenName = gardenId,
            createdDate = Date(), // Just use current date as we can't access the real creation date
            areaSize = 0.0, // Don't have access to the area
            district = "Access restricted", 
            soilType = "Access restricted",
            climate = "Access restricted", 
            rainfall = "Access restricted",
            sunlight = "Access restricted"
        )
        
        // Try to at least load the plant names from this garden if it has public access
        tryLoadPlantsWithPermissionHandling(gardenId)
    }
    
    /**
     * Displays legacy garden data from a Firestore document
     */
    private fun displayLegacyGardenData(gardenDoc: com.google.firebase.firestore.DocumentSnapshot, gardenId: String) {
        try {
            // Set garden details - with safe fallbacks for all fields
            val gardenName = gardenId // Use the ID as name in case field is missing
            val createdDate = gardenDoc.getTimestamp("createdAt")?.toDate() ?: Date()
            val areaSize = gardenDoc.getDouble("area") ?: gardenDoc.getDouble("areaSize") ?: 0.0
            val district = gardenDoc.getString("district") ?: "Not specified"
            val soilType = gardenDoc.getString("soilType") ?: "Not specified"
            val climate = gardenDoc.getString("climate") ?: "Not specified"
            val rainfall = gardenDoc.getString("rainfall") ?: "Not specified"
            val sunlight = gardenDoc.getString("sunlight") ?: "Not specified"

            updateUI(gardenName, createdDate, areaSize, district, soilType, climate, rainfall, sunlight)

            // Get plants in this garden - using a safer approach
            tryLoadPlantsWithPermissionHandling(gardenId)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing plant document: ${gardenDoc.id}", e)
            // We already displayed basic info, so just show a toast
            Toast.makeText(this, "Error parsing garden details", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Display basic garden info based on what we know without Firestore access
     */
    private fun showBasicGardenInfo(gardenId: String) {
        gardenNameTextView.text = gardenId
        gardenCreatedDateTextView.text = "Created: Not available"
        gardenAreaTextView.text = "Area: Not available"
        gardenLocationTextView.text = "Location: Not available"
        soilTypeTextView.text = "Soil: Not available"
        climateTextView.text = "Climate: Not available"
        rainfallTextView.text = "Rainfall: Not available"
        sunlightTextView.text = "Sunlight: Not available"

        // Setup empty plants list
        plantsRecyclerView.adapter = ModernPlantDetailAdapter(
            emptyList(),
            onDeletePlant = { plant -> 
                // No-op for empty list
            }
        )
    }    /**
     * Try to load plants with careful permission handling
     */
    private fun tryLoadPlantsWithPermissionHandling(gardenId: String) {
        Log.d(TAG, "Attempting to load plants for garden: $gardenId")

        // First let's try to use a query with a more permissive approach
        // This may work even if the user doesn't have access to the full garden
        db.collection("user_gardens")
            .document(gardenId)
            .collection("plants")
            .get()
            .addOnSuccessListener { plantsSnapshot ->
                val plantsList = mutableListOf<Plant>()

                if (plantsSnapshot.isEmpty) {
                    Log.d(TAG, "No plants found for garden: $gardenId")
                } else {
                    for (plantDoc in plantsSnapshot.documents) {
                        try {
                            // Log plant field values for debugging
                            logPlantFieldValues(plantDoc)
                            
                            // Extract plant details safely
                            val plant = Plant(
                                id = plantDoc.id,
                                name = plantDoc.getString("name") ?: plantDoc.id,
                                plantedDate = plantDoc.getTimestamp("dateAdded")?.toDate() ?: Date(),
                                harvestDate = null,
                                growthPeriodDays = plantDoc.getLong("growthPeriod")?.toInt()
                                    ?: plantDoc.getLong("growthPeriodDays")?.toInt() ?: 0,
                                imageRef = plantDoc.getString("imageRef") ?: "",
                                description = plantDoc.getString("description") ?: "No description available",
                                expectedYieldPerPlant = plantDoc.getLong("expectedYieldPerPlant")?.toInt() ?: 0,
                                fertilizer = plantDoc.getLong("fertilizer")?.toInt() ?: 0,
                                costPerUnit = plantDoc.getLong("costPerUnit")?.toInt() ?: 0
                            )
                            plantsList.add(plant)
                                                      // Log the image reference to help debugging
                            if (plant.imageRef.isNotEmpty()) {
                                Log.d(TAG, "Plant ${plant.name} has image: ${plant.imageRef}")
                                
                                // Validate the image URL format
                                if (!plant.imageRef.startsWith("http")) {
                                    Log.w(TAG, "Plant ${plant.name} image URL may not be valid: ${plant.imageRef}")
                                } else {
                                    Log.d(TAG, "Plant ${plant.name} has valid image URL format")
                                }
                            } else {
                                Log.w(TAG, "Plant ${plant.name} has no image reference")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing plant document: ${plantDoc.id}", e)
                        }
                    }

                    Log.d(TAG, "Loaded ${plantsList.size} plants for garden: $gardenId")
                }

                // Update UI even if plants list is empty
                val adapter = ModernPlantDetailAdapter(
                    plantsList,
                    onDeletePlant = { plant -> 
                        val gardenId = intent.getStringExtra("GARDEN_ID") ?: return@ModernPlantDetailAdapter
                        // Show confirmation dialog before deleting
                        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Delete ${plant.name}")
                            .setMessage("Are you sure you want to delete this plant?")
                            .setPositiveButton("Delete") { _, _ -> 
                                deletePlantFromFirebase(plant, gardenId)
                            }
                            .setNegativeButton("Cancel", null)
                            
                        // Show dialog and get its reference
                        val dialog = dialogBuilder.create()
                        dialog.show()
                        
                        // Change button colors after showing the dialog
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    }
                )
                runOnUiThread {
                    plantsRecyclerView.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading plants: ${e.message}")

                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    // When permission is denied, try a second approach
                    tryAlternatePublicPlantAccess(gardenId)
                } else {
                    Log.e(TAG, "Error loading plants: ${e.message}")
                }
            }
    }
    
    /**
     * Try to access plants in a more public way, useful when a garden's privacy is restricted
     * but individual plants may be public
     */
    private fun tryAlternatePublicPlantAccess(gardenId: String) {
        Log.d(TAG, "Trying alternate public plant access for garden: $gardenId")
        
        // For legacy paths, this might work with different security rules
        val currentUser = auth.currentUser
          // Show message to explain the situation
        runOnUiThread {
            Toast.makeText(this, 
                "This garden uses legacy data that requires special permissions. " +
                "Contact support if you need full access.", 
                Toast.LENGTH_LONG).show()
        }
        
        // Create a basic plant to show something
        val fallbackPlant = Plant(
            id = "sample",
            name = "Sample Plant",
            description = "Sorry, you don't have permission to view the plants in this garden. " +
                    "This might be because you're not the owner of this garden or the garden " +
                    "has restricted access. Try logging in with a different account or contact support.",
            growthPeriodDays = 0,
            imageRef = ""
        )
          // Show at least something in the UI
        val adapter = ModernPlantDetailAdapter(
            listOf(fallbackPlant),
            onDeletePlant = { plant -> 
                // Fallback plants shouldn't be deleted - this is just for display
                Toast.makeText(this, "Cannot delete demo plant", Toast.LENGTH_SHORT).show()
            }
        )
        runOnUiThread {
            plantsRecyclerView.adapter = adapter
        }
    }
      
    private fun loadPlantsForStandardGarden(userId: String, gardenId: String) {
        Log.d(TAG, "Loading plants for standard garden path: userId=$userId, gardenId=$gardenId")
        
        db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(gardenId)
            .collection("plants")
            .get()
            .addOnSuccessListener { plantsSnapshot ->
                val plantsList = mutableListOf<Plant>()
                
                if (plantsSnapshot.isEmpty) {
                    Log.d(TAG, "No plants found for standard garden: $gardenId")
                } else {
                    for (plantDoc in plantsSnapshot) {
                        try {
                            // Log plant field values for debugging
                            logPlantFieldValues(plantDoc)
                            
                            val plant = Plant(
                                id = plantDoc.id,
                                name = plantDoc.getString("name") ?: plantDoc.id,
                                plantedDate = plantDoc.getTimestamp("plantedDate")?.toDate(),
                                harvestDate = plantDoc.getTimestamp("harvestDate")?.toDate(),
                                growthPeriodDays = plantDoc.getLong("growthPeriodDays")?.toInt()
                                    ?: plantDoc.getLong("growthPeriod")?.toInt() ?: 0,
                                imageRef = plantDoc.getString("imageRef") ?: "",
                                description = plantDoc.getString("description") ?: "No description available",
                                expectedYieldPerPlant = plantDoc.getLong("expectedYieldPerPlant")?.toInt() ?: 0,
                                fertilizer = plantDoc.getLong("fertilizer")?.toInt() ?: 0,
                                costPerUnit = plantDoc.getLong("costPerUnit")?.toInt() ?: 0
                            )
                            plantsList.add(plant)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing plant document in standard path: ${plantDoc.id}", e)
                        }
                    }
                    
                    Log.d(TAG, "Loaded ${plantsList.size} plants for standard garden: $gardenId")
                }
                
                // Update UI even if plants list is empty
                val adapter = ModernPlantDetailAdapter(
                    plantsList,
                    onDeletePlant = { plant -> 
                        // Show confirmation dialog before deleting
                        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Delete ${plant.name}")
                            .setMessage("Are you sure you want to delete this plant?")
                            .setPositiveButton("Delete") { _, _ -> 
                                deletePlantFromFirebase(plant, gardenId)
                            }
                            .setNegativeButton("Cancel", null)
                            
                        // Show dialog and get its reference
                        val dialog = dialogBuilder.create()
                        dialog.show()
                        
                        // Change button colors after showing the dialog
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    }
                )
                runOnUiThread {
                    plantsRecyclerView.adapter = adapter
                }
                
                showProgress(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading plants for standard garden: ${e.message}")
                showProgress(false)
                Toast.makeText(this, "Error loading plants: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun updateUI(
        gardenName: String,
        createdDate: Date,
        areaSize: Double, 
        district: String,
        soilType: String,
        climate: String,
        rainfall: String,
        sunlight: String
    ) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Set garden name and add a description based on the garden type
        gardenNameTextView.text = gardenName
        
        // Add a description if there's a recognizable garden type in the name
        val gardenDescription = findViewById<TextView>(R.id.gardenDescriptionTextView)
        when {
            gardenName.contains("vegetable", ignoreCase = true) -> {
                gardenDescription.text = "Main production area"
            }
            gardenName.contains("herb", ignoreCase = true) -> {
                gardenDescription.text = "Herb cultivation area"
            }
            gardenName.contains("fruit", ignoreCase = true) -> {
                gardenDescription.text = "Fruit production area"
            }
            gardenName.contains("flower", ignoreCase = true) -> {
                gardenDescription.text = "Ornamental garden"
            }
            else -> {
                gardenDescription.text = "Cultivation area"
            }
        }
        
        // Format date and area as shown in the mockup
        gardenCreatedDateTextView.text = dateFormat.format(createdDate)
        gardenAreaTextView.text = "${String.format("%.3f", areaSize)} sq.m"
        
        // Format location as shown in the mockup
        gardenLocationTextView.text = district
        
        // Set condition values handling potentially long values
        // Truncate values if they're excessively long
        soilTypeTextView.text = if (soilType.length > 50) soilType.substring(0, 47) + "..." else soilType
        climateTextView.text = if (climate.length > 50) climate.substring(0, 47) + "..." else climate
        rainfallTextView.text = if (rainfall.length > 50) rainfall.substring(0, 47) + "..." else rainfall
        sunlightTextView.text = if (sunlight.length > 50) sunlight.substring(0, 47) + "..." else sunlight
    }
    
    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    /**
     * Try to verify where this garden exists in Firebase
     * This method checks both standard and legacy paths
     */
    private fun verifyGardenExistence(gardenId: String) {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Log.w(TAG, "Cannot verify garden existence - user not authenticated")
            return
        }
        
        val userId = currentUser.uid
          // Check in standard path
        db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(gardenId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d(TAG, "Garden exists in STANDARD path: user_data/$userId/user_gardens/$gardenId")
                } else {
                    Log.d(TAG, "Garden NOT found in standard path: user_data/$userId/user_gardens/$gardenId")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error checking standard path: ${exception.message}")
            }
              // Check in legacy path
        db.collection("user_gardens")
            .document(gardenId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d(TAG, "Garden exists in LEGACY path: user_gardens/$gardenId")
                } else {
                    Log.d(TAG, "Garden NOT found in legacy path: user_gardens/$gardenId")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error checking legacy path: ${exception.message}")
            }
    }
    
    /**
     * Try to load garden data by attempting multiple possible paths
     * This is a more robust approach when the exact garden ID structure is uncertain
     */
    private fun tryAlternativeGardenPaths(gardenId: String) {
        Log.d(TAG, "Trying alternative paths for garden: $gardenId")
        showProgress(true)
        
        // Get garden name from intent as an additional fallback
        val gardenName = intent.getStringExtra("GARDEN_NAME")
        
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated, cannot try alternative paths")
            showProgress(false)
            return
        }
        
        val userId = currentUser.uid
        
        // Build a more comprehensive list of possible IDs to try
        val possibleIds = mutableListOf<String>()
        
        // Add all the basic variations
        possibleIds.add(gardenId)                      // Original ID
        possibleIds.add(gardenId.lowercase())          // Lowercase
        possibleIds.add(gardenId.replace(" ", "_"))    // Replace spaces with underscores
        possibleIds.add(gardenId.replace("_", " "))    // Replace underscores with spaces
        possibleIds.add(gardenId.trim())               // Trimmed
        
        // Add user ID as a possibility
        possibleIds.add(userId)                        // Some apps use user ID directly
        
        // Add variations using garden name if available and different from ID
        if (gardenName != null && gardenName != gardenId) {
            possibleIds.add(gardenName)                // Original name
            possibleIds.add(gardenName.lowercase())    // Lowercase name
            possibleIds.add(gardenName.replace(" ", "_")) // Name with underscores
            possibleIds.add(gardenName.trim())         // Trimmed name
        }
        
        var checkedCount = 0
        var foundGarden = false
        
        // Try each possible ID
        for (possibleId in possibleIds.distinct()) { // Use distinct to avoid duplicates
            Log.d(TAG, "Trying alternative ID: $possibleId")
            
            db.collection("user_gardens")
                .document(possibleId)
                .get()
                .addOnCompleteListener { task ->
                    checkedCount++
                    
                    if (task.isSuccessful && task.result?.exists() == true && !foundGarden) {
                        // Found the garden with this ID
                        foundGarden = true
                        Log.d(TAG, "Found garden using alternative ID: $possibleId")
                        displayLegacyGardenData(task.result!!, possibleId)
                    }
                    
                    // If we've tried all possibilities and none worked
                    if (checkedCount == possibleIds.size && !foundGarden) {
                        Log.d(TAG, "No alternative garden IDs worked")
                        
                        // Instead of showing failure, use our more extensive search
                        Log.d(TAG, "Escalating to comprehensive ID variation search")
                        findGardenWithIDVariations(gardenId)
                    }
                }
        }
    }
    
    /**
     * Try different encodings of the garden ID that might be used in Firestore documents
     * Sometimes IDs can be encoded or transformed when stored
     */
    private fun tryEncodedGardenId(gardenId: String) {
        Log.d(TAG, "Trying different encodings of garden ID: $gardenId")
        
        // Common encodings to try
        val encodings = listOf(
            gardenId.trim(),                           // Remove whitespace
            gardenId.replace(" ", "-"),                // Replace spaces with hyphens
            gardenId.replace(" ", ""),                 // Remove spaces completely
            gardenId.replace("[^a-zA-Z0-9]".toRegex(), "_"), // Replace special chars with underscores
            gardenId.lowercase().trim()                // Lowercase and trim
        )
        
        for (encodedId in encodings) {
            if (encodedId == gardenId) continue // Skip if it's the same as original
            
            Log.d(TAG, "Trying encoded garden ID: $encodedId")
            db.collection("user_gardens")
                .document(encodedId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        Log.d(TAG, "Found garden with encoded ID: $encodedId")
                        Toast.makeText(this, "Found garden with modified ID: $encodedId", Toast.LENGTH_SHORT).show()
                        displayLegacyGardenData(doc, encodedId)
                    }
                }
        }
    }
    
    /**
     * Try loading garden details using both ID and name as fallbacks
     * This is especially helpful for legacy gardens where the ID and name might be used inconsistently
     */
    private fun loadGardenDetailsWithFallback(gardenId: String, gardenName: String, isLegacyPath: Boolean) {
        showProgress(true)
        Log.d(TAG, "Trying to load garden with both ID and name - ID: $gardenId, Name: $gardenName")
        
        // First, try with the ID in the legacy path
        db.collection("user_gardens")
            .document(gardenId)
            .get()
            .addOnSuccessListener { gardenDoc ->
                if (gardenDoc.exists()) {
                    // Success! Found with ID
                    Log.d(TAG, "Found garden with ID: $gardenId")
                    displayLegacyGardenData(gardenDoc, gardenId)
                    showProgress(false)
                } else {
                    // Try with the name instead
                    Log.d(TAG, "Garden not found with ID, trying with name: $gardenName")
                    db.collection("user_gardens")
                        .document(gardenName)
                        .get()
                        .addOnSuccessListener { nameDoc ->
                            if (nameDoc.exists()) {
                                // Success! Found with name
                                Log.d(TAG, "Found garden with name: $gardenName")
                                displayLegacyGardenData(nameDoc, gardenName)
                            } else {
                                // Neither worked, fall back to regular loading
                                Log.d(TAG, "Garden not found with either ID or name, falling back to regular methods")
                                loadLegacyGardenDetails(gardenId)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking garden by name: ${e.message}")
                            // Fall back to regular loading
                            loadLegacyGardenDetails(gardenId)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking garden by ID: ${e.message}")
                
                // Try with the name instead
                db.collection("user_gardens")
                    .document(gardenName)
                    .get()
                    .addOnSuccessListener { nameDoc ->
                        if (nameDoc.exists()) {
                            // Success! Found with name
                            Log.d(TAG, "Found garden with name: $gardenName")
                            displayLegacyGardenData(nameDoc, gardenName)
                        } else {
                            // Neither worked, fall back to regular loading
                            loadLegacyGardenDetails(gardenId)
                        }
                    }
                    .addOnFailureListener { nameError ->
                        Log.e(TAG, "Error checking garden by name: ${nameError.message}")
                        // Fall back to regular loading
                        loadLegacyGardenDetails(gardenId)
                    }
            }
    }
    
    /**
     * Find gardens by searching for a field value
     * This method tries to find a garden by matching any field's value (like name or other property)
     */
    private fun findGardenByFieldValue(fieldName: String, value: String) {
        Log.d(TAG, "Searching for garden with $fieldName = $value")
        showProgress(true)
        
        // Try in the legacy path first
        db.collection("user_gardens")
            .whereEqualTo(fieldName, value)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    Log.d(TAG, "Found garden by $fieldName = $value with ID: ${doc.id}")
                    displayLegacyGardenData(doc, doc.id)
                    showProgress(false)
                    return@addOnSuccessListener
                }
                
                // If not found in legacy path, try in standard path if user is authenticated
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userId = currentUser.uid
                    db.collection("user_data")
                        .document(userId)
                        .collection("user_gardens")
                        .whereEqualTo(fieldName, value)
                        .get()
                        .addOnSuccessListener { standardSnapshot ->
                            if (!standardSnapshot.isEmpty) {
                                val doc = standardSnapshot.documents.first()
                                Log.d(TAG, "Found garden by $fieldName = $value in standard path")
                                
                                // Set garden details
                                val gardenName = doc.id
                                val createdDate = doc.getTimestamp("createdDate")?.toDate() ?: Date()
                                val areaSize = doc.getDouble("areaSize") ?: doc.getDouble("area") ?: 0.0
                                val district = doc.getString("district") ?: "Not specified"
                                val soilType = doc.getString("soilType") ?: "Not specified"
                                val climate = doc.getString("climate") ?: "Not specified"
                                val rainfall = doc.getString("rainfall") ?: "Not specified"
                                val sunlight = doc.getString("sunlight") ?: "Not specified"
                                
                                updateUI(gardenName, createdDate, areaSize, district, soilType, climate, rainfall, sunlight)
                                loadPlantsForStandardGarden(userId, gardenName)
                            } else {
                                Log.d(TAG, "Garden not found by field search")
                                // As a very last resort, try the similarity matching
                                Log.d(TAG, "Trying final simplified similarity matching approach")
                                findPotentialGardenMatchesSimplified(value)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error searching standard path by field: ${e.message}")
                            showProgress(false)
                        }
                } else {
                    Log.d(TAG, "User not authenticated, cannot search standard path")
                    showProgress(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error searching legacy path by field: ${e.message}")
                showProgress(false)
            }
    }
    
    /**
     * Exhaustively try all possible garden ID variations with smart transformations
     * This is more thorough than the previous methods
     */
    private fun findGardenWithIDVariations(originalId: String) {
        Log.d(TAG, "Exhaustively searching for garden with all possible ID variations")
        showProgress(true)
        
        // Generate a wide range of possible variations
        val variations = generateAllPossibleIDVariations(originalId)
        
        Log.d(TAG, "Generated ${variations.size} possible ID variations to try")
        
        var checkedCount = 0
        var foundGarden = false
        
        // Create a list to store IDs we've already checked to avoid duplicates
        val checkedIds = mutableSetOf<String>()
        
        // Go through each variation
        for (id in variations) {
            // Skip if we've already checked this ID
            if (id in checkedIds) {
                checkedCount++
                continue
            }
            
            checkedIds.add(id)
            
            // Try in the legacy path
            db.collection("user_gardens")
                .document(id)
                .get()
                .addOnCompleteListener { task ->
                    checkedCount++
                    
                    if (task.isSuccessful && task.result?.exists() == true) {
                        // Found the garden with this ID variation
                        Log.d(TAG, "Found garden with ID variation: $id")
                        if (!foundGarden) {  // Only display the first match we find
                            foundGarden = true
                            displayLegacyGardenData(task.result!!, id)
                            Toast.makeText(this, "Found garden with modified ID: $id", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    // Check if we're done trying all variations
                    if (checkedCount >= variations.size && !foundGarden) {
                        // Last attempt - try to search by name value
                        Log.d(TAG, "All ID variations failed, trying to search by name field")
                        findGardenByFieldValue("name", originalId)
                        
                        // If even findGardenByFieldValue fails, it will call our final method
                        // as a last resort - similarity matching across all accessible gardens
                        runOnUiThread {
                            // Immediately try the final approach
                            Log.d(TAG, "All previous methods failed, trying similarity matching")
                            findPotentialGardenMatchesSimplified(originalId)
                        }
                    }
                }
        }
    }
    
    /**
     * Generate all possible variations of a garden ID
     */
    private fun generateAllPossibleIDVariations(originalId: String): List<String> {
        val variations = mutableListOf<String>()
        
        // Original and basic variations
        variations.add(originalId)
        variations.add(originalId.lowercase())
        variations.add(originalId.trim())
        
        // Special character handling
        variations.add(originalId.replace(" ", "_"))
        variations.add(originalId.replace("_", " "))
        variations.add(originalId.replace(" ", "-"))
        variations.add(originalId.replace("-", "_"))
        variations.add(originalId.replace(" ", ""))
        variations.add(originalId.replace("[^a-zA-Z0-9]".toRegex(), "_"))
        variations.add(originalId.replace("[^a-zA-Z0-9]".toRegex(), ""))
        
        // Case variations with special character handling
        variations.add(originalId.lowercase().replace(" ", "_"))
        variations.add(originalId.lowercase().replace(" ", "-"))
        variations.add(originalId.lowercase().replace(" ", ""))
        variations.add(originalId.lowercase().trim())
        variations.add(originalId.lowercase().replace("[^a-zA-Z0-9]".toRegex(), "_"))
        variations.add(originalId.lowercase().replace("[^a-zA-Z0-9]".toRegex(), ""))
        
        // Try to separate words by common delimiters and recombine
        val words = originalId.split(" ", "_", "-")
        if (words.size > 1) {
            variations.add(words.joinToString(""))
            variations.add(words.joinToString("_"))
            variations.add(words.joinToString("-"))
        }
        
        // Common garden name transformations
        if (originalId.contains("garden", ignoreCase = true)) {
            variations.add(originalId.replace("garden", "", ignoreCase = true).trim())
            variations.add(originalId.replace("garden", "_", ignoreCase = true).trim())
        }
        
        // Try with the authenticated user's ID appended/prefixed
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            variations.add("${userId}_$originalId")
            variations.add("${originalId}_$userId")
            variations.add(userId)
        }
        
        return variations.distinct() // Remove duplicates
    }
    
    /**
     * Query all accessible gardens and try to find potential matches by comparing name similarities
     * This is a last resort when all other methods fail
     */
    private fun findPotentialGardenMatches(nameToMatch: String) {
        Log.d(TAG, "Querying all gardens to find potential matches for: $nameToMatch")
        showProgress(true)
        
        // Try in the legacy path first with a larger limit
        db.collection("user_gardens")
            .limit(50)  // Limit to avoid excessive document reads
            .get()
            .addOnSuccessListener { querySnapshot ->
                val matches = findBestMatches(querySnapshot.documents, nameToMatch)
                
                if (matches.isNotEmpty()) {
                    // Found potential matches
                    val bestMatch = matches.first()
                    Log.d(TAG, "Found potential garden match: ${bestMatch.id} (similarity score)")
                    displayLegacyGardenData(bestMatch, bestMatch.id)
                    Toast.makeText(this, "Found similar garden: ${bestMatch.id}", Toast.LENGTH_SHORT).show()
                    showProgress(false)
                    return@addOnSuccessListener
                }
                
                // If not found in legacy path, try in standard path if user is authenticated
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val userId = currentUser.uid
                    db.collection("user_data")
                        .document(userId)
                        .collection("user_gardens")
                        .get()
                        .addOnSuccessListener { standardSnapshot ->
                            val standardMatches = findBestMatches(standardSnapshot.documents, nameToMatch)
                            
                            if (standardMatches.isNotEmpty()) {
                                // Found potential matches in standard path
                                val bestMatch = standardMatches.first()
                                Log.d(TAG, "Found potential garden match in standard path: ${bestMatch.id}")
                                
                                // Set garden details
                                val gardenName = bestMatch.id
                                val createdDate = bestMatch.getTimestamp("createdDate")?.toDate() ?: Date()
                                val areaSize = bestMatch.getDouble("areaSize") ?: bestMatch.getDouble("area") ?: 0.0
                                val district = bestMatch.getString("district") ?: "Not specified"
                                val soilType = bestMatch.getString("soilType") ?: "Not specified"
                                val climate = bestMatch.getString("climate") ?: "Not specified"
                                val rainfall = bestMatch.getString("rainfall") ?: "Not specified"
                                val sunlight = bestMatch.getString("sunlight") ?: "Not specified"
                                
                                updateUI(gardenName, createdDate, areaSize, district, soilType, climate, rainfall, sunlight)
                                loadPlantsForStandardGarden(userId, gardenName)
                            } else {
                                // No matches found anywhere - this is our final failure case
                                Log.d(TAG, "No potential garden matches found - all methods exhausted")
                                showProgress(false)
                                
                                // Show a detailed error message
                                Toast.makeText(this, 
                                    "Could not find any garden that matches. Displaying diagnostic information...",
                                    Toast.LENGTH_LONG).show()
                                    
                                // Show diagnostic information about all available gardens
                                listAllAvailableGardens()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error searching standard path for matches: ${e.message}")
                            showProgress(false)
                            Toast.makeText(this, "Error finding garden: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // No matches and user not authenticated
                    Log.d(TAG, "No potential garden matches found and user not authenticated")
                    showProgress(false)
                    Toast.makeText(this, 
                        "Could not find any garden that matches. Please try logging in again.",
                        Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error searching legacy path for matches: ${e.message}")
                showProgress(false)
                Toast.makeText(this, "Error finding garden: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * Simplified method to find potential garden matches
     */
    private fun findPotentialGardenMatchesSimplified(nameToMatch: String) {
        Log.d(TAG, "Simplified search for potential garden matches: $nameToMatch")
        showProgress(true)
        
        // Try to find all gardens in the legacy path
        db.collection("user_gardens")
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot -> 
                if (!snapshot.isEmpty) {
                    // Found gardens, try to find best match
                    val match = findBestGardenMatch(snapshot.documents, nameToMatch)
                    if (match != null) {
                        // Found a match
                        Log.d(TAG, "Found matching garden: ${match.id}")
                        displayLegacyGardenData(match, match.id)
                        Toast.makeText(this, "Found garden: ${match.id}", Toast.LENGTH_SHORT).show()
                        showProgress(false)
                        return@addOnSuccessListener
                    }
                }
                
                // If no match found in legacy path, try standard path
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    try {
                        db.collection("user_data")
                            .document(currentUser.uid)
                            .collection("user_gardens")
                            .get()
                            .addOnSuccessListener { standardSnapshot ->
                                if (!standardSnapshot.isEmpty) {
                                    val match = findBestGardenMatch(standardSnapshot.documents, nameToMatch)
                                    if (match != null) {
                                        // Found a match in standard path
                                        val gardenName = match.id
                                        val createdDate = match.getTimestamp("createdDate")?.toDate() ?: Date()
                                        val areaSize = match.getDouble("areaSize") ?: match.getDouble("area") ?: 0.0
                                        val district = match.getString("district") ?: "Not specified"
                                        val soilType = match.getString("soilType") ?: "Not specified"
                                        val climate = match.getString("climate") ?: "Not specified"
                                        val rainfall = match.getString("rainfall") ?: "Not specified"
                                        val sunlight = match.getString("sunlight") ?: "Not specified"
                                        
                                        updateUI(gardenName, createdDate, areaSize, district, soilType, 
                                            climate, rainfall, sunlight)
                                        loadPlantsForStandardGarden(currentUser.uid, gardenName)
                                        return@addOnSuccessListener
                                    }
                                }
                                
                                // If we get here, no matches found in either path
                                showProgress(false)
                                // Silently handle garden not found
                                Log.w(TAG, "Garden not found in any location")
                                listAllAvailableGardens()
                            }
                            .addOnFailureListener { e ->
                                showProgress(false)
                                Log.e(TAG, "Error searching standard path: ${e.message}")
                            }
                    } catch (e: Exception) {
                        showProgress(false)
                        Log.e(TAG, "Exception searching standard path: ${e.message}")
                    }
                } else {
                    // Not authenticated
                    showProgress(false)
                    Toast.makeText(this, "Please login to search for gardens", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                showProgress(false)
                Log.e(TAG, "Error searching legacy gardens: ${e.message}")
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * Helper method to find the best garden match
     */
    private fun findBestGardenMatch(documents: List<com.google.firebase.firestore.DocumentSnapshot>, nameToMatch: String): com.google.firebase.firestore.DocumentSnapshot? {
        // Simple matching logic - find exact match first
        for (doc in documents) {
            if (doc.id.equals(nameToMatch, ignoreCase = true)) {
                return doc
            }
        }

        // No exact match, try contains
        for (doc in documents) {
            if (doc.id.contains(nameToMatch, ignoreCase = true) ||
                nameToMatch.contains(doc.id, ignoreCase = true)) {
                return doc
            }
        }

        // No name matches, just return the first document as fallback
        return if (documents.isNotEmpty()) documents.first() else null
    }

    /**
     * Find the best matching documents based on name similarity
     */
    private fun findBestMatches(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>,
        nameToMatch: String
    ): List<com.google.firebase.firestore.DocumentSnapshot> {
        // Simple similarity metrics to compare strings
        fun calculateSimilarity(s1: String, s2: String): Double {
            val s1Lower = s1.lowercase()
            val s2Lower = s2.lowercase()

            // Exact match gets highest score
            if (s1Lower == s2Lower) return 1.0

            // Contains relationship
            if (s1Lower.contains(s2Lower)) return 0.9
            if (s2Lower.contains(s1Lower)) return 0.8

            // Calculate word overlap
            val words1 = s1Lower.split(" ", "_", "-")
            val words2 = s2Lower.split(" ", "_", "-")

            val commonWords = words1.intersect(words2.toSet())
            if (commonWords.isNotEmpty()) {
                // Percentage of words in common
                return commonWords.size.toDouble() / Math.max(words1.size, words2.size).toDouble() * 0.7
            }

            // Basic character similarity as last resort
            val maxLength = Math.max(s1Lower.length, s2Lower.length)
            if (maxLength > 0) {
                var sameChars = 0
                val minLength = Math.min(s1Lower.length, s2Lower.length)

                for (i in 0 until minLength) {
                    if (s1Lower[i] == s2Lower[i]) {
                        sameChars++
                    }
                }

                return sameChars.toDouble() / maxLength.toDouble() * 0.5
            }

            return 0.0
        }

        // Create a list of documents with their similarity scores
        val scoredDocs = documents.map { doc ->
            // Compare both ID and name fields
            val idSimilarity = calculateSimilarity(doc.id, nameToMatch)

            // Check if there's a name field and if so, check its similarity too
            val nameSimilarity = if (doc.contains("name")) {
                doc.getString("name")?.let { calculateSimilarity(it, nameToMatch) } ?: 0.0
            } else {
                0.0
            }

            // Use max of ID and name similarity
            val maxSimilarity = Math.max(idSimilarity, nameSimilarity)
            Pair(doc, maxSimilarity)
        }
        
        // Filter those with reasonable similarity and sort by similarity (descending)
        return scoredDocs
            .filter { it.second > 0.3 } // Only keep those with reasonable similarity
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Lists all available gardens as a diagnostic measure
     */
    private fun listAllAvailableGardens() {
        Log.d(TAG, "Attempting to list all available gardens as a diagnostic measure")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "Cannot show available gardens - user not authenticated")
            return
        }

        val userId = currentUser.uid
        val availableGardens = mutableListOf<String>()

        // Check legacy path
        db.collection("user_gardens")
            .limit(30) // Limit results for performance
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    Log.d(TAG, "Found ${querySnapshot.size()} gardens in legacy path")
                    for (doc in querySnapshot.documents) {
                        availableGardens.add("Legacy: ${doc.id}")
                    }

                    // Now check standard path
                    db.collection("user_data")
                        .document(userId)
                        .collection("user_gardens")
                        .get()
                        .addOnSuccessListener { standardSnapshot ->
                            if (!standardSnapshot.isEmpty) {
                                Log.d(TAG, "Found ${standardSnapshot.size()} gardens in standard path")
                                for (doc in standardSnapshot.documents) {
                                    availableGardens.add("Standard: ${doc.id}")
                                }
                            }

                            // Show the list in a log message for debugging
                            Log.d(TAG, "Available gardens: ${availableGardens.joinToString(", ")}")

                            // Also show a toast with a summary
                            Toast.makeText(this,
                                "Found ${availableGardens.size} gardens in your account. " +
                                "See logs for details.",
                                Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error getting standard gardens: ${e.message}")
                        }
                } else {
                    Log.d(TAG, "No gardens found in legacy path")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting legacy gardens: ${e.message}")
            }
    }
    
    private fun logPlantFieldValues(plantDoc: DocumentSnapshot) {
        Log.d(TAG, "Plant document: ${plantDoc.id}")
        Log.d(TAG, "Raw growthPeriod value: ${plantDoc.get("growthPeriod")}")
        Log.d(TAG, "Raw growthPeriodDays value: ${plantDoc.get("growthPeriodDays")}")
        
        // Try to convert the values to see if there are type conversion issues
        val growthPeriodValue = plantDoc.getLong("growthPeriod")
        val growthPeriodDaysValue = plantDoc.getLong("growthPeriodDays")
        
        Log.d(TAG, "Converted growthPeriod: $growthPeriodValue")
        Log.d(TAG, "Converted growthPeriodDays: $growthPeriodDaysValue")
    }
    
    /**
     * Deletes a plant from Firebase
     */
    private fun deletePlantFromFirebase(plant: Plant, gardenId: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot delete plant: User not authenticated")
            Toast.makeText(this, "You need to be logged in to delete plants", Toast.LENGTH_SHORT).show()
            return
        }

        showProgress(true)
        Log.d(TAG, "Deleting plant: ${plant.name} (id: ${plant.id}) from garden: $gardenId")
        
        db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(gardenId)
            .collection("plants")
            .document(plant.id)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Plant successfully deleted: ${plant.name}")
                Toast.makeText(this, "${plant.name} successfully deleted", Toast.LENGTH_SHORT).show()
                
                // Reload the plants to refresh the list
                loadPlantsForStandardGarden(userId, gardenId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting plant: ${e.message}")
                Toast.makeText(this, "Error deleting plant: ${e.message}", Toast.LENGTH_SHORT).show()
                showProgress(false)
            }
    }
}



