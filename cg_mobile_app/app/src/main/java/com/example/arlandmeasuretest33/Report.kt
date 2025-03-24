package com.example.arlandmeasuretest33

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
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
    private lateinit var downloadButton: ImageView  // Change to ImageView
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
        val recyclerView = findViewById<RecyclerView>(R.id.pdfRecyclerView)
        downloadButton = findViewById(R.id.downloadButton)

        // Paths
        val templatePath = copyAssetToInternalStorage("template.docx", this)
        val imagePath = copyAssetToInternalStorage("images.jpeg", this)
        val outputDocxPath = getExternalFilesDir(null)?.absolutePath + "/output.docx"
        pdfFilePath = getExternalFilesDir(null)?.absolutePath + "/output.pdf"

        if (templatePath == null || imagePath == null) {
            Log.e("ERROR", "Missing required files in assets folder.")
            return
        }

        // Show loading screen
        lottieView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        downloadButton.visibility = View.GONE

        // Get AR measurements from intent and calculate number of plants
        getArMeasurementsAndCalculatePlants()

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        currentUser =auth.currentUser
        // Get garden name from SharedPreferences instead of intent
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val gardenName = sharedPreferences.getString("GARDEN_NAME", "") ?: ""
        val plantName = sharedPreferences.getString("CURRENT_PLANT_NAME", "") ?: ""
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
                            println("hello")
                            // Proceed to fetch crop data and process document
                            fetchCropDataFromFirebase(userDistrict, userPlantName) { replacements ->
                                println("Replacements"+replacements)
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
            downloadPdfToDownloads(pdfFilePath, "Ceilão Grid Report.pdf")
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
    private fun fetchCropDataFromFirebase(district: String, cropName: String, callback: (Map<String, String>) -> Unit) {
        val replacementsMap = mutableMapOf<String, String>()

        // Set default values in case of error
        val defaultValues = mapOf(
            "description" to "Description not available",
            "estimated_expenses_per_Month" to "0",
            "expected_income_per_month" to "0",
            "expected_yield_per_plant" to "0",
            "market_price_per_unit" to "0",
            "growth_cycle_duration" to "0",
            "spacing" to "0.25"
        )

        // Reference to the crop document in Firestore
        val cropRef = db.collection("districts").document(district).collection("crops").document(cropName)
        println("abc" + cropRef)

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
                    val estimatedExpenses = document.getString("estimated_expenses_per_Month") ?: defaultValues["estimated_expenses_per_Month"]!!
                    val expectedIncome = document.getString("expected_income_per_month") ?: defaultValues["expected_income_per_month"]!!
                    val expectedYieldPerPlant = document.getString("expected_yield_per_plant") ?: defaultValues["expected_yield_per_plant"]!!
                    val marketPricePerUnit = document.getString("market_price_per_unit") ?: defaultValues["market_price_per_unit"]!!
                    val growthCycleDuration = document.getString("growth_cycle_duration") ?: defaultValues["growth_cycle_duration"]!!

                    // Basic crop information
                    replacementsMap["{{description}}"] = description
                    replacementsMap["{{estimated_expenses_per_Month}}"] = estimatedExpenses
                    replacementsMap["{{expected_income_per_month}}"] = expectedIncome
                    replacementsMap["{{expected_yield_per_plant}}"] = expectedYieldPerPlant
                    replacementsMap["{{market_price_per_unit}}"] = marketPricePerUnit
                    replacementsMap["{{growth_cycle_duration}}"] = growthCycleDuration

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

                    // Add basic area information to replacements
                    replacementsMap["{{sqm}}"] = String.format("%.2f", totalSqm)
                    replacementsMap["{{number_of_plants}}"] = numberOfPlants.toString()

                    // Extract numeric values for calculations
                    val seedCostPerUnit = 50.0 // Cost per plant in LKR (from "cost_per_unit" field)
                    val fertilizerCostPerUnit = document.getString("Fertilizers")?.toDoubleOrNull() ?: 2.0
                    val yieldPerPlant = parseYieldValue(expectedYieldPerPlant)
                    val pricePerKg = marketPricePerUnit.replace("LKR", "").trim().toDoubleOrNull() ?: 150.0

                    // ---- COST BREAKDOWN CALCULATIONS ----

                    // Seeds/Plants costs
                    val seedsQuantity = numberOfPlants
                    val seedsTotalCost = seedCostPerUnit * seedsQuantity

                    // Fertilizers costs (10kg as specified)
                    val fertilizerQuantity = 10
                    val fertilizerTotalCost = fertilizerCostPerUnit * fertilizerQuantity

                    // Total costs
                    val totalCost = seedsTotalCost + fertilizerTotalCost

                    // ---- REVENUE ESTIMATION CALCULATIONS ----

                    // Total yield
                    val totalYield = yieldPerPlant * numberOfPlants

                    // Total revenue
                    val totalRevenue = totalYield * pricePerKg

                    // Net profit
                    val netProfit = totalRevenue - totalCost

                    // ADD THESE LINES HERE - Update estimated expenses and expected income
                    replacementsMap["{{estimated_expenses_per_Month}}"] = String.format("%.2f", totalCost)
                    replacementsMap["{{expected_income_per_month}}"] = String.format("%.2f", netProfit)

                    // ---- ADD CALCULATIONS TO TEMPLATE PLACEHOLDERS ----

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

                    Log.d("FIREBASE", "✅ Successfully calculated all values for the report")
                } else {
                    Log.e("FIREBASE", "No data found for $district/$cropName")
                    // Use default values if no document exists
                    setDefaultReplacements(replacementsMap, district, cropName, defaultValues)
                }

                // Call the callback with the final map
                callback(replacementsMap)
            }
            .addOnFailureListener { exception ->
                Log.e("FIREBASE", "Error fetching crop data: ${exception.message}")
                // Use default values on error
                setDefaultReplacements(replacementsMap, district, cropName, defaultValues)
                callback(replacementsMap)
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
        replacementsMap["{{estimated_expenses_per_Month}}"] = defaultValues["estimated_expenses_per_Month"]!!
        replacementsMap["{{expected_income_per_month}}"] = defaultValues["expected_income_per_month"]!!
        replacementsMap["{{expected_yield_per_plant}}"] = defaultValues["expected_yield_per_plant"]!!
        replacementsMap["{{market_price_per_unit}}"] = defaultValues["market_price_per_unit"]!!
        replacementsMap["{{growth_cycle_duration}}"] = defaultValues["growth_cycle_duration"]!!

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

        // Update these lines in setDefaultReplacements
        replacementsMap["{{estimated_expenses_per_Month}}"] = String.format("%.2f", totalCost)
        replacementsMap["{{expected_income_per_month}}"] = String.format("%.2f", netProfit)


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
            // Get area and perimeter from intent extras (from AR activity)
            totalSqm = intent.getFloatExtra("AR_AREA", 0f).toDouble()
            perimeter = intent.getFloatExtra("AR_PERIMETER", 0f).toDouble()

            if (totalSqm <= 0) {
                Log.d("REPORT", "No AR area data found, using default value")
                totalSqm = 0.41 // Default value from screenshot (0.41 m²)
                perimeter = 2.65 // Default value from screenshot (2.65 m)
            }

            Log.d("REPORT", "Area from AR: $totalSqm sq m, Perimeter: $perimeter m")

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

                    // Cost per unit - Cell 1 (index 1)
                    if (seedsRow.tableCells.size > 1) {
                        setCellText(seedsRow.getCell(1), String.format("%.2f", 50.0) + " LKR")
                    }

                    // Quantity - Cell 2 (index 2)
                    if (seedsRow.tableCells.size > 2) {
                        setCellText(seedsRow.getCell(2), numberOfPlants.toString())
                    }

                    // Total Cost - Cell 3 (index 3)
                    if (seedsRow.tableCells.size > 3) {
                        val seedsTotalCost = 50.0 * numberOfPlants
                        setCellText(seedsRow.getCell(3), String.format("%.2f", seedsTotalCost) + " LKR")
                    }
                }

                // Fertilizers row - Row 2 (index 2)
                if (costTable.rows.size > 2) {
                    val fertRow = costTable.rows[2]

                    // Cost per unit - Cell 1 (index 1)
                    if (fertRow.tableCells.size > 1) {
                        setCellText(fertRow.getCell(1), String.format("%.2f", 2.0) + " LKR")
                    }

                    // Quantity - Cell 2 (index 2)
                    if (fertRow.tableCells.size > 2) {
                        setCellText(fertRow.getCell(2), "10 Kg")
                    }

                    // Total Cost - Cell 3 (index 3)
                    if (fertRow.tableCells.size > 3) {
                        val fertTotalCost = 2.0 * 10
                        setCellText(fertRow.getCell(3), String.format("%.2f", fertTotalCost) + " LKR")
                    }
                }

                // Total Cost row - Row 3 (index 3)
                if (costTable.rows.size > 3) {
                    val totalRow = costTable.rows[3]

                    // Total Cost - Cell 3 (index 3)
                    if (totalRow.tableCells.size > 3) {
                        val seedsTotalCost = 50.0 * numberOfPlants
                        val fertTotalCost = 2.0 * 10
                        val totalCost = seedsTotalCost + fertTotalCost
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
                    val yieldPerPlant = parseYieldValue(yieldPerPlantStr)

                    // Quantity - Cell 1 (index 1)
                    if (yieldRow.tableCells.size > 1) {
                        val totalYield = yieldPerPlant * numberOfPlants
                        setCellText(yieldRow.getCell(1), String.format("%.2f", totalYield) + " Kg")
                    }

                    // Price per Unit - Cell 2 (index 2)
                    if (yieldRow.tableCells.size > 2) {
                        // Get market price per unit value
                        val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                        val pricePerUnit = pricePerUnitStr.toDoubleOrNull() ?: 150.0
                        setCellText(yieldRow.getCell(2), String.format("%.2f", pricePerUnit) + " LKR/Kg")
                    }

                    // Total Revenue - Cell 3 (index 3)
                    if (yieldRow.tableCells.size > 3) {
                        val totalYield = yieldPerPlant * numberOfPlants
                        val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                        val pricePerUnit = pricePerUnitStr.toDoubleOrNull() ?: 150.0
                        val totalRevenue = totalYield * pricePerUnit
                        setCellText(yieldRow.getCell(3), String.format("%.2f", totalRevenue) + " LKR")
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
                    val yieldPerPlant = parseYieldValue(yieldPerPlantStr)
                    val totalYield = yieldPerPlant * numberOfPlants

                    val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                    val pricePerUnit = pricePerUnitStr.toDoubleOrNull() ?: 150.0
                    val totalRevenue = totalYield * pricePerUnit

                    // Replace the paragraph text
                    for (run in paragraph.runs) {
                        run.setText("", 0)
                    }
                    paragraph.createRun().setText("Total Revenue: " + String.format("%.2f", totalRevenue) + " LKR")
                }

                // Total Cost paragraph
                if (text.startsWith("Total Cost:")) {
                    // Calculate total cost
                    val seedsTotalCost = 50.0 * numberOfPlants
                    val fertTotalCost = 2.0 * 10
                    val totalCost = seedsTotalCost + fertTotalCost

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
                    val yieldPerPlant = parseYieldValue(yieldPerPlantStr)
                    val totalYield = yieldPerPlant * numberOfPlants

                    val pricePerUnitStr = replacementsMap["{{market_price_per_unit}}"] ?: "0"
                    val pricePerUnit = pricePerUnitStr.toDoubleOrNull() ?: 150.0
                    val totalRevenue = totalYield * pricePerUnit

                    val seedsTotalCost = 50.0 * numberOfPlants
                    val fertTotalCost = 2.0 * 10
                    val totalCost = seedsTotalCost + fertTotalCost

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
                    // Update UI only after PDF is successfully created
                    runOnUiThread {
                        lottieView.visibility = View.GONE
                        renderPdfToRecyclerView(pdfFile.absolutePath)
                        downloadButton.visibility = View.VISIBLE
                    }
                } else {
                    Log.e("ERROR", "PDF file created but appears to be empty or missing")
                    runOnUiThread {
                        lottieView.visibility = View.GONE
                        Toast.makeText(this, "Error creating PDF. Please try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to convert Word to PDF: ${e.message}")

            // If the current API key failed, try the next one
            currentApiKeyIndex = (currentApiKeyIndex + 1) % apiKeys.size
            runOnUiThread {
                lottieView.visibility = View.GONE
            }

            // Recursively call the function to try the next API key
            convertWordToPdf(inputDocx, outputPdf)
        }
    }

    // Function to render PDF first page to a RecyclerView
    private fun renderPdfToRecyclerView(pdfPath: String) {
        val file = File(pdfPath)
        if (!file.exists()) {
            Log.e("ERROR", "PDF file does not exist: $pdfPath")
            return
        }

        try {
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount

            val bitmaps = ArrayList<Bitmap>()
            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            pdfRenderer.close()

            val recyclerView = findViewById<RecyclerView>(R.id.pdfRecyclerView)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = PdfPageAdapter(bitmaps)

            runOnUiThread {
                lottieView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                downloadButton.visibility = View.VISIBLE
            }
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to render PDF: ${e.message}")
            runOnUiThread {
                lottieView.visibility = View.GONE
                Toast.makeText(this, "Failed to render PDF", Toast.LENGTH_LONG).show()
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
                yieldString.toDoubleOrNull() != null -> yieldString.toDouble()
                
                // Handle "X kg" or "X Kg" format
                yieldString.toLowerCase().contains("kg") -> {
                    val numericPart = yieldString.toLowerCase().replace("kg", "")
                        .replace("per cycle", "").replace("per plant", "").trim()
                    numericPart.toDoubleOrNull() ?: 0.15
                }
                
                // Default fallback value if no pattern matches
                else -> 0.15
            }
        } catch (e: Exception) {
            Log.e("YIELD_PARSING", "Error parsing yield value: $yieldString", e)
            0.15 // Default fallback value
        }
    }
}