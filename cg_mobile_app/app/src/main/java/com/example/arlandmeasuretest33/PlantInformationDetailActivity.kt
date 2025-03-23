package com.example.arlandmeasuretest33

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.firestore.FirebaseFirestore

class PlantInformationDetailActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private val TAG = "PlantInfoDetail"

    // UI Elements
    private lateinit var plantImage: ImageView
    private lateinit var plantName: TextView
    private lateinit var plantSubtitle: TextView
    private lateinit var plantDescription: TextView
    private lateinit var fertilizerValue: TextView
    private lateinit var costValue: TextView
    private lateinit var growthPeriodValue: TextView
    private lateinit var yieldValue: TextView
    private lateinit var spaceValue: TextView
    private lateinit var saveButton: Button

    // Plant data
    private var plantId: String = ""
    private var district: String = "Ampara" // Default district
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()

        // Get data from intent
        plantId = intent.getStringExtra("PLANT_ID") ?: ""
        // If plant ID is not passed, try to get plant name
        if (plantId.isEmpty()) {
            plantId = intent.getStringExtra("PLANT_NAME") ?: ""
        }
        
        district = intent.getStringExtra("DISTRICT") ?: "Ampara"

        // Set up save button
        saveButton.setOnClickListener {
            // Placeholder for save functionality
            Toast.makeText(this, "Plant saved to favorites", Toast.LENGTH_SHORT).show()
        }

        if (plantId.isNotEmpty()) {
            // Fetch plant data from Firestore
            fetchPlantData(plantId, district)
        } else {
            // No plant ID/name provided
            Toast.makeText(this, "Error: Plant information not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Add support for system back button
    override fun onBackPressed() {
        super.onBackPressed()
        // Safely apply transition animation if resources exist
        try {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } catch (e: Exception) {
            Log.e(TAG, "Could not apply animation: ${e.message}")
            // Fall back to default transition
        }
    }

    private fun initializeViews() {
        saveButton = findViewById(R.id.saveButton)
        plantImage = findViewById(R.id.plantImage)
        plantName = findViewById(R.id.plantName)
        plantSubtitle = findViewById(R.id.plantSubtitle)
        plantDescription = findViewById(R.id.plantDescription)

        // Get additional fields - may need to add these to your layout
        try {
            fertilizerValue = findViewById(R.id.fertilizerValue)
            costValue = findViewById(R.id.costValue)
            growthPeriodValue = findViewById(R.id.growthPeriodValue)
            yieldValue = findViewById(R.id.yieldValue)
            spaceValue = findViewById(R.id.spaceValue)
        } catch (e: Exception) {
            Log.e(TAG, "Some views might be missing from the layout: ${e.message}")
        }
    }

    private fun fetchPlantData(plantId: String, district: String) {
        Log.d(TAG, "Fetching plant data for '$plantId' in district '$district'")
        
        // Special handling for problematic plants
        if (plantId.equals("Taro", ignoreCase = true) || 
            plantId.equals("Winged Bean", ignoreCase = true) || 
            plantId.equals("Winged_Bean", ignoreCase = true) ||
            plantId.equals("Winged_bean", ignoreCase = true)) {
            
            Log.d(TAG, "Using special fetching for problematic plant: $plantId")
            tryFetchProblemPlant(plantId)
            return
        }
        
        // First try the specified district
        tryFetchFromDistrict(plantId, district) { success ->
            if (!success) {
                // If not found in specified district, try Ampara as a fallback
                if (district != "Ampara") {
                    tryFetchFromDistrict(plantId, "Ampara") { amparaSuccess ->
                        if (!amparaSuccess) {
                            // If still not found, try Colombo as a second fallback
                            tryFetchFromDistrict(plantId, "Colombo") { colomboSuccess ->
                                if (!colomboSuccess) {
                                    // If not found in any district, check for special cases
                                    handleSpecialCasePlants(plantId)
                                }
                            }
                        }
                    }
                } else {
                    // If Ampara was already tried, try Colombo
                    tryFetchFromDistrict(plantId, "Colombo") { colomboSuccess ->
                        if (!colomboSuccess) {
                            // If not found in Colombo either, check for special cases
                            handleSpecialCasePlants(plantId)
                        }
                    }
                }
            }
        }
    }

    private fun tryFetchFromDistrict(plantId: String, district: String, callback: (Boolean) -> Unit) {
        // First try with exact match
        db.collection("districts").document(district).collection("crops").document(plantId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(TAG, "Found exact match for '$plantId' in $district")
                    displayPlantData(document.id, document.data, district)
                    callback(true)
                } else {
                    // If exact match not found, try case-insensitive search
                    Log.d(TAG, "No exact match for '$plantId' in $district, trying case-insensitive search")
                    db.collection("districts").document(district).collection("crops")
                        .get()
                        .addOnSuccessListener { documents ->
                            var found = false
                            for (doc in documents) {
                                if (doc.id.equals(plantId, ignoreCase = true)) {
                                    Log.d(TAG, "Found case-insensitive match for '$plantId' as '${doc.id}' in $district")
                                    displayPlantData(doc.id, doc.data, district)
                                    found = true
                                    break
                                }
                            }
                            callback(found)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error searching crops in $district: ${e.message}")
                            callback(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting document for $plantId in $district: ${e.message}")
                callback(false)
            }
    }

    private fun displayPlantData(plantName: String, data: Map<String, Any>?, district: String) {
        if (data == null) {
            Toast.makeText(this, "No data found for this plant", Toast.LENGTH_SHORT).show()
            return
        }

        // Log all data to check what fields are available
        Log.d(TAG, "Plant data for $plantName: $data")

        // Set plant name and subtitle
        this.plantName.text = plantName
        plantSubtitle.text = "From $district District"

        // Get and set description - handle case sensitivity
        val description = getCaseInsensitiveField(data, "description", "Description")
            ?: "No description available for this plant."
        plantDescription.text = description.toString()

        // Get and load image URL
        val imageUrl = getCaseInsensitiveField(data, "image", "Image") as? String
        if (!imageUrl.isNullOrEmpty()) {
            loadImage(imageUrl)
        } else {
            // Use fallback image
            plantImage.setImageResource(R.drawable.aloe_vera)
        }

        // Try to set other fields if they exist in the layout
        try {
            // Get cost per unit
            val cost = getCaseInsensitiveField(data, "cost_per_unit", "costPerUnit")
            if (cost != null) {
                when (cost) {
                    is Number -> costValue.text = "Rs. ${cost.toFloat()}"
                    is String -> costValue.text = "Rs. $cost"
                    else -> costValue.text = "N/A"
                }
            } else {
                costValue.text = "N/A"
            }

            // Get fertilizer
            val fertilizer = getCaseInsensitiveField(data, "fertilizer", "Fertilizer")
            if (fertilizer != null) {
                when (fertilizer) {
                    is Number -> fertilizerValue.text = "${fertilizer.toInt()}"
                    is String -> fertilizerValue.text = fertilizer.toString()
                    else -> fertilizerValue.text = "N/A"
                }
            } else {
                fertilizerValue.text = "N/A"
            }

            // Get growth cycle duration
            val growthPeriod = getCaseInsensitiveField(data, "growth_cycle_duration", "growthCycleDuration", "growthPeriod")
            if (growthPeriod != null) {
                when (growthPeriod) {
                    is Number -> growthPeriodValue.text = "${growthPeriod.toInt()} days"
                    is String -> growthPeriodValue.text = "$growthPeriod days"
                    else -> growthPeriodValue.text = "N/A"
                }
            } else {
                growthPeriodValue.text = "N/A"
            }

            // Get expected yield per plant
            val yield = getCaseInsensitiveField(data, "expected_yield_per_plant", "expectedYieldPerPlant", "yield")
            if (yield != null) {
                when (yield) {
                    is Number -> yieldValue.text = "${yield.toInt()} kg"
                    is String -> yieldValue.text = "$yield kg"
                    else -> yieldValue.text = "N/A"
                }
            } else {
                yieldValue.text = "N/A"
            }

            // Get space requirement
            val space = getCaseInsensitiveField(data, "space", "Space") as? String
            spaceValue.text = space ?: "N/A"

        } catch (e: Exception) {
            Log.e(TAG, "Error setting additional field values: ${e.message}")
        }
    }

    private fun loadImage(imageUrl: String) {
        try {
            val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.aloe_vera)
                .error(R.drawable.aloe_vera)

            Glide.with(this)
                .load(imageUrl)
                .apply(requestOptions)
                .into(plantImage)

            Log.d(TAG, "Loading image from URL: $imageUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}")
            plantImage.setImageResource(R.drawable.aloe_vera)
        }
    }

    private fun handleSpecialCasePlants(plantId: String) {
        // List of all priority plants to ensure they're properly shown
        val priorityPlants = listOf(
            "bitter melon", "winged bean", "red spinach", 
            "beetroot", "brinjal", "carrots", "cabbage", "leeks", "potato", 
            "onion", "manioc", "taro", "eggplant", "pumpkin", "knolkhol", "drumstick"
        )
        
        // Check if this is a priority plant (case-insensitive)
        val isPriorityPlant = priorityPlants.any { it.equals(plantId, ignoreCase = true) }
        
        if (isPriorityPlant) {
            Log.d(TAG, "Handling priority plant: $plantId")
            
            // Try to use the current plant info with a better fallback
            tryLoadPlantFromAllDistricts(plantId) { success ->
                if (!success) {
                    // Only if all database attempts failed, use hardcoded data as last resort
                    loadHardcodedPlantData(plantId)
                }
            }
        } else {
            // For non-priority plants, use existing special case handling
            loadHardcodedPlantData(plantId)
        }
    }
    
    private fun tryLoadPlantFromAllDistricts(plantId: String, callback: (Boolean) -> Unit) {
        // Extended list of districts to try
        val allDistricts = listOf(
            "Ampara", "Colombo", "Badulla", "Anuradhapura", "Batticaloa", "Galle",
            "Kandy", "Jaffna", "Matara", "Kurunegala"
        )
        
        var districtsChecked = 0
        var plantFound = false
        
        for (district in allDistricts) {
            db.collection("districts").document(district).collection("crops")
                .get()
                .addOnSuccessListener { documents ->
                    if (!plantFound) {  // Only proceed if plant not found yet
                        for (doc in documents) {
                            if (doc.id.equals(plantId, ignoreCase = true)) {
                                Log.d(TAG, "Found priority plant '$plantId' as '${doc.id}' in $district")
                                displayPlantData(doc.id, doc.data, district)
                                plantFound = true
                                callback(true)
                                break
                            }
                        }
                    }
                    
                    districtsChecked++
                    if (districtsChecked == allDistricts.size && !plantFound) {
                        callback(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error searching for priority plant '$plantId' in $district: ${e.message}")
                    
                    districtsChecked++
                    if (districtsChecked == allDistricts.size && !plantFound) {
                        callback(false)
                    }
                }
        }
    }
    
    private fun loadHardcodedPlantData(plantId: String) {
        // Special case handling for known plants
        val specialPlantData = mapOf(
            "brinjal" to mapOf(
                "image" to "https://gourmetgarden.in/cdn/shop/products/Brinjal_Long_Purple_3_875d-466f-8625-3e8da2207e1d.jpg?v=1742037242",
                "description" to "Brinjal, also known as eggplant, is a highly marketable and nutritious vegetable. It grows well up to 1300m above sea level and is widely cultivated for its high yield and adaptability to different climates.",
                "cost_per_unit" to 9.75,
                "fertilizer" to 22,
                "expected_yield_per_plant" to 3500,
                "growth_cycle_duration" to 85,
                "space" to "90 x 60"
            ),
            "onion" to mapOf(
                "image" to "https://plantix.net/en/library/assets/custom/crop-images/onion.jpeg",
                "description" to "Onion is a versatile vegetable used in almost all cuisines. It is rich in vitamins and minerals, and has medicinal properties.",
                "cost_per_unit" to 12.5,
                "fertilizer" to 18,
                "expected_yield_per_plant" to 800,
                "growth_cycle_duration" to 90,
                "space" to "30 x 15"
            ),
            "potato" to mapOf(
                "image" to "https://cdn.mos.cms.futurecdn.net/iC7HBvohbJqExqvbKcV3pP.jpg",
                "description" to "Potato is a starchy root vegetable that is a staple food in many countries. It is rich in carbohydrates and vitamin C.",
                "cost_per_unit" to 15.0,
                "fertilizer" to 25,
                "expected_yield_per_plant" to 1200,
                "growth_cycle_duration" to 100,
                "space" to "60 x 30"
            ),
            "manioc" to mapOf(
                "image" to "https://www.shutterstock.com/image-photo/cassava-root-vegetable-isolated-on-260nw-315185849.jpg",
                "description" to "Manioc, also known as cassava, is a root vegetable. It is a staple food in many tropical regions due to its drought tolerance and high carbohydrate content.",
                "cost_per_unit" to 8.0,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 5000,
                "growth_cycle_duration" to 270,
                "space" to "100 x 100"
            ),
            "bitter melon" to mapOf(
                "image" to "https://www.gardeningknowhow.com/wp-content/uploads/2017/09/bitter-melon.jpg",
                "description" to "Bitter melon is a tropical vine with distinct warty fruits. It has medicinal properties and is used in many Asian cuisines.",
                "cost_per_unit" to 10.0,
                "fertilizer" to 18,
                "expected_yield_per_plant" to 950,
                "growth_cycle_duration" to 70,
                "space" to "150 x 50"
            ),
            "winged bean" to mapOf(
                "image" to "https://specialtyproduce.com/sppics/9879.png",
                "description" to "Winged bean is a tropical legume plant with edible pods, leaves, flowers, and seeds. It is highly nutritious and has a high protein content.",
                "cost_per_unit" to 12.0,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 800,
                "growth_cycle_duration" to 90,
                "space" to "60 x 30"
            ),
            "red spinach" to mapOf(
                "image" to "https://m.media-amazon.com/images/I/71K1zYQXYVL._AC_UF1000,1000_QL80_.jpg",
                "description" to "Red spinach is a leafy vegetable known for its red-purple colored leaves. It is rich in iron, vitamins, and antioxidants.",
                "cost_per_unit" to 7.0,
                "fertilizer" to 20,
                "expected_yield_per_plant" to 500,
                "growth_cycle_duration" to 45,
                "space" to "30 x 15"
            ),
            "beetroot" to mapOf(
                "image" to "https://cdn.shopify.com/s/files/1/0274/3481/articles/BEETROOT-HERO.jpg?v=1518095572",
                "description" to "Beetroot is a root vegetable with a deep red color. It is rich in fiber, vitamins, and minerals, and has been linked to improved athletic performance.",
                "cost_per_unit" to 9.0,
                "fertilizer" to 22,
                "expected_yield_per_plant" to 750,
                "growth_cycle_duration" to 60,
                "space" to "30 x 10"
            ),
            "eggplant" to mapOf(
                "image" to "https://gourmetgarden.in/cdn/shop/products/Brinjal_Long_Purple_3_875d-466f-8625-3e8da2207e1d.jpg?v=1742037242",
                "description" to "Eggplant, also known as brinjal, is a widely cultivated vegetable with a smooth, glossy skin and meaty texture. It is rich in fiber and antioxidants.",
                "cost_per_unit" to 9.75,
                "fertilizer" to 22,
                "expected_yield_per_plant" to 3500,
                "growth_cycle_duration" to 85,
                "space" to "90 x 60"
            ),
            "carrots" to mapOf(
                "image" to "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Vegetable-Carrot-Bundle-wStalks.jpg/1200px-Vegetable-Carrot-Bundle-wStalks.jpg",
                "description" to "Carrot is a root vegetable, usually orange in color. It is rich in beta carotene and other nutrients, making it a popular health food.",
                "cost_per_unit" to 8.5,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 250,
                "growth_cycle_duration" to 70,
                "space" to "20 x 5"
            ),
            "cabbage" to mapOf(
                "image" to "https://upload.wikimedia.org/wikipedia/commons/6/6f/Cabbage_in_a_stack.jpg",
                "description" to "Cabbage is a leafy vegetable that grows in large compact heads. It is rich in vitamin C and fiber, and is used in many cuisines around the world.",
                "cost_per_unit" to 11.0,
                "fertilizer" to 25,
                "expected_yield_per_plant" to 2500,
                "growth_cycle_duration" to 90,
                "space" to "60 x 45"
            ),
            "leeks" to mapOf(
                "image" to "https://thumbs.dreamstime.com/b/leek-vegetable-white-space-32225693.jpg",
                "description" to "Leek is a vegetable in the allium family, related to onions and garlic. It has a milder, sweeter flavor than onions and is used in soups, stews, and salads.",
                "cost_per_unit" to 10.0,
                "fertilizer" to 20,
                "expected_yield_per_plant" to 600,
                "growth_cycle_duration" to 120,
                "space" to "30 x 15"
            ),
            "taro" to mapOf(
                "image" to "https://static.toiimg.com/photo/msid-81246913/81246913.jpg",
                "description" to "Taro is a starchy root vegetable widely cultivated in tropical regions. Its large, elephant ear-like leaves and corm are edible but must be cooked properly.",
                "cost_per_unit" to 12.0,
                "fertilizer" to 18,
                "expected_yield_per_plant" to 3000,
                "growth_cycle_duration" to 200,
                "space" to "90 x 60"
            ),
            "pumpkin" to mapOf(
                "image" to "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/FrenchMarketPumpkinsB.jpg/800px-FrenchMarketPumpkinsB.jpg",
                "description" to "Pumpkin is a winter squash with a sweet, earthy flavor. It is rich in vitamins and minerals, particularly vitamin A and potassium.",
                "cost_per_unit" to 15.0,
                "fertilizer" to 25,
                "expected_yield_per_plant" to 10000,
                "growth_cycle_duration" to 120,
                "space" to "180 x 180"
            ),
            "knolkhol" to mapOf(
                "image" to "https://img.freepik.com/premium-photo/kohlrabi-white-background_253984-5071.jpg",
                "description" to "Knolkhol, also known as kohlrabi, is a vegetable related to cabbage, with a swollen stem that resembles a turnip. It has a crisp texture and a mild, sweet flavor.",
                "cost_per_unit" to 9.0,
                "fertilizer" to 20,
                "expected_yield_per_plant" to 750,
                "growth_cycle_duration" to 60,
                "space" to "30 x 15"
            ),
            "drumstick" to mapOf(
                "image" to "https://static.toiimg.com/thumb/79890873.cms?width=400&height=300&resizemode=4&imgsize=290147",
                "description" to "Drumstick is the fruit of the Moringa tree. It is highly nutritious and used in various cuisines, particularly in South Asian dishes like sambar and curry.",
                "cost_per_unit" to 14.0,
                "fertilizer" to 15,
                "expected_yield_per_plant" to 5000,
                "growth_cycle_duration" to 270,
                "space" to "300 x 300"
            )
        )

        // Find matching plant (case-insensitive)
        val matchedPlant = specialPlantData.entries.firstOrNull { 
            it.key.equals(plantId, ignoreCase = true) 
        }

        if (matchedPlant != null) {
            Log.d(TAG, "Found special case for plant: ${matchedPlant.key}")
            displayPlantData(matchedPlant.key, matchedPlant.value as Map<String, Any>, "Special Database")
        } else {
            Log.d(TAG, "No data found for plant: $plantId")
            Toast.makeText(this, "No information found for this plant", Toast.LENGTH_SHORT).show()
            
            // Display with a generic placeholder but useful name
            this.plantName.text = plantId.split(" ").joinToString(" ") { it.capitalize() }
            plantSubtitle.text = "Plant Information"
            plantDescription.text = "This is a common vegetable plant. Detailed information for this plant is being updated."
            plantImage.setImageResource(R.drawable.aloe_vera)
            
            // Set reasonable defaults for other fields
            try {
                fertilizerValue.text = "N/A"
                costValue.text = "N/A"
                growthPeriodValue.text = "N/A"
                yieldValue.text = "N/A"
                spaceValue.text = "N/A"
            } catch (e: Exception) {
                Log.e(TAG, "Error setting default values: ${e.message}")
            }
        }
    }

    private fun tryFetchProblemPlant(plantId: String) {
        // Normalize plant name to match what's expected in the database 
        val normalizedName = when {
            plantId.equals("Winged Bean", ignoreCase = true) -> "Winged_bean"
            plantId.equals("Winged_bean", ignoreCase = true) -> "Winged_bean"
            plantId.equals("Taro", ignoreCase = true) -> "Taro"
            else -> plantId
        }
        
        Log.d(TAG, "Fetching problem plant with normalized name: $normalizedName")
        
        // Districts where these plants are known to exist
        val targetDistricts = listOf("Kalutara", "Hambantota", "Ampara", "Colombo")
        
        var districtsChecked = 0
        var plantFound = false
        
        for (district in targetDistricts) {
            // Try exact match first
            db.collection("districts").document(district).collection("crops").document(normalizedName)
                .get()
                .addOnSuccessListener { document ->
                    if (!plantFound && document.exists()) {
                        Log.d(TAG, "Found problem plant '$normalizedName' in $district with exact match")
                        displayPlantData(document.id, document.data, district)
                        plantFound = true
                    }
                    
                    // Continue checking if not found with exact match
                    if (!plantFound) {
                        // Try case-insensitive search if exact match didn't work
                        db.collection("districts").document(district).collection("crops")
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!plantFound) {  // Only proceed if plant not found yet
                                    for (doc in documents) {
                                        // Check with underscore or space variants
                                        if (doc.id.equals(normalizedName, ignoreCase = true) || 
                                            doc.id.replace("_", " ").equals(plantId, ignoreCase = true) ||
                                            doc.id.replace(" ", "_").equals(plantId, ignoreCase = true)) {
                                            
                                            Log.d(TAG, "Found problem plant '$plantId' as '${doc.id}' in $district")
                                            displayPlantData(doc.id, doc.data, district)
                                            plantFound = true
                                            break
                                        }
                                    }
                                }
                                
                                districtsChecked++
                                if (districtsChecked == targetDistricts.size && !plantFound) {
                                    // If still not found after checking all districts, use fallback
                                    handleSpecialCasePlants(plantId)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error searching for problem plant in $district: ${e.message}")
                                districtsChecked++
                                if (districtsChecked == targetDistricts.size && !plantFound) {
                                    handleSpecialCasePlants(plantId)
                                }
                            }
                    } else {
                        districtsChecked++
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting document for problem plant in $district: ${e.message}")
                    
                    // Try case-insensitive search if exact lookup failed
                    if (!plantFound) {
                        db.collection("districts").document(district).collection("crops")
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!plantFound) {  // Only proceed if plant not found yet
                                    for (doc in documents) {
                                        // Check with underscore or space variants
                                        if (doc.id.equals(normalizedName, ignoreCase = true) || 
                                            doc.id.replace("_", " ").equals(plantId, ignoreCase = true) ||
                                            doc.id.replace(" ", "_").equals(plantId, ignoreCase = true)) {
                                            
                                            Log.d(TAG, "Found problem plant '$plantId' as '${doc.id}' in $district")
                                            displayPlantData(doc.id, doc.data, district)
                                            plantFound = true
                                            break
                                        }
                                    }
                                }
                                
                                districtsChecked++
                                if (districtsChecked == targetDistricts.size && !plantFound) {
                                    // If still not found after checking all districts, use fallback
                                    handleSpecialCasePlants(plantId)
                                }
                            }
                            .addOnFailureListener { e2 ->
                                Log.e(TAG, "Error with case-insensitive search: ${e2.message}")
                                districtsChecked++
                                if (districtsChecked == targetDistricts.size && !plantFound) {
                                    handleSpecialCasePlants(plantId)
                                }
                            }
                    } else {
                        districtsChecked++
                    }
                }
        }
    }

    // Helper function to handle case-insensitive field lookup
    private fun getCaseInsensitiveField(data: Map<String, Any>, vararg fieldNames: String): Any? {
        // First try exact match
        for (fieldName in fieldNames) {
            if (data.containsKey(fieldName)) {
                return data[fieldName]
            }
        }
        
        // If no exact match, try case-insensitive match
        val lowerCaseMap = data.mapKeys { it.key.lowercase() }
        for (fieldName in fieldNames) {
            val result = lowerCaseMap[fieldName.lowercase()]
            if (result != null) {
                return result
            }
        }
        
        return null
    }
} 