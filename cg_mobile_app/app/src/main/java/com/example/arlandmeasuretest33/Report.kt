package com.example.arlandmeasuretest33

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.*
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.lottie.LottieAnimationView
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


val db = FirebaseFirestore.getInstance()

class Report : AppCompatActivity() {
    private lateinit var lottieView: LottieAnimationView
    private lateinit var pdfImageView: ImageView
    private lateinit var downloadButton: Button
    private lateinit var auth: FirebaseAuth
    private var pdfFilePath: String = ""
    private var plantSpacing: Double = 0.0 // Space needed per plant in sq meters
    private var totalSqm: Double = 0.0 // Total area in sq meters
    private var numberOfPlants: Int = 0 // Calculated number of plants
    private var perimeter: Double = 0.0 // Perimeter from AR
    private var currentUser: FirebaseUser? = null
    private var userPlantName: String = ""
    private var userDistrict: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.report)

        // UI Elements
        lottieView = findViewById(R.id.lottie_view)
        val reportContentLayout = findViewById<View>(R.id.reportContentLayout)
        downloadButton = findViewById(R.id.downloadButton)
        
        // Summary card fields
        val plantTypeText = findViewById<TextView>(R.id.plantTypeText)
        val reportDateText = findViewById<TextView>(R.id.reportDateText)
        val landAreaValue = findViewById<TextView>(R.id.landAreaValue)
        val plantsValue = findViewById<TextView>(R.id.plantsValue)
        val monthlyCostValue = findViewById<TextView>(R.id.monthlyCostValue)
        val monthlyIncomeValue = findViewById<TextView>(R.id.monthlyIncomeValue)
        val expectedYieldValue = findViewById<TextView>(R.id.expectedYieldValue)
        val marketPriceValue = findViewById<TextView>(R.id.marketPriceValue)
        val growthDurationValue = findViewById<TextView>(R.id.growthDurationValue)
        
        // Profit summary fields
        val totalRevenueValue = findViewById<TextView>(R.id.totalRevenueValue)
        val totalCostValue = findViewById<TextView>(R.id.totalCostValue)
        val netProfitValue = findViewById<TextView>(R.id.netProfitValue)
        val footerText = findViewById<TextView>(R.id.footerText)

        // Paths
        val templatePath = copyAssetToInternalStorage("template.docx", this)
        val imagePath = copyAssetToInternalStorage("images.jpeg", this)
        val outputDocxPath = getExternalFilesDir(null)?.absolutePath + "/output.docx"
        pdfFilePath = getExternalFilesDir(null)?.absolutePath + "/output.pdf"

        if (templatePath == null || imagePath == null) {
            Log.e("ERROR", "Missing required files in assets folder.")
            return
        }

        // Show report UI directly without loading animation
        lottieView.visibility = View.GONE
        reportContentLayout.visibility = View.VISIBLE
        
        // Log the source of the report request (AR screen vs View Reports button)
        logReportSource()
        
        // Get measurements from intent or use defaults if coming from View Reports button
        getArMeasurementsAndCalculatePlants()

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        
        // Get garden name from SharedPreferences
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val gardenName = sharedPreferences.getString("GARDEN_NAME", "") ?: ""
        val plantName = sharedPreferences.getString("CURRENT_PLANT_NAME", "") ?: ""
        
        // Set current date in the format YYYY-MM-DD
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val currentDate = dateFormatter.format(java.util.Date())
        reportDateText.text = currentDate
        
        // Set initial plant type
        plantTypeText.text = "$userDistrict / $plantName"
        
        // Set footer text with current date
        footerText.text = "Report generated on $currentDate • Ceilão.Grid Agricultural Analytics"
//        var district = ""

//        // Reference to the specific garden document's plants subcollectionk
//        currentUser?.let {
//            db.collection("user_data")
//                .document(it.uid)
//                .collection("user_gardens")
//                .document(gardenName)
//                .get()
//                .addOnSuccessListener { document ->
//                    district = document.getString("district").toString()
//                    println("District: $district")
//                }
//                .addOnFailureListener { e ->
//                    println("Error: ${e.message}")
//                }
//        }

        if (gardenName.isBlank()) {
            Log.e("MainActivity", "Garden name is blank, cannot save measurements")
            Toast.makeText(this, "Garden name not found, measurements not saved", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity", "Using garden name from SharedPreferences: '$gardenName'")

        // Start by getting the district first
        currentUser?.let { user ->
            val userDocRef = db.collection("user_data")
                .document(user.uid)
                .collection("user_gardens")
                .document(gardenName)

            userDocRef.get()
                .addOnSuccessListener { document ->
                    val district = document.getString("district") ?: ""
                    
                    // Now that we have the district, fetch plant data
                    getUserPlantData(district, gardenName, plantName) { success ->
                        if (success) {
                            // Proceed to fetch crop data and process document
                            fetchCropDataFromFirebase(userDistrict, userPlantName) { replacements ->
                                // Update UI with fetched data
                                runOnUiThread {
                                    // Update plant name display
                                    val plantTypeText = findViewById<TextView>(R.id.plantTypeText)
                                    plantTypeText.text = "$userDistrict / $userPlantName"
                                    
                                    // Update measurements
                                    val landAreaValue = findViewById<TextView>(R.id.landAreaValue)
                                    landAreaValue.text = String.format("%.2f Sq.m", totalSqm)
                                    
                                    val plantsValue = findViewById<TextView>(R.id.plantsValue)
                                    plantsValue.text = numberOfPlants.toString()
                                    
                                    // Update income values
                                    val income = replacements["{{expected_income_per_month}}"] ?: "0.00"
                                    
                                    // Calculate total cost based on plants for consistency
                                    val seedsCost = 50.0 * numberOfPlants
                                    val fertilizerCost = 2.0 * 10
                                    val calculatedCost = seedsCost + fertilizerCost
                                    
                                    // Update the UI with the calculated cost
                                    val monthlyCostValue = findViewById<TextView>(R.id.monthlyCostValue)
                                    monthlyCostValue.text = "LKR ${String.format("%.2f", calculatedCost)}"
                                    
                                    // Always update the replacements map with the calculated cost for PDF generation
                                    replacements["{{estimated_expenses_per_Month}}"] = String.format("%.2f", calculatedCost)
                                    
                                    val monthlyIncomeValue = findViewById<TextView>(R.id.monthlyIncomeValue)
                                    
                                    // Always calculate income based on yield and price for consistency
                                    val yieldPerPlant = parseYieldValue(replacements["{{expected_yield_per_plant}}"] ?: "150") // 150g = 0.15kg
                                    val totalYield = yieldPerPlant * numberOfPlants
                                    val priceString = replacements["{{market_price_per_unit}}"] ?: "150"
                                    val pricePerKg = priceString.toDoubleOrNull()?.toInt()?.toDouble() ?: 150.0
                                    val calculatedIncome = totalYield * pricePerKg
                                    
                                    if (income == "0" || income == "0.00") {
                                        monthlyIncomeValue.text = "LKR ${String.format("%.2f", calculatedIncome)}"
                                        // Also update the replacements map for PDF generation
                                        replacements["{{expected_income_per_month}}"] = String.format("%.2f", calculatedIncome)
                                    } else {
                                        monthlyIncomeValue.text = "LKR $income"
                                    }
                                    
                                    // Update yield and market price
                                    val yieldValue = replacements["{{expected_yield_per_plant}}"] ?: "0"
                                    val marketPrice = replacements["{{market_price_per_unit}}"] ?: "0"
                                    val growthDuration = replacements["{{growth_cycle_duration}}"] ?: "0"
                                    
                                    // Check if yield value contains unit information
                                    val expectedYieldValue = findViewById<TextView>(R.id.expectedYieldValue)
                                    if (yieldValue.contains("kg", ignoreCase = true)) {
                                        // Convert kg to g for display
                                        val numericPart = yieldValue.toLowerCase().replace("kg", "").trim()
                                        val valueInGrams = try {
                                            (numericPart.toDouble() * 1000).toInt().toString()
                                        } catch (e: Exception) {
                                            yieldValue
                                        }
                                        expectedYieldValue.text = "$valueInGrams g per cycle"
                                    } else {
                                        expectedYieldValue.text = "$yieldValue g per cycle"
                                    }
                                    
                                    val marketPriceValue = findViewById<TextView>(R.id.marketPriceValue)
                                    val roundedMarketPrice = if (marketPrice == "0") {
                                        "150" // Default value if price is zero
                                    } else {
                                        marketPrice.toDoubleOrNull()?.toInt()?.toString() ?: marketPrice
                                    }
                                    marketPriceValue.text = "LKR $roundedMarketPrice/Kg"
                                    
                                    val growthDurationValue = findViewById<TextView>(R.id.growthDurationValue)
                                    growthDurationValue.text = "$growthDuration Days"
                                    
                                    // Update profit summary
                                    // Calculate actual total revenue based on yield per plant, number of plants, and market price
                                    val yieldPerPlantForRevenue = parseYieldValue(yieldValue) // This now converts grams to kg
                                    val totalYieldForRevenue = yieldPerPlantForRevenue * numberOfPlants
                                    val cleanMarketPrice = marketPrice.replace("LKR", "").trim()
                                    val pricePerKgForRevenue = cleanMarketPrice.toDoubleOrNull()?.toInt()?.toDouble() ?: 150.0
                                    val calculatedTotalRevenue = totalYieldForRevenue * pricePerKgForRevenue
                                    
                                    // Use the already calculated cost value directly
                                    val totalCostDouble = calculatedCost
                                    
                                    // Calculate net profit
                                    val calculatedNetProfit = calculatedTotalRevenue - totalCostDouble
                                    
                                    val totalRevenueValue = findViewById<TextView>(R.id.totalRevenueValue)
                                    totalRevenueValue.text = "LKR ${String.format("%.2f", calculatedTotalRevenue)}"
                                    
                                    val totalCostValue = findViewById<TextView>(R.id.totalCostValue)
                                    totalCostValue.text = "LKR ${String.format("%.2f", totalCostDouble)}"
                                    
                                    val netProfitValue = findViewById<TextView>(R.id.netProfitValue)
                                    netProfitValue.text = "LKR ${String.format("%.2f", calculatedNetProfit)}"
                                }
                                
                                Thread {
                                    modifyWordDocument(
                                        templatePath,
                                        outputDocxPath,
                                        imagePath,
                                        replacements
                                    )
                                    convertWordToPdf(outputDocxPath, pdfFilePath)
                                }.start()
                            }
                        } else {
                            runOnUiThread {
                                lottieView.visibility = View.GONE
                                Toast.makeText(this, "Error loading plant data", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error getting district: ${e.message}")
                    Toast.makeText(this, "Failed to load garden details", Toast.LENGTH_SHORT).show()
                }
        }

        // Set Download Button Click Event
        downloadButton.setOnClickListener {
            // Show loading indicator or disable button
            downloadButton.isEnabled = false
            downloadButton.text = "Downloading..."
            
            // Download the PDF
            downloadPdfToDownloads(pdfFilePath, "Ceilão Grid Report.pdf")
            
            // Re-enable button after a short delay
            Handler(mainLooper).postDelayed({
                downloadButton.isEnabled = true
                downloadButton.text = "Download Full Report (PDF)"
            }, 2000)
        }
        
        // Set up the Back to Home text view
        val backToHomeText = findViewById<TextView>(R.id.backToHomeText)
        
        // Add underline to the text
        backToHomeText.paintFlags = backToHomeText.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        
        // Set click listener to navigate to home
        backToHomeText.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        
        // Set up the back button at the top
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // Use the existing back press logic
        }
    }

    private fun getUserPlantData(
        district: String,
        userGardenName: String,
        plant: String,
        callback: (Boolean) -> Unit
    ) {
        if (currentUser == null) {
            Log.e("USER_DATA", "No user logged in")
            callback(false)
            return
        }
        println(district+userGardenName+plant)
        // Check if data is passed through Intent
        val intentPlantName = intent.getStringExtra("PLANT_NAME")
        if (!intentPlantName.isNullOrEmpty()) {
            userPlantName = intentPlantName
            userDistrict = intent.getStringExtra("DISTRICT") ?: district
            Log.d("USER_DATA", "Using plant data from intent: $userDistrict/$userPlantName")
            callback(true)
            return
        }

        val userId = currentUser!!.uid
        val plantDocRef = db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(userGardenName)
            .collection("plants")

        // Get all documents in "plants" collection and pick the first one
        plantDocRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val firstPlantDoc = querySnapshot.documents.first()
                    userPlantName = firstPlantDoc.id
                    userDistrict = district
                    Log.d("USER_DATA", "Found user plant: $userDistrict/$userPlantName")
                    callback(true)
                } else {
                    // No plant documents, fallback
                    userPlantName = plant
                    userDistrict = district
                    Log.d("USER_DATA", "No plants found, using fallback: $userDistrict/$userPlantName")
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                Log.e("USER_DATA", "Error getting plants: ${e.message}")
                userPlantName = plant
                userDistrict = district
                callback(true)
            }
    }


    // Fix the onBackPressed method to prevent crashes
    override fun onBackPressed() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Clear Cache")
                .setMessage("Do you want to clear the cache?")
                .setPositiveButton("Yes") { dialog, _ ->
                    try {
                        clearCache()
                    } catch (e: Exception) {
                        Log.e("REPORT", "Error clearing cache: ${e.message}")
                    }
                    dialog.dismiss()
                    // Navigate to previous activity
                    finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    // Navigate to previous activity
                    finish()
                }
                .show()
        } catch (e: Exception) {
            Log.e("REPORT", "Error in onBackPressed: ${e.message}")
            // Fallback to just finishing the activity
            finish()
        }
    }

    // Modify the fetchCropDataFromFirebase function to match your template.docx placeholders
    private fun fetchCropDataFromFirebase(district: String, cropName: String, callback: (MutableMap<String, String>) -> Unit) {
        val replacementsMap = mutableMapOf<String, String>()

        // Set default values in case of error
        val defaultValues = mapOf(
            "description" to "Description not available",
            // Removed estimated_expenses_per_Month as we'll use totalCost directly
            "expected_income_per_month" to "0",
            "expected_yield_per_plant" to "0",
            "market_price_per_unit" to "0",
            "growth_cycle_duration" to "0",
            "spacing" to "0.25",
            "cost_per_unit" to "N/A",
            "Fertilizer" to "N/A"
        )

        // Reference to the crop document in Firestore
        val cropRef = db.collection("districts").document(district).collection("crops").document(cropName)
        println("abc" + cropRef)

        // First get the commodity price data
        val commodityRef = db.collection("commodities").document(cropName)
        Log.d("FETCH_DEBUG", "Fetching commodity data for $cropName")
        Log.d("COMMODITY_PRICE", "Attempting to fetch price data for $cropName from commodities collection")
        
        commodityRef.get()
            .addOnSuccessListener { commodityDoc ->
                var latestPrice = defaultValues["market_price_per_unit"]!!
                Log.d("FETCH_DEBUG", "Default price value: $latestPrice")
                
                if (commodityDoc != null && commodityDoc.exists()) {
                    Log.d("FETCH_DEBUG", "Found commodity document for $cropName")
                    
                    // Special handling for Drumstick plant - sometimes the data format is different
                    if (cropName.equals("Drumstick", ignoreCase = true)) {
                        Log.d("COMMODITY_PRICE", "Special handling for Drumstick plant")
                        
                        // Try getting price directly if it's stored differently
                        val directPrice = commodityDoc.getString("price") ?: commodityDoc.getDouble("price")?.toString()
                        if (!directPrice.isNullOrEmpty()) {
                            latestPrice = directPrice
                            Log.d("COMMODITY_PRICE", "Found direct price for Drumstick: $latestPrice")
                        }
                    }
                    
                    // Get the predictions field which contains date-price pairs
                    val predictions = commodityDoc.get("predictions") as? Map<*, *>
                    
                    if (predictions != null && predictions.isNotEmpty()) {
                        Log.d("FETCH_DEBUG", "Found predictions: ${predictions.keys}")
                        // Find the latest date
                        var latestDate: String? = null
                        
                        for (dateKey in predictions.keys) {
                            val dateStr = dateKey.toString()
                            if (latestDate == null || dateStr > latestDate) {
                                latestDate = dateStr
                            }
                        }
                        
                        // Get the price for the latest date
                        latestDate?.let {
                            val priceObj = predictions[it]
                            // Try multiple ways to get the price value
                            val price = when {
                                priceObj is String -> priceObj
                                priceObj is Number -> priceObj.toString()
                                priceObj is Map<*, *> -> priceObj["price"]?.toString() ?: priceObj.values.firstOrNull()?.toString()
                                else -> priceObj?.toString()
                            }
                            
                            if (!price.isNullOrEmpty()) {
                                try {
                                    // Try to convert to double to ensure it's a valid number
                                    val priceValue = price.toDoubleOrNull()
                                    if (priceValue != null && priceValue > 0) {
                                        latestPrice = price
                                        Log.d("COMMODITY_PRICE", "Found latest price for $cropName: $latestPrice on date $latestDate")
                                    } else {
                                        Log.d("COMMODITY_PRICE", "Invalid price for $cropName: $price, using default")
                                    }
                                } catch (e: Exception) {
                                    Log.e("COMMODITY_PRICE", "Error parsing price for $cropName: $e")
                                }
                            }
                        }
                    }
                } else {
                    Log.d("COMMODITY_PRICE", "No commodity data found for $cropName")
                }
                
                // Ensure Drumstick has a valid price if nothing else worked
                if (cropName.equals("Drumstick", ignoreCase = true) && (latestPrice == "0" || latestPrice.isEmpty())) {
                    latestPrice = "150" // Default price for Drumstick
                    Log.d("COMMODITY_PRICE", "Using default price for Drumstick: $latestPrice")
                }
                
                // Now get the crop data
                cropRef.get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // Format current date
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())

                            // Add date to replacements
                            replacementsMap["{{date}}"] = currentDate

                            // Add plant type to replacements
                            replacementsMap["{{plant_type}}"] = "$district/$cropName"

                            // Get basic crop data from Firestore
                            val description = document.getString("description") ?: defaultValues["description"]!!
                            // Removed separate retrieval of estimated_expenses_per_Month as we'll use totalCost directly
                            val expectedIncome = document.getString("expected_income_per_month") ?: defaultValues["expected_income_per_month"]!!
                            val expectedYieldPerPlant = document.getString("expected_yield_per_plant") ?: defaultValues["expected_yield_per_plant"]!!
                            val growthCycleDuration = document.getString("growth_cycle_duration") ?: defaultValues["growth_cycle_duration"]!!
                            val costPerUnit = document.getString("cost_per_unit") ?: defaultValues["cost_per_unit"]!!
                            val fertilizer = document.getString("Fertilizer") ?: defaultValues["Fertilizer"]!!
                            
                            // Log the values for debugging
                            Log.d("FETCH_DEBUG", "Income: $expectedIncome, Yield: $expectedYieldPerPlant")
                            Log.d("FETCH_DEBUG", "Using market price: $latestPrice")
                            Log.d("FETCH_DEBUG", "Cost per unit: $costPerUnit, Fertilizer: $fertilizer")

                            // Basic crop information
                            replacementsMap["{{description}}"] = description
                            // We'll set estimated_expenses_per_Month later based on calculated total cost
                            replacementsMap["{{expected_income_per_month}}"] = expectedIncome
                            replacementsMap["{{expected_yield_per_plant}}"] = expectedYieldPerPlant
                            replacementsMap["{{market_price_per_unit}}"] = latestPrice // Use the latest price from commodities
                            replacementsMap["{{growth_cycle_duration}}"] = growthCycleDuration
                            replacementsMap["{{cost_per_unit}}"] = costPerUnit
                            replacementsMap["{{fertilizer}}"] = fertilizer

                            // Get plant spacing for calculation
                            // Parse the spacing format (e.g., "25*5" from cm to square meters)
                            val spacingString = document.getString("spacing") ?: defaultValues["spacing"]!!
                            val plantSpacing = if (spacingString.contains("*")) {
                                // Format like "25*5" (in cm)
                                val dimensions = spacingString.split("*")
                                if (dimensions.size == 2) {
                                    try {
                                        // Convert from cm² to m²
                                        val length = dimensions[0].trim().toDouble() / 100
                                        val width = dimensions[1].trim().toDouble() / 100
                                        length * width
                                    } catch (e: Exception) {
                                        defaultValues["spacing"]!!.toDouble()
                                    }
                                } else {
                                    defaultValues["spacing"]!!.toDouble()
                                }
                            } else {
                                // Just a single value
                                try {
                                    spacingString.toDouble()
                                } catch (e: Exception) {
                                    defaultValues["spacing"]!!.toDouble()
                                }
                            }

                            // Update the plant spacing value in the class
                            this.plantSpacing = plantSpacing

                            // Calculate number of plants based on the area
                            if (totalSqm > 0) {
                                numberOfPlants = (totalSqm / plantSpacing).toInt()
                                if (numberOfPlants < 1 && totalSqm > 0) {
                                    numberOfPlants = 1
                                }
                            }

                            Log.d("FIREBASE", "✅ Successfully calculated all values for the report")
                            
                            // Update the replacement map with the calculated values
                            replacementsMap["{{land_area}}"] = String.format("%.2f", totalSqm)
                            replacementsMap["{{number_of_plants}}"] = numberOfPlants.toString()

                            callback(replacementsMap)
                        } else {
                            Log.d("FIREBASE", "❌ Crop document doesn't exist. Setting default values.")
                            setDefaultReplacements(replacementsMap, district, cropName, defaultValues)
                            callback(replacementsMap)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE", "❌ Error getting crop data: ${e.message}")
                        setDefaultReplacements(replacementsMap, district, cropName, defaultValues)
                        callback(replacementsMap)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("COMMODITY_PRICE", "Error getting commodity data: ${e.message}")
                
                // Continue with crop data even if commodity data fails
                cropRef.get()
                    .addOnSuccessListener { document ->
                        // Process crop data as above...
                        if (document != null && document.exists()) {
                            // Format current date and continue with the same code as above...
                            // [Code omitted for brevity - identical to the success case above]
                            
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
                            replacementsMap["{{date}}"] = currentDate
                            replacementsMap["{{plant_type}}"] = "$district/$cropName"
                            
                            // Get basic crop data with default market price
                            val description = document.getString("description") ?: defaultValues["description"]!!
                            // Removed separate retrieval of estimated_expenses_per_Month as we'll use totalCost directly
                            val expectedIncome = document.getString("expected_income_per_month") ?: defaultValues["expected_income_per_month"]!!
                            val expectedYieldPerPlant = document.getString("expected_yield_per_plant") ?: defaultValues["expected_yield_per_plant"]!!
                            val marketPricePerUnit = document.getString("market_price_per_unit") ?: defaultValues["market_price_per_unit"]!!
                            val growthCycleDuration = document.getString("growth_cycle_duration") ?: defaultValues["growth_cycle_duration"]!!
                            val costPerUnit = document.getString("cost_per_unit") ?: defaultValues["cost_per_unit"]!!
                            val fertilizer = document.getString("Fertilizer") ?: defaultValues["Fertilizer"]!!
                            
                            // Basic crop information
                            replacementsMap["{{description}}"] = description
                            // We'll set estimated_expenses_per_Month later based on calculated total cost
                            replacementsMap["{{expected_income_per_month}}"] = expectedIncome
                            replacementsMap["{{expected_yield_per_plant}}"] = expectedYieldPerPlant
                            replacementsMap["{{market_price_per_unit}}"] = marketPricePerUnit
                            replacementsMap["{{growth_cycle_duration}}"] = growthCycleDuration
                            replacementsMap["{{cost_per_unit}}"] = costPerUnit
                            replacementsMap["{{fertilizer}}"] = fertilizer
                            
                            callback(replacementsMap)
                        } else {
                            Log.d("FIREBASE", "❌ Crop document doesn't exist. Setting default values.")
                            setDefaultReplacements(replacementsMap, district, cropName, defaultValues)
                            callback(replacementsMap)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE", "❌ Error getting crop data: ${e.message}")
                        setDefaultReplacements(replacementsMap, district, cropName, defaultValues)
                        callback(replacementsMap)
                    }
            }
    }


    // Update the setDefaultReplacements function with simplified replacement values
    private fun setDefaultReplacements(
        replacementsMap: MutableMap<String, String>,
        district: String,
        cropName: String,
        defaultValues: Map<String, String>
    ) {
        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())

        // Basic info
        replacementsMap["{{date}}"] = currentDate
        replacementsMap["{{plant_type}}"] = "$district/$cropName"
        replacementsMap["{{description}}"] = defaultValues["description"]!!
        replacementsMap["{{expected_income_per_month}}"] = defaultValues["expected_income_per_month"]!!
        replacementsMap["{{expected_yield_per_plant}}"] = defaultValues["expected_yield_per_plant"]!!
        replacementsMap["{{market_price_per_unit}}"] = defaultValues["market_price_per_unit"]!!
        replacementsMap["{{growth_cycle_duration}}"] = defaultValues["growth_cycle_duration"]!!
        replacementsMap["{{cost_per_unit}}"] = defaultValues["cost_per_unit"]!!
        replacementsMap["{{fertilizer}}"] = defaultValues["Fertilizer"]!!

        // Set a minimum area if not available
        if (totalSqm <= 0) {
            totalSqm = 1.0
        }

        // Calculate number of plants based on the default spacing
        val plantSpacing = defaultValues["spacing"]!!.toDouble()
        numberOfPlants = (totalSqm / plantSpacing).toInt()
        if (numberOfPlants < 1) {
            numberOfPlants = 1
        }

        replacementsMap["{{sqm}}"] = String.format("%.2f", totalSqm)
        replacementsMap["{{number_of_plants}}"] = numberOfPlants.toString()

        // Default values for calculations
        val seedCostPerUnit = 50.0 // Default cost per plant in LKR
        val fertilizerCostPerUnit = 2.0 // Default cost per kg in LKR
        val yieldPerPlant = 0.15 // Default yield in kg per plant
        val pricePerKg = 150.0 // Default price per kg in LKR

        // Seeds/Plants costs
        val seedsQuantity = numberOfPlants
        val seedsTotalCost = seedCostPerUnit * seedsQuantity

        // Fertilizers costs
        val fertilizerQuantity = 10
        val fertilizerTotalCost = fertilizerCostPerUnit * fertilizerQuantity

        // Total costs
        val totalCost = seedsTotalCost + fertilizerTotalCost

        // Total yield
        val totalYield = yieldPerPlant * numberOfPlants

        // Total revenue
        val totalRevenue = totalYield * pricePerKg

        // Net profit
        val netProfit = totalRevenue - totalCost
        
        // Always set estimated_expenses_per_Month to totalCost
        replacementsMap["{{estimated_expenses_per_Month}}"] = String.format("%.2f", totalCost)
        replacementsMap["{{expected_income_per_month}}"] = String.format("%.2f", netProfit)
        
        // Log the calculated default values
        Log.d("DEFAULT_VALUES", "Calculated cost: ${String.format("%.2f", totalCost)}, net profit: ${String.format("%.2f", netProfit)}")


        // Fill all the empty placeholders with calculated values
        // Cost table - Seeds/Plants row
        replacementsMap["{{}}"] = String.format("%.2f", seedCostPerUnit) // Cost per unit
        replacementsMap["{{}}"] = seedsQuantity.toString() // Quantity
        replacementsMap["{{}}"] = String.format("%.2f", seedsTotalCost) // Total Cost

        // Cost table - Fertilizers row
        replacementsMap["{{}}"] = String.format("%.2f", fertilizerCostPerUnit) // Cost per unit
        replacementsMap["{{}}"] = fertilizerQuantity.toString() // Quantity
        replacementsMap["{{}}"] = String.format("%.2f", fertilizerTotalCost) // Total Cost

        // Cost table - Total Cost
        replacementsMap["{{}}"] = String.format("%.2f", totalCost) // Total Cost at bottom of table

        // Revenue table
        replacementsMap["{{}}"] = String.format("%.2f", totalYield) // Quantity
        replacementsMap["{{}}"] = String.format("%.2f", pricePerKg) // Price per Unit
        replacementsMap["{{}}"] = String.format("%.2f", totalRevenue) // Total Revenue

        // Profit calculation
        replacementsMap["{{}}"] = String.format("%.2f", totalRevenue) // Total Revenue
        replacementsMap["{{}}"] = String.format("%.2f", totalCost) // Total Cost
        replacementsMap["{{}}"] = String.format("%.2f", netProfit) // Net Profit
    }

    // Function to get AR measurements from intent and calculate plants
    private fun getArMeasurementsAndCalculatePlants() {
        try {
            // Check if we have AR_AREA and AR_PERIMETER extras - these will be present when coming from MainActivity (AR)
            // but not when coming from HomeActivity "View Reports" button
            if (intent.hasExtra("AR_AREA") && intent.hasExtra("AR_PERIMETER")) {
                // Get area and perimeter from intent extras (from AR activity)
                totalSqm = intent.getFloatExtra("AR_AREA", 0f).toDouble()
                perimeter = intent.getFloatExtra("AR_PERIMETER", 0f).toDouble()
                
                Log.d("REPORT", "Area from AR: $totalSqm sq m, Perimeter: $perimeter m")
            } else {
                Log.d("REPORT", "No AR measurement data, using default garden size")
                // This is the case when opened from HomeActivity "View Reports" button
                totalSqm = 1.0 // Default to 1 square meter when not from AR
                perimeter = 4.0 // Default perimeter for a 1x1m square
            }

            // If still no valid measurements, use defaults
            if (totalSqm <= 0) {
                Log.d("REPORT", "Invalid area, using default value")
                totalSqm = 0.41 // Default value from screenshot (0.41 m²)
                perimeter = 2.65 // Default value from screenshot (2.65 m)
            }

            // Note: We'll calculate the number of plants after fetching the spacing from Firebase
            // This happens in fetchCropDataFromFirebase now
        } catch (e: Exception) {
            Log.e("ERROR", "Failed to get AR measurements: ${e.message}")
            totalSqm = 0.41 // Default value
            perimeter = 2.65 // Default value
        }
    }

    // Format area for display in the document
    private fun formatAreaRepresentation(): String {
        return if (totalSqm > 0) {
            // Format with 2 decimal places if using actual measurement
            String.format("%.2f m²", totalSqm)
        } else {
            "250x569" // Fallback to original format
        }
    }

    // Updated function to get plant spacing from Firebase
    private fun getPlantSpacingFromFirebase(district: String, cropName: String, callback: (Double) -> Unit) {
        db.collection("districts").document(district).collection("crops").document(cropName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val spacing = document.getDouble("spacing") ?: 0.25
                    callback(spacing)
                    Log.d("FIREBASE", "Retrieved plant spacing for $cropName: $spacing sq meters per plant")
                } else {
                    Log.d("FIREBASE", "No spacing data for $cropName, using default")
                    callback(0.25) // Default spacing
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FIREBASE", "Error getting spacing data: ${exception.message}")
                callback(0.25) // Default spacing on error
            }
    }

    // Function to copy assets to internal storage
    private fun copyAssetToInternalStorage(assetFileName: String, context: Context): String? {
        val file = File(context.filesDir, assetFileName)
        return try {
            context.assets.open(assetFileName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Log.d("FILE_COPY", "✅ Copied $assetFileName to internal storage: ${file.absolutePath}")
            file.absolutePath
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to copy asset file: ${e.message}")
            null
        }
    }

    // Improved function to modify Word document with special handling for tables
    private fun modifyWordDocument(
        templatePath: String,
        outputPath: String,
        imagePath: String,
        replacementsMap: Map<String, String>
    ) {
        try {
            val doc = XWPFDocument(FileInputStream(templatePath))

            // Replace named placeholders in paragraphs
            for (paragraph in doc.paragraphs) {
                val text = paragraph.text
                var modifiedText = text

                for ((key, value) in replacementsMap) {
                    // Only replace named placeholders (those with text inside curly braces)
                    if (key.contains("{") && key.contains("}") && key.length > 4) {
                        if (modifiedText.contains(key)) {
                            modifiedText = modifiedText.replace(key, value)
                        }
                    }
                }

                if (text != modifiedText) {
                    for (run in paragraph.runs) {
                        run.setText("", 0) // Clear existing text
                    }
                    paragraph.createRun().setText(modifiedText) // Set new text
                }
            }

            // Handle tables separately
            if (doc.tables.size >= 2) {
                // Cost table is the first table (index 0)
                val costTable = doc.tables[0]

                // Seeds/Plants row - Row 1 (index 1)
                if (costTable.rows.size > 1) {
                    val seedsRow = costTable.rows[1]

                    // Get cost per unit from replacements or default
                    val costPerUnitStr = replacementsMap["{{cost_per_unit}}"] ?: "N/A"
                    val costPerUnit = if (costPerUnitStr != "N/A") {
                        try {
                            costPerUnitStr.toDouble()
                        } catch (e: NumberFormatException) {
                            0.0
                        }
                    } else {
                        0.0
                    }

                    // Cost per unit - Cell 1 (index 1)
                    if (seedsRow.tableCells.size > 1) {
                        if (costPerUnitStr == "N/A") {
                            setCellText(seedsRow.getCell(1), "N/A")
                        } else {
                            setCellText(seedsRow.getCell(1), String.format("%.2f", costPerUnit) + " LKR")
                        }
                    }

                    // Quantity - Cell 2 (index 2)
                    if (seedsRow.tableCells.size > 2) {
                        setCellText(seedsRow.getCell(2), numberOfPlants.toString())
                    }

                    // Total Cost - Cell 3 (index 3)
                    if (seedsRow.tableCells.size > 3) {
                        if (costPerUnitStr == "N/A") {
                            setCellText(seedsRow.getCell(3), "N/A")
                        } else {
                            val seedsTotalCost = costPerUnit * numberOfPlants
                            setCellText(seedsRow.getCell(3), String.format("%.2f", seedsTotalCost) + " LKR")
                        }
                    }
                }

                // Fertilizers row - Row 2 (index 2)
                if (costTable.rows.size > 2) {
                    val fertRow = costTable.rows[2]

                    // Get fertilizer info from replacements
                    val fertilizerStr = replacementsMap["{{fertilizer}}"] ?: "N/A"
                    
                    // Parse fertilizer cost as a numeric value
                    val fertilizerCost = try {
                        if (fertilizerStr != "N/A") {
                            // Try to extract numeric value from the string
                            val numericValue = fertilizerStr.replace(Regex("[^0-9.]"), "")
                            if (numericValue.isNotEmpty()) numericValue.toDouble() else 2.0
                        } else {
                            2.0 // Default fertilizer cost per plant
                        }
                    } catch (e: Exception) {
                        Log.e("REPORT", "Error parsing fertilizer cost: $fertilizerStr, using default", e)
                        2.0 // Default fertilizer cost per plant
                    }
                    
                    // Cost per unit - Cell 1 (index 1)
                    if (fertRow.tableCells.size > 1) {
                        setCellText(fertRow.getCell(1), String.format("%.2f", fertilizerCost) + " LKR/plant")
                    }

                    // Quantity - Cell 2 (index 2)
                    if (fertRow.tableCells.size > 2) {
                        setCellText(fertRow.getCell(2), numberOfPlants.toString())
                    }

                    // Total Cost - Cell 3 (index 3)
                    if (fertRow.tableCells.size > 3) {
                        val totalFertilizerCost = fertilizerCost * numberOfPlants
                        setCellText(fertRow.getCell(3), String.format("%.2f", totalFertilizerCost) + " LKR")
                    }
                }

                // Total Cost row - Row 3 (index 3)
                if (costTable.rows.size > 3) {
                    val totalRow = costTable.rows[3]

                    // Total Cost - Cell 3 (index 3)
                    if (totalRow.tableCells.size > 3) {
                        // Calculate seeds/plants cost
                        val costPerUnitStr = replacementsMap["{{cost_per_unit}}"] ?: "N/A"
                        val costPerUnit = if (costPerUnitStr != "N/A") {
                            try {
                                costPerUnitStr.toDouble()
                            } catch (e: NumberFormatException) {
                                0.0
                            }
                        } else {
                            0.0
                        }
                        
                        val seedsTotalCost = if (costPerUnitStr != "N/A") {
                            costPerUnit * numberOfPlants
                        } else {
                            0.0
                        }
                        
                        // Calculate fertilizer cost
                        val fertilizerStr = replacementsMap["{{fertilizer}}"] ?: "N/A"
                        val fertilizerCost = try {
                            if (fertilizerStr != "N/A") {
                                // Try to extract numeric value from the string
                                val numericValue = fertilizerStr.replace(Regex("[^0-9.]"), "")
                                if (numericValue.isNotEmpty()) numericValue.toDouble() else 2.0
                            } else {
                                2.0 // Default fertilizer cost per plant
                            }
                        } catch (e: Exception) {
                            Log.e("REPORT", "Error parsing fertilizer cost for total: $fertilizerStr, using default", e)
                            2.0 // Default fertilizer cost per plant
                        }
                        
                        val totalFertilizerCost = fertilizerCost * numberOfPlants
                        
                        // Sum up total costs
                        val totalCost = seedsTotalCost + totalFertilizerCost
                        setCellText(totalRow.getCell(3), String.format("%.2f", totalCost) + " LKR")
                    }
                }

                // Revenue table is the second table (index 1)
                val revenueTable = doc.tables[1]

                // Expected Yield row - Row 1 (index 1)
                if (revenueTable.rows.size > 1) {
                    val yieldRow = revenueTable.rows[1]

                    // Get expected yield per plant value
                    val yieldPerPlantStr = replacementsMap["{{expected_yield_per_plant}}"] ?: "0.15"
                    // Ensure yield value is not 0
                    val effectiveYieldStr = if (yieldPerPlantStr == "0") "150" else yieldPerPlantStr
                    val yieldPerPlant = parseYieldValue(effectiveYieldStr)

                    // Quantity - Cell 1 (index 1)
                    if (yieldRow.tableCells.size > 1) {
                        val totalYield = yieldPerPlant * numberOfPlants
                        // Convert back to grams for display
                        val totalYieldInGrams = totalYield * 1000
                        setCellText(yieldRow.getCell(1), String.format("%.0f", totalYieldInGrams) + " g")
                    }

                    // Price per Unit - Cell 2 (index 2)
                    if (yieldRow.tableCells.size > 2) {
                        // Get market price per unit value
                        val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                        // Use default price if zero
                        val effectivePriceStr = if (pricePerUnitStr == "0" || pricePerUnitStr.trim().isEmpty()) "150" else pricePerUnitStr
                        
                        // More robust parsing with fallback
                        val pricePerUnit = try {
                            val parsed = effectivePriceStr.toDoubleOrNull() ?: 150.0
                            if (parsed <= 0) 150 else parsed.toInt()
                        } catch (e: Exception) {
                            Log.e("REPORT", "Error parsing price: $effectivePriceStr, using default", e)
                            150
                        }
                        
                        setCellText(yieldRow.getCell(2), String.format("%d", pricePerUnit) + " LKR/Kg")
                        
                        // Log for debugging
                        Log.d("REPORT", "Using price per unit: $pricePerUnit from raw value: $pricePerUnitStr")
                    }

                    // Total Revenue - Cell 3 (index 3)
                    if (yieldRow.tableCells.size > 3) {
                        val totalYield = yieldPerPlant * numberOfPlants
                        val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                        // Use default price if zero
                        val effectivePriceStr = if (pricePerUnitStr == "0" || pricePerUnitStr.trim().isEmpty()) "150" else pricePerUnitStr
                        
                        // More robust parsing with fallback
                        val pricePerUnit = try {
                            val parsed = effectivePriceStr.toDoubleOrNull() ?: 150.0
                            if (parsed <= 0) 150.0 else parsed
                        } catch (e: Exception) {
                            Log.e("REPORT", "Error parsing price for revenue: $effectivePriceStr, using default", e)
                            150.0
                        }
                        
                        val totalRevenue = totalYield * pricePerUnit
                        setCellText(yieldRow.getCell(3), String.format("%.2f", totalRevenue) + " LKR")
                        
                        // Log for debugging
                        Log.d("REPORT", "Total Revenue: $totalRevenue (Yield: $totalYield × Price: $pricePerUnit)")
                    }
                }
            }

            // Find and process profit calculation paragraphs
            for (i in doc.paragraphs.indices) {
                val paragraph = doc.paragraphs[i]
                val text = paragraph.text.trim()

                // Total Revenue paragraph
                if (text.startsWith("Total Revenue:")) {
                    // Calculate total revenue
                    val yieldPerPlantStr = replacementsMap["{{expected_yield_per_plant}}"] ?: "0.15"
                    // Use default yield if zero
                    val effectiveYieldStr = if (yieldPerPlantStr == "0") "150" else yieldPerPlantStr
                    val yieldPerPlant = parseYieldValue(effectiveYieldStr)
                    val totalYield = yieldPerPlant * numberOfPlants

                    val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                    // Use default price if zero or invalid
                    val effectivePriceStr = if (pricePerUnitStr == "0" || pricePerUnitStr.trim().isEmpty()) "150" else pricePerUnitStr
                    
                    // More robust price parsing with fallback
                    val pricePerUnit = try {
                        val parsed = effectivePriceStr.toDoubleOrNull() ?: 150.0
                        if (parsed <= 0) 150.0 else parsed
                    } catch (e: Exception) {
                        Log.e("REPORT", "Error parsing price for total revenue paragraph: $effectivePriceStr, using default", e)
                        150.0
                    }
                    
                    val totalRevenue = totalYield * pricePerUnit
                    
                    // Log for debugging
                    Log.d("REPORT", "Profit Paragraph - Total Revenue: $totalRevenue (Yield: $totalYield × Price: $pricePerUnit)")

                    // Replace the paragraph text
                    for (run in paragraph.runs) {
                        run.setText("", 0)
                    }
                    paragraph.createRun().setText("Total Revenue: " + String.format("%.2f", totalRevenue) + " LKR")
                }

                // Total Cost paragraph
                if (text.startsWith("Total Cost:")) {
                    // Calculate seeds/plants cost
                    val costPerUnitStr = replacementsMap["{{cost_per_unit}}"] ?: "N/A"
                    val costPerUnit = if (costPerUnitStr != "N/A") {
                        try {
                            costPerUnitStr.toDouble()
                        } catch (e: NumberFormatException) {
                            50.0 // Default cost per plant
                        }
                    } else {
                        50.0 // Default cost per plant
                    }
                    
                    val seedsTotalCost = costPerUnit * numberOfPlants
                    
                    // Calculate fertilizer cost
                    val fertilizerStr = replacementsMap["{{fertilizer}}"] ?: "N/A"
                    val fertilizerCost = try {
                        if (fertilizerStr != "N/A") {
                            // Try to extract numeric value from the string
                            val numericValue = fertilizerStr.replace(Regex("[^0-9.]"), "")
                            if (numericValue.isNotEmpty()) numericValue.toDouble() else 2.0
                        } else {
                            2.0 // Default fertilizer cost per plant
                        }
                    } catch (e: Exception) {
                        Log.e("REPORT", "Error parsing fertilizer cost for profit calc: $fertilizerStr, using default", e)
                        2.0 // Default fertilizer cost per plant
                    }
                    
                    val totalFertilizerCost = fertilizerCost * numberOfPlants
                    
                    // Sum up total costs
                    val totalCost = seedsTotalCost + totalFertilizerCost

                    // Replace the paragraph text
                    for (run in paragraph.runs) {
                        run.setText("", 0)
                    }
                    paragraph.createRun().setText("Total Cost: " + String.format("%.2f", totalCost) + " LKR")
                }

                // Net Profit paragraph
                if (text.startsWith("Net Profit:")) {
                    // Calculate net profit
                    val yieldPerPlantStr = replacementsMap["{{expected_yield_per_plant}}"] ?: "0.15"
                    // Use default yield if zero
                    val effectiveYieldStr = if (yieldPerPlantStr == "0") "150" else yieldPerPlantStr
                    val yieldPerPlant = parseYieldValue(effectiveYieldStr)
                    val totalYield = yieldPerPlant * numberOfPlants

                    val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                    // Use default price if zero or invalid
                    val effectivePriceStr = if (pricePerUnitStr == "0" || pricePerUnitStr.trim().isEmpty()) "150" else pricePerUnitStr
                    
                    // More robust price parsing with fallback
                    val pricePerUnit = try {
                        val parsed = effectivePriceStr.toDoubleOrNull() ?: 150.0
                        if (parsed <= 0) 150.0 else parsed
                    } catch (e: Exception) {
                        Log.e("REPORT", "Error parsing price for net profit: $effectivePriceStr, using default", e)
                        150.0
                    }
                    
                    val totalRevenue = totalYield * pricePerUnit
                    
                    // Log for debugging
                    Log.d("REPORT", "Net Profit Calculation - Revenue: $totalRevenue (Yield: $totalYield × Price: $pricePerUnit)")

                    // Calculate seeds/plants cost
                    val costPerUnitStr = replacementsMap["{{cost_per_unit}}"] ?: "N/A"
                    val costPerUnit = if (costPerUnitStr != "N/A") {
                        try {
                            costPerUnitStr.toDouble()
                        } catch (e: NumberFormatException) {
                            50.0 // Default cost per plant
                        }
                    } else {
                        50.0 // Default cost per plant
                    }
                    
                    val seedsTotalCost = costPerUnit * numberOfPlants
                    
                    // Calculate fertilizer cost
                    val fertilizerStr = replacementsMap["{{fertilizer}}"] ?: "N/A"
                    val fertilizerCost = try {
                        if (fertilizerStr != "N/A") {
                            // Try to extract numeric value from the string
                            val numericValue = fertilizerStr.replace(Regex("[^0-9.]"), "")
                            if (numericValue.isNotEmpty()) numericValue.toDouble() else 2.0
                        } else {
                            2.0 // Default fertilizer cost per plant
                        }
                    } catch (e: Exception) {
                        Log.e("REPORT", "Error parsing fertilizer cost for net profit: $fertilizerStr, using default", e)
                        2.0 // Default fertilizer cost per plant
                    }
                    
                    val totalFertilizerCost = fertilizerCost * numberOfPlants
                    
                    // Sum up total costs
                    val totalCost = seedsTotalCost + totalFertilizerCost

                    val netProfit = totalRevenue - totalCost

                    // Replace the paragraph text
                    for (run in paragraph.runs) {
                        run.setText("", 0)
                    }
                    paragraph.createRun().setText("Net Profit: [Revenue - Cost] = " + String.format("%.2f", netProfit) + " LKR")
                }
            }

            // Insert Image
            for (paragraph in doc.paragraphs) {
                if (paragraph.text.contains("{{plant_image}}")) {
                    paragraph.runs.forEach { run -> run.setText("", 0) } // Remove the placeholder

                    val run = paragraph.createRun()
                    val imgInputStream = FileInputStream(imagePath)
                    run.addPicture(
                        imgInputStream,
                        XWPFDocument.PICTURE_TYPE_JPEG,
                        imagePath,
                        600_000,
                        600_000
                    )
                    imgInputStream.close()
                }
            }

            // Save the modified document
            val outStream = FileOutputStream(outputPath)
            doc.write(outStream)
            outStream.close()
            doc.close()

            Log.d("SUCCESS", "✅ Word Document Modified Successfully: $outputPath")
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to modify Word document: ${e.message}")
            e.printStackTrace()
        }
    }

    // Helper function to set text in a table cell, clearing existing content first
    private fun setCellText(cell: XWPFTableCell, text: String) {
        // Clear existing paragraphs
        if (cell.paragraphs.size > 0) {
            val paragraph = cell.paragraphs[0]
            paragraph.runs.forEach { run -> run.setText("", 0) }
            paragraph.createRun().setText(text)
        } else {
            // Create new paragraph if needed
            cell.addParagraph().createRun().setText(text)
        }
    }

    // List of API keys
    val apiKeys = listOf(
        "APY03qWaC91oOu2oyrvtwpfQOWujBs6T4tuBddE2l9VsTvak52Guf31JuxGMQ1HArpRH1XHp4K",
        "APY0teeUq8CAtaq9CVw6P98KFjL8i392G0AUt9XI8RjLkpscbE3Neerdd3N1emmCj1rI",
        "APY0HErz5rADpPGWknWyEFBjT3aB7QAcUF5mhYyCiA95fJY3d97CojeNaW5gpuPjGiiV3e",
        "APY0AA3CbDcwlUs51HvIvRU1sG7xDYiUVS3FJvYyZeppxNLr2SvBZHvHc2agNaXbJN7xCqFtzR",
        "APY0UMEnBOTLPaABk5UHhmXkq1o40E3H3daKK8L0PG3XH50PZmSr8PDMDHVbPOGC77Kno",
        "APY0CIqWAo29H5qlPnXMgJHuziTDBNh4c0vVbxkdJ4A4pxbX8UQkJIjzXGc4dJuaWc3IritDQC",
        "APY04NmgEqQehQXgnh2xO1VyywB4SZdkTD7S0yQPbEiyJsp4lB3TYcQFXbcRPvSgIa8WUuqu6lq82R",
        "APY0JiNrSBEGDQMES90z7UKAAOS3ckq2liRpnoO9jMvnuDfC7bOEdvpNMJjyvwC8oGK4C",
        "APY0HvIjCvomf9ZmDpCTr3CGBUgaIeQ5TpNSjRJXf5qVvGCvtd1MnwDnNLdSitVcx4QQh8jpfeY2U",
        "APY0ttOrvL5Bf3I6PKVx6I3c0aQcdpMyJsQLGhROeb1DEgA5Dm1FxizCtjPpgt5JQjqMa"
    )

    // Counter to keep track of the current API key index
    var currentApiKeyIndex = 0

    // Function to convert Word to PDF
    private fun convertWordToPdf(inputDocx: String, outputPdf: String) {
        val client = OkHttpClient()
        val file = File(inputDocx)

        if (!file.exists()) {
            Log.e("ERROR", "DOCX file not found at $inputDocx")
            runOnUiThread {
                lottieView.visibility = View.GONE
                Toast.makeText(this, "Error: Word document not found", Toast.LENGTH_LONG).show()
            }
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody())
            .build()

        val request = Request.Builder()
            .url("https://api.apyhub.com/convert/word-file/pdf-file?output=test-sample.pdf&landscape=false")
            .post(requestBody)
            .header("apy-token", apiKeys[currentApiKeyIndex])  // Use the current API key
            .header("content-type", "multipart/form-data")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }

                // Save the converted PDF
                val pdfFile = File(outputPdf)
                pdfFile.writeBytes(response.body!!.bytes())

                Log.d("SUCCESS", "✅ PDF Created Successfully: ${pdfFile.absolutePath}")

                // Verify file exists and has content before updating UI
                if (pdfFile.exists() && pdfFile.length() > 0) {
                    // PDF is created successfully, just show toast
                    runOnUiThread {
                        downloadButton.visibility = View.VISIBLE
                        Toast.makeText(this, "Report generated successfully!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("ERROR", "PDF file created but appears to be empty or missing")
                    runOnUiThread {
                        Toast.makeText(this, "Error creating PDF. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to convert Word to PDF: ${e.message}")

            // If the current API key failed, try the next one
            currentApiKeyIndex = (currentApiKeyIndex + 1) % apiKeys.size
            
            // If we've already tried all keys but still failing, show error
            if (currentApiKeyIndex == 0) {
                runOnUiThread {
                    Toast.makeText(this, "PDF generation failed, showing report summary only.", Toast.LENGTH_LONG).show()
                }
                return
            }
            
            // Recursively call the function to try the next API key
            convertWordToPdf(inputDocx, outputPdf)
        }
    }

    // This function is kept for future reference if needed, but is not used in the current UI
    private fun previewPdfInView(pdfPath: String) {
        val file = File(pdfPath)
        if (!file.exists()) {
            Log.e("ERROR", "PDF file does not exist: $pdfPath")
            return
        }

        try {
            // Verify the PDF is valid by attempting to open it
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount
            
            // Log successful PDF validation
            Log.d("PDF_VALIDATION", "PDF is valid with $pageCount pages")
            
            // Close resources
            pdfRenderer.close()
            fileDescriptor.close()
            
            // Update UI to show PDF is ready for download
            runOnUiThread {
                downloadButton.isEnabled = true
                downloadButton.text = "Download Full Report (PDF)"
            }
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to validate PDF: ${e.message}")
            runOnUiThread {
                downloadButton.isEnabled = false
                Toast.makeText(this, "PDF validation failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Function to clear cache
    private fun clearCache() {
        try {
            // Delete the temporary Word document
            val outputDocxPath = getExternalFilesDir(null)?.absolutePath + "/output.docx"
            val docxFile = File(outputDocxPath)
            if (docxFile.exists()) {
                try {
                    docxFile.delete()
                    Log.d("CACHE", "Deleted temporary Word document: $outputDocxPath")
                } catch (e: Exception) {
                    Log.e("CACHE", "Failed to delete Word document: ${e.message}")
                }
            }

            // Delete the temporary PDF file
            val pdfFilePath = getExternalFilesDir(null)?.absolutePath + "/output.pdf"
            val pdfFile = File(pdfFilePath)
            if (pdfFile.exists()) {
                try {
                    pdfFile.delete()
                    Log.d("CACHE", "Deleted temporary PDF file: $pdfFilePath")
                } catch (e: Exception) {
                    Log.e("CACHE", "Failed to delete PDF file: ${e.message}")
                }
            }

            // Optionally, delete the entire cache directory
            val cacheDir = getExternalFilesDir(null)
            if (cacheDir != null && cacheDir.exists()) {
                try {
                    cacheDir.listFiles()?.forEach { file ->
                        try {
                            if (file.name.endsWith(".docx") || file.name.endsWith(".pdf") || file.name.endsWith(".temp")) {
                                file.delete()
                                Log.d("CACHE", "Deleted cache file: ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.e("CACHE", "Failed to delete cache file ${file.name}: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CACHE", "Failed to process cache directory: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("CACHE", "Error in clearCache: ${e.message}")
        }
    }

    // Function to save the PDF to the Downloads folder
    private fun downloadPdfToDownloads(pdfPath: String, fileName: String) {
        try {
            val sourceFile = File(pdfPath)

            if (!sourceFile.exists()) {
                runOnUiThread {
                    Toast.makeText(this, "PDF file not ready yet. Please wait.", Toast.LENGTH_LONG).show()
                }
                return
            }

            // Try modern approach first (for Android 10+)
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    Log.d("DOWNLOAD", "✅ PDF successfully saved using ContentResolver: $it")
                    
                    runOnUiThread {
                        Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
                    }
                    
                    // Clear cache after successful download
                    clearCache()
                    return
                }
            } catch (e: Exception) {
                Log.e("DOWNLOAD", "ContentResolver approach failed: ${e.message}")
                // Fall back to the traditional method below
            }

            // Fall back to traditional approach
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDir, fileName)

            if (!downloadsDir.exists()) {
                val dirCreated = downloadsDir.mkdirs()  // Create Downloads folder if not exists
                if (!dirCreated) {
                    Log.e("DOWNLOAD", "Failed to create Downloads directory")
                }
            }

            // Copy file from internal storage to Downloads folder
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Notify Media Scanner to detect the file
            val uri = Uri.fromFile(destinationFile)
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))

            Log.d("DOWNLOAD", "✅ PDF successfully saved to: ${destinationFile.absolutePath}")

            // Show success message
            runOnUiThread {
                Toast.makeText(this, "PDF saved to Downloads folder", Toast.LENGTH_LONG).show()
            }

            // Clear cache after download
            clearCache()

        } catch (e: Exception) {
            Log.e("ERROR", "Failed to copy PDF to Downloads: ${e.message}")
            e.printStackTrace()

            runOnUiThread {
                Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Add this helper function to properly parse yield values
    private fun parseYieldValue(yieldString: String): Double {
        return try {
            when {
                // Handle case when the value is just a number (integer or double)
                yieldString.toDoubleOrNull() != null -> {
                    // Assuming the value is in grams, convert to kg
                    yieldString.toDouble() / 1000.0
                }
                
                // Handle "X kg" or "X Kg" format (keep as is)
                yieldString.toLowerCase().contains("kg") -> {
                    val numericPart = yieldString.toLowerCase().replace("kg", "")
                        .replace("per cycle", "").replace("per plant", "").trim()
                    numericPart.toDoubleOrNull() ?: 0.15
                }
                
                // Handle "X g" or "X g" format (convert to kg)
                yieldString.toLowerCase().contains("g") && !yieldString.toLowerCase().contains("kg") -> {
                    val numericPart = yieldString.toLowerCase().replace("g", "")
                        .replace("per cycle", "").replace("per plant", "").trim()
                    val grams = numericPart.toDoubleOrNull() ?: 150.0
                    grams / 1000.0 // Convert grams to kg
                }
                
                // Default fallback value if no pattern matches (assume 150g = 0.15kg)
                else -> 0.15
            }
        } catch (e: Exception) {
            Log.e("YIELD_PARSING", "Error parsing yield value: $yieldString", e)
            0.15 // Default fallback value (150g = 0.15kg)
        }
    }

    // Log the source of the report request
    private fun logReportSource() {
        val hasArData = intent.hasExtra("AR_AREA") && intent.hasExtra("AR_PERIMETER")
        val source = if (hasArData) "AR Screen" else "View Reports Button"
        Log.d("REPORT", "Report requested from: $source")
        
        // Log all available extras for debugging
        intent.extras?.keySet()?.forEach { key ->
            Log.d("REPORT", "Intent extra: $key = ${intent.extras?.get(key)}")
        }
    }
}