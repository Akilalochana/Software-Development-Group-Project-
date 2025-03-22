package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arlandmeasuretest33.adapters.PlantCatalogAdapter
import com.example.arlandmeasuretest33.models.PlantCategory
import com.google.firebase.firestore.FirebaseFirestore

class PlantInformationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var backButton: ImageView
    private lateinit var adapter: PlantCatalogAdapter
    private val plantCategories = ArrayList<PlantCategory>()
    private lateinit var db: FirebaseFirestore
    private lateinit var noResultsText: TextView
    private val TAG = "PlantInfoActivity"

    // List of districts to check for plants
    private val districts = listOf("Ampara", "Colombo", "Badulla", "Anuradhapura", "Batticaloa", "Galle")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_information)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize views
        recyclerView = findViewById(R.id.plantsRecyclerView)
        searchView = findViewById(R.id.searchView)
        backButton = findViewById(R.id.backButton)
        
        try {
            noResultsText = findViewById(R.id.noResultsText)
            noResultsText.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "No results text view not found: ${e.message}")
        }

        // Set up back button
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Set up RecyclerView
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        adapter = PlantCatalogAdapter(plantCategories) { plantCategory ->
            // Handle plant category click - launch detail activity
            val intent = Intent(this, PlantInformationDetailActivity::class.java)
            intent.putExtra("PLANT_NAME", plantCategory.name)
            intent.putExtra("DISTRICT", plantCategory.district)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Set up search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPlants(newText)
                return true
            }
        })

        // Ensure priority plants exist in the database
        ensurePriorityPlantsInDatabase()

        // Load plants from Firestore
        loadPlantsFromFirestore()
    }

    private fun ensurePriorityPlantsInDatabase() {
        // Define plant data for each priority plant
        val priorityPlantsData = mapOf(
            "Bitter Melon" to mapOf(
                "category" to "Vegetable",
                "description" to "Bitter melon is a tropical vine with distinct warty fruits. It has medicinal properties and is used in many Asian cuisines.",
                "image" to "https://www.gardeningknowhow.com/wp-content/uploads/2017/09/bitter-melon.jpg",
                "cost_per_unit" to 10.0,
                "fertilizer" to 18,
                "expected_yield_per_plant" to 950,
                "growth_cycle_duration" to 70,
                "space" to "150 x 50"
            ),
            "Winged Bean" to mapOf(
                "category" to "Vegetable",
                "description" to "Winged bean is a tropical legume plant with edible pods, leaves, flowers, and seeds. It is highly nutritious and has a high protein content.",
                "image" to "https://specialtyproduce.com/sppics/9879.png",
                "cost_per_unit" to 12.0,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 800,
                "growth_cycle_duration" to 90,
                "space" to "60 x 30"
            ),
            "Red Spinach" to mapOf(
                "category" to "Vegetable",
                "description" to "Red spinach is a leafy vegetable known for its red-purple colored leaves. It is rich in iron, vitamins, and antioxidants.",
                "image" to "https://m.media-amazon.com/images/I/71K1zYQXYVL._AC_UF1000,1000_QL80_.jpg",
                "cost_per_unit" to 7.0,
                "fertilizer" to 20,
                "expected_yield_per_plant" to 500,
                "growth_cycle_duration" to 45,
                "space" to "30 x 15"
            ),
            "Beetroot" to mapOf(
                "category" to "Vegetable",
                "description" to "Beetroot is a root vegetable with a deep red color. It is rich in fiber, vitamins, and minerals, and has been linked to improved athletic performance.",
                "image" to "https://cdn.shopify.com/s/files/1/0274/3481/articles/BEETROOT-HERO.jpg?v=1518095572",
                "cost_per_unit" to 9.0,
                "fertilizer" to 22,
                "expected_yield_per_plant" to 750,
                "growth_cycle_duration" to 60,
                "space" to "30 x 10"
            ),
            "Brinjal" to mapOf(
                "category" to "Vegetable",
                "description" to "Brinjal, also known as eggplant, is a highly marketable and nutritious vegetable. It grows well up to 1300m above sea level and is widely cultivated for its high yield and adaptability to different climates.",
                "image" to "https://gourmetgarden.in/cdn/shop/products/Brinjal_Long_Purple_3_875d-466f-8625-3e8da2207e1d.jpg?v=1742037242",
                "cost_per_unit" to 9.75,
                "fertilizer" to 22,
                "expected_yield_per_plant" to 3500,
                "growth_cycle_duration" to 85,
                "space" to "90 x 60"
            ),
            "Carrots" to mapOf(
                "category" to "Vegetable",
                "description" to "Carrot is a root vegetable, usually orange in color. It is rich in beta carotene and other nutrients, making it a popular health food.",
                "image" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Vegetable-Carrot-Bundle-wStalks.jpg/1200px-Vegetable-Carrot-Bundle-wStalks.jpg",
                "cost_per_unit" to 8.5,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 250,
                "growth_cycle_duration" to 70,
                "space" to "20 x 5"
            ),
            "Cabbage" to mapOf(
                "category" to "Vegetable",
                "description" to "Cabbage is a leafy vegetable that grows in large compact heads. It is rich in vitamin C and fiber, and is used in many cuisines around the world.",
                "image" to "https://upload.wikimedia.org/wikipedia/commons/6/6f/Cabbage_in_a_stack.jpg",
                "cost_per_unit" to 11.0,
                "fertilizer" to 25,
                "expected_yield_per_plant" to 2500,
                "growth_cycle_duration" to 90,
                "space" to "60 x 45"
            ),
            "Leeks" to mapOf(
                "category" to "Vegetable",
                "description" to "Leek is a vegetable in the allium family, related to onions and garlic. It has a milder, sweeter flavor than onions and is used in soups, stews, and salads.",
                "image" to "https://thumbs.dreamstime.com/b/leek-vegetable-white-space-32225693.jpg",
                "cost_per_unit" to 10.0,
                "fertilizer" to 20,
                "expected_yield_per_plant" to 600,
                "growth_cycle_duration" to 120,
                "space" to "30 x 15"
            ),
            "Potato" to mapOf(
                "category" to "Vegetable",
                "description" to "Potato is a starchy root vegetable that is a staple food in many countries. It is rich in carbohydrates and vitamin C.",
                "image" to "https://cdn.mos.cms.futurecdn.net/iC7HBvohbJqExqvbKcV3pP.jpg",
                "cost_per_unit" to 15.0,
                "fertilizer" to 25,
                "expected_yield_per_plant" to 1200,
                "growth_cycle_duration" to 100,
                "space" to "60 x 30"
            ),
            "Onion" to mapOf(
                "category" to "Vegetable",
                "description" to "Onion is a versatile vegetable used in almost all cuisines. It is rich in vitamins and minerals, and has medicinal properties.",
                "image" to "https://plantix.net/en/library/assets/custom/crop-images/onion.jpeg",
                "cost_per_unit" to 12.5,
                "fertilizer" to 18,
                "expected_yield_per_plant" to 800,
                "growth_cycle_duration" to 90,
                "space" to "30 x 15"
            ),
            "Manioc" to mapOf(
                "category" to "Vegetable",
                "description" to "Manioc, also known as cassava, is a root vegetable. It is a staple food in many tropical regions due to its drought tolerance and high carbohydrate content.",
                "image" to "https://www.shutterstock.com/image-photo/cassava-root-vegetable-isolated-on-260nw-315185849.jpg",
                "cost_per_unit" to 8.0,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 5000,
                "growth_cycle_duration" to 270,
                "space" to "100 x 100"
            ),
            "Taro" to mapOf(
                "category" to "Vegetable",
                "description" to "Taro is a starchy root vegetable widely cultivated in tropical regions. Its large, elephant ear-like leaves and corm are edible but must be cooked properly.",
                "image" to "https://static.toiimg.com/photo/msid-81246913/81246913.jpg",
                "cost_per_unit" to 12.0,
                "fertilizer" to 18,
                "expected_yield_per_plant" to 3000,
                "growth_cycle_duration" to 200,
                "space" to "90 x 60"
            ),
            "Eggplant" to mapOf(
                "category" to "Vegetable",
                "description" to "Eggplant, also known as brinjal, is a widely cultivated vegetable with a smooth, glossy skin and meaty texture. It is rich in fiber and antioxidants.",
                "image" to "https://gourmetgarden.in/cdn/shop/products/Brinjal_Long_Purple_3_875d-466f-8625-3e8da2207e1d.jpg?v=1742037242",
                "cost_per_unit" to 9.75,
                "fertilizer" to 22,
                "expected_yield_per_plant" to 3500,
                "growth_cycle_duration" to 85,
                "space" to "90 x 60"
            ),
            "Pumpkin" to mapOf(
                "category" to "Vegetable",
                "description" to "Pumpkin is a winter squash with a sweet, earthy flavor. It is rich in vitamins and minerals, particularly vitamin A and potassium.",
                "image" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/FrenchMarketPumpkinsB.jpg/800px-FrenchMarketPumpkinsB.jpg",
                "cost_per_unit" to 15.0,
                "fertilizer" to 25,
                "expected_yield_per_plant" to 10000,
                "growth_cycle_duration" to 120,
                "space" to "180 x 180"
            ),
            "Knolkhol" to mapOf(
                "category" to "Vegetable",
                "description" to "Knolkhol, also known as kohlrabi, is a vegetable related to cabbage, with a swollen stem that resembles a turnip. It has a crisp texture and a mild, sweet flavor.",
                "image" to "https://img.freepik.com/premium-photo/kohlrabi-white-background_253984-5071.jpg",
                "cost_per_unit" to 9.0,
                "fertilizer" to 20,
                "expected_yield_per_plant" to 750,
                "growth_cycle_duration" to 60,
                "space" to "30 x 15"
            ),
            "Drumstick" to mapOf(
                "category" to "Vegetable",
                "description" to "Drumstick is the fruit of the Moringa tree. It is highly nutritious and used in various cuisines, particularly in South Asian dishes like sambar and curry.",
                "image" to "https://static.toiimg.com/thumb/79890873.cms?width=400&height=300&resizemode=4&imgsize=290147",
                "cost_per_unit" to 14.0,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 5000,
                "growth_cycle_duration" to 270,
                "space" to "300 x 300"
            )
        )
        
        // Special handling for Taro and Winged Bean to ensure proper images
        ensureSpecialPlantsInAllDistricts()
        
        // For each priority plant, check if it exists in at least one district
        for ((plantName, plantData) in priorityPlantsData) {
            // Default district to write to if not found
            val defaultDistrict = "Ampara"
            
            // Special handling for cabbage, leeks, and taro - ensure they exist and have proper images
            if (plantName.equals("Cabbage", ignoreCase = true) || 
                plantName.equals("Leeks", ignoreCase = true)) {
                
                Log.d(TAG, "Ensuring database has proper data for priority plant: $plantName")
                
                // Force update in Ampara district first to ensure it exists
                db.collection("districts").document(defaultDistrict)
                    .collection("crops").document(plantName)
                    .set(plantData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully added/updated priority plant '$plantName' to database")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to add/update priority plant '$plantName' to database: ${e.message}")
                    }
                
                // Also add to Colombo as a backup
                db.collection("districts").document("Colombo")
                    .collection("crops").document(plantName)
                    .set(plantData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully added backup for '$plantName' to Colombo district")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to add backup for '$plantName' to Colombo district: ${e.message}")
                    }
            } else if (!plantName.equals("Taro", ignoreCase = true) && 
                       !plantName.equals("Winged Bean", ignoreCase = true)) {
                // Standard flow for other plants (except Taro and Winged Bean which are handled separately)
                checkIfPlantExistsInAnyDistrict(plantName) { exists ->
                    if (!exists) {
                        // Plant doesn't exist, add it to database
                        Log.d(TAG, "Adding missing plant '$plantName' to database in $defaultDistrict")
                        db.collection("districts").document(defaultDistrict)
                            .collection("crops").document(plantName)
                            .set(plantData)
                            .addOnSuccessListener {
                                Log.d(TAG, "Successfully added plant '$plantName' to database")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to add plant '$plantName' to database: ${e.message}")
                            }
                    } else {
                        Log.d(TAG, "Plant '$plantName' already exists in the database")
                    }
                }
            }
        }
    }
    
    private fun ensureSpecialPlantsInAllDistricts() {
        // Handle special cases for Taro and Winged Bean/Winged_bean
        val specialPlantsData = mapOf(
            "Taro" to mapOf(
                "category" to "Vegetable",
                "description" to "Taro is a starchy root vegetable widely cultivated in tropical regions. Its large, elephant ear-like leaves and corm are edible but must be cooked properly.",
                "image" to "https://static.toiimg.com/photo/msid-81246913/81246913.jpg",
                "cost_per_unit" to 12.0,
                "fertilizer" to 18,
                "expected_yield_per_plant" to 3000,
                "growth_cycle_duration" to 200,
                "space" to "90 x 60"
            ),
            "Winged_bean" to mapOf(
                "category" to "Vegetable",
                "description" to "Winged bean is a tropical legume plant with edible pods, leaves, flowers, and seeds. It is highly nutritious and has a high protein content.",
                "image" to "https://specialtyproduce.com/sppics/9879.png",
                "cost_per_unit" to 12.0,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 800,
                "growth_cycle_duration" to 90,
                "space" to "60 x 30"
            )
        )
        
        // These districts specifically have these plants according to the screenshots
        val targetDistricts = listOf("Kalutara", "Hambantota", "Ampara", "Colombo")
        
        for ((plantName, plantData) in specialPlantsData) {
            for (district in targetDistricts) {
                // Force update each plant in each target district
                Log.d(TAG, "Force adding/updating '$plantName' in $district district")
                
                db.collection("districts").document(district)
                    .collection("crops").document(plantName)
                    .set(plantData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully added/updated '$plantName' to $district district")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to add/update '$plantName' to $district district: ${e.message}")
                    }
            }
        }
    }
    
    private fun checkIfPlantExistsInAnyDistrict(plantName: String, callback: (Boolean) -> Unit) {
        var districtsChecked = 0
        var plantFound = false
        
        for (district in districts) {
            db.collection("districts").document(district).collection("crops")
                .get()
                .addOnSuccessListener { documents ->
                    if (!plantFound) {
                        // Check if this plant exists (case-insensitive)
                        for (doc in documents) {
                            if (doc.id.equals(plantName, ignoreCase = true)) {
                                plantFound = true
                                break
                            }
                        }
                    }
                    
                    districtsChecked++
                    if (districtsChecked == districts.size) {
                        callback(plantFound)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking if plant '$plantName' exists in $district: ${e.message}")
                    
                    districtsChecked++
                    if (districtsChecked == districts.size) {
                        callback(plantFound)
                    }
                }
        }
    }

    private fun loadPlantsFromFirestore() {
        Log.d(TAG, "Starting to load plants from Firestore")
        plantCategories.clear()
        
        // Track if we found any plants at all
        var plantsFound = false
        
        // Priority plants to ensure they're included
        val priorityPlants = listOf(
            "Bitter Melon", "Winged Bean", "Red Spinach", 
            "Beetroot", "Brinjal", "Carrots", "Cabbage", "Leeks", "Potato", 
            "Onion", "Manioc", "Taro", "Eggplant", "Pumpkin", "Knolkhol", "Drumstick"
        )
        
        // Count to track how many districts we've processed
        var districtsProcessed = 0
        
        // Use a LinkedHashMap to track unique plants by name (case-insensitive)
        val uniquePlants = LinkedHashMap<String, PlantCategory>()
        
        // Check all districts for plants
        for (district in districts) {
            db.collection("districts").document(district).collection("crops")
                .get()
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "Found ${documents.size()} plants in $district")
                    
                    for (document in documents) {
                        try {
                            val plantName = document.id
                            // Get the category (vegetable, fruit, etc.) or default to "Other"
                            val category = document.getString("category") ?: "Vegetable"
                            
                            // Only add the plant if we haven't seen it before (case-insensitive)
                            val plantNameLower = plantName.lowercase()
                            if (!uniquePlants.containsKey(plantNameLower)) {
                                val plant = PlantCategory(
                                    name = plantName,
                                    category = category,
                                    // Use default drawable resource - the adapter will handle mapping to the correct image
                                    imageResource = R.drawable.ic_plant,
                                    district = district
                                )
                                uniquePlants[plantNameLower] = plant
                                plantsFound = true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing document ${document.id}: ${e.message}")
                        }
                    }
                    
                    // Increment processed count
                    districtsProcessed++
                    
                    // If we've processed all districts, update the UI
                    if (districtsProcessed == districts.size) {
                        // Check for missing priority plants and search for them specifically
                        val missingPlants = priorityPlants.filter { priority ->
                            !uniquePlants.keys.any { it.equals(priority.lowercase(), ignoreCase = true) }
                        }
                        
                        if (missingPlants.isNotEmpty()) {
                            Log.d(TAG, "Missing priority plants: $missingPlants")
                            fetchMissingPriorityPlants(missingPlants, uniquePlants) {
                                // Add all unique plants to our list
                                plantCategories.addAll(uniquePlants.values)
                                
                                // Log the final plant count
                                Log.d(TAG, "Finished loading from all districts. Found ${plantCategories.size} unique plants")
                                
                                // Check if we found any plants
                                if (uniquePlants.isNotEmpty()) {
                                    adapter.notifyDataSetChanged()
                                    hideNoResultsMessage()
                                } else {
                                    // No plants found, add hardcoded sample data
                                    loadHardcodedPlantCategories()
                                }
                            }
                        } else {
                            // All priority plants found, add all unique plants to our list
                            plantCategories.addAll(uniquePlants.values)
                            
                            // Log the final plant count
                            Log.d(TAG, "Finished loading from all districts. Found ${plantCategories.size} unique plants")
                            
                            // Check if we found any plants
                            if (plantsFound) {
                                adapter.notifyDataSetChanged()
                                hideNoResultsMessage()
                            } else {
                                // No plants found, add hardcoded sample data
                                loadHardcodedPlantCategories()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting plants from $district: ${e.message}")
                    
                    // Increment processed count even on failure
                    districtsProcessed++
                    
                    // If we've processed all districts, update the UI
                    if (districtsProcessed == districts.size && !plantsFound) {
                        // No plants found in any district, add hardcoded sample data
                        loadHardcodedPlantCategories()
                    }
                }
        }
    }
    
    private fun fetchMissingPriorityPlants(
        missingPlants: List<String>, 
        uniquePlants: LinkedHashMap<String, PlantCategory>,
        onComplete: () -> Unit
    ) {
        // Try to retrieve each missing plant with a dedicated query
        var plantsProcessed = 0
        
        // If no missing plants, just complete
        if (missingPlants.isEmpty()) {
            onComplete()
            return
        }
        
        // For each missing plant, try all districts with a case-insensitive query
        for (plantName in missingPlants) {
            var plantFound = false
            var districtsChecked = 0
            
            for (district in districts) {
                db.collection("districts").document(district).collection("crops")
                    .get()
                    .addOnSuccessListener { documents ->
                        // Try to find a case-insensitive match
                        for (doc in documents) {
                            if (doc.id.equals(plantName, ignoreCase = true)) {
                                Log.d(TAG, "Found missing plant '$plantName' as '${doc.id}' in $district")
                                val category = doc.getString("category") ?: "Vegetable"
                                val plant = PlantCategory(
                                    name = doc.id,
                                    category = category,
                                    // Use default drawable - adapter will map to correct image
                                    imageResource = R.drawable.ic_plant,
                                    district = district
                                )
                                uniquePlants[doc.id.lowercase()] = plant
                                plantFound = true
                                break
                            }
                        }
                        
                        districtsChecked++
                        
                        // If all districts checked for this plant or plant found
                        if (districtsChecked == districts.size || plantFound) {
                            if (!plantFound) {
                                Log.d(TAG, "Could not find missing plant '$plantName' in any district")
                                // Add a default entry for the plant to ensure it's displayed
                                val plant = PlantCategory(
                                    name = plantName,
                                    category = "Vegetable",
                                    // Use default drawable - adapter will map to correct image
                                    imageResource = R.drawable.ic_plant,
                                    district = "Default"
                                )
                                uniquePlants[plantName.lowercase()] = plant
                            }
                            
                            plantsProcessed++
                            
                            // If all plants processed, call onComplete
                            if (plantsProcessed == missingPlants.size) {
                                onComplete()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error searching for missing plant '$plantName' in $district: ${e.message}")
                        
                        districtsChecked++
                        
                        // If all districts checked for this plant
                        if (districtsChecked == districts.size) {
                            // Add a default entry for the plant to ensure it's displayed
                            val plant = PlantCategory(
                                name = plantName,
                                category = "Vegetable",
                                // Use default drawable - adapter will map to correct image
                                imageResource = R.drawable.ic_plant,
                                district = "Default"
                            )
                            uniquePlants[plantName.lowercase()] = plant
                            
                            plantsProcessed++
                            
                            // If all plants processed, call onComplete
                            if (plantsProcessed == missingPlants.size) {
                                onComplete()
                            }
                        }
                    }
            }
        }
    }
    
    private fun loadHardcodedPlantCategories() {
        Log.d(TAG, "Loading hardcoded plant categories as fallback")
        
        // Use a LinkedHashMap to ensure unique plants
        val uniquePlants = LinkedHashMap<String, PlantCategory>()
        
        // Add hardcoded plants
        val hardcodedPlants = listOf(
            PlantCategory("Pumpkin", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Bitter Melon", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Winged Bean", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Red Spinach", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Beetroot", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Brinjal", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Carrot", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Cabbage", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Leeks", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Potato", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Onion", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Manioc", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Taro", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Eggplant", "Vegetables", R.drawable.ic_plant, "Ampara"),
            PlantCategory("Drumstick", "Vegetables", R.drawable.ic_plant, "Ampara")
        )
        
        // Add each plant to our map to ensure uniqueness
        for (plant in hardcodedPlants) {
            uniquePlants[plant.name.lowercase()] = plant
        }
        
        // Add all unique plants to our list
        plantCategories.addAll(uniquePlants.values)
        
        adapter.notifyDataSetChanged()
        hideNoResultsMessage()
    }

    private fun filterPlants(query: String?) {
        if (query.isNullOrEmpty()) {
            adapter.updateList(plantCategories)
            hideNoResultsMessage()
        } else {
            val filteredList = plantCategories.filter { 
                it.name.lowercase().contains(query.lowercase()) || 
                it.category.lowercase().contains(query.lowercase()) 
            }
            
            adapter.updateList(filteredList)
            
            // Show "no results" message if filtered list is empty
            if (filteredList.isEmpty()) {
                showNoResultsMessage()
            } else {
                hideNoResultsMessage()
            }
        }
    }
    
    private fun showNoResultsMessage() {
        try {
            if (::noResultsText.isInitialized) {
                noResultsText.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing no results message: ${e.message}")
        }
    }
    
    private fun hideNoResultsMessage() {
        try {
            if (::noResultsText.isInitialized) {
                noResultsText.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding no results message: ${e.message}")
        }
    }
} 