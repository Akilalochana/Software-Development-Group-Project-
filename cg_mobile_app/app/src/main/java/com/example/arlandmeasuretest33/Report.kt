package com.example.arlandmeasuretest33

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.*
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.lottie.LottieAnimationView

val db = FirebaseFirestore.getInstance()

class Report : AppCompatActivity() {
    private lateinit var lottieView: LottieAnimationView
    private lateinit var pdfImageView: ImageView
    private lateinit var downloadButton: ImageView  // Change to ImageView
    private var pdfFilePath: String = ""
    private var plantSpacing: Double = 0.0 // Space needed per plant in sq meters
    private var totalSqm: Double = 0.0 // Total area in sq meters
    private var numberOfPlants: Int = 0 // Calculated number of plants
    private var perimeter: Double = 0.0 // Perimeter from AR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.report)

        db.collection("districts").document("Mannar").collection("crops")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    println("Crop: ${document.id}")
                }
            }
            .addOnFailureListener { exception ->
                println("Error getting documents: ${exception.message}")
            }

        // UI Elements
        lottieView = findViewById(R.id.lottie_view)
        val recyclerView = findViewById<RecyclerView>(R.id.pdfRecyclerView)
        downloadButton = findViewById(R.id.downloadButton)  // Change to ImageView

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

        // Format sqm representation: width×height or area value
        val sqmRepresentation = formatAreaRepresentation()

        // Generate data map
        val replacements = mapOf(
            "{{date}}" to "2025-02-17",
            "{{plant_type}}" to "NuwaraEliya/Carrot",
            "{{description}}" to "The carrot (Daucus carota) is a root vegetable often claimed to be the perfect health food. It is crunchy, tasty, and highly nutritious.",
            "{{sqm}}" to sqmRepresentation,
            "{{number_of_plants}}" to numberOfPlants.toString(),
            "{{estimated_expenses_per_Month}}" to "36000",
            "{{expected_income_per_month}}" to "82000",
            "{{expected_yield_per_plant}}" to "3",
            "{{market_price_per_unit}}" to "180",
            "{{growth_cycle_duration}}" to "60",
        )

        // Start Processing in Background
        Thread {
            modifyWordDocument(templatePath, outputDocxPath, imagePath, replacements)
            convertWordToPdf(outputDocxPath, pdfFilePath)
            // UI update happens inside convertWordToPdf after successful conversion
        }.start()

        //Set Download Button Click Event
        downloadButton = findViewById(R.id.downloadButton)
        downloadButton.setOnClickListener {
            downloadPdfToDownloads(pdfFilePath, "Ceilão Grid Report.pdf")
        }
    }

    // Function to get AR measurements from intent and calculate plants
    private fun getArMeasurementsAndCalculatePlants() {
        try {
            // Get area and perimeter from intent extras (from AR activity)
            totalSqm = intent.getFloatExtra("AR_AREA", 0f).toDouble()
            perimeter = intent.getFloatExtra("AR_PERIMETER", 0f).toDouble()

//            // If no AR data was passed, fallback to default values (for testing)
//            if (totalSqm <= 0) {
//                Log.d("REPORT", "No AR area data found, using default value")
//                totalSqm = 0.41 // Default value from screenshot (0.41 m²)
//                perimeter = 2.65 // Default value from screenshot (2.65 m)
//            }

            Log.d("REPORT", "Area from AR: $totalSqm sq m, Perimeter: $perimeter m")

            // Get plant spacing from Firebase and calculate number of plants
            getPlantSpacingFromFirebase("Carrot") { spacing ->
                plantSpacing = spacing
                numberOfPlants = (totalSqm / plantSpacing).toInt()

                // Ensure at least 1 plant if area is positive but very small
                if (totalSqm > 0 && numberOfPlants < 1) {
                    numberOfPlants = 1
                }

                Log.d("PLANTS", "Area: $totalSqm sq m, Plants per sq m: $plantSpacing, Total plants: $numberOfPlants")
            }
        } catch (e: Exception) {
            Log.e("ERROR", "Failed to calculate number of plants: ${e.message}")
            numberOfPlants = 43 // Fallback to hardcoded value
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

    // Function to get plant spacing from Firebase
    private fun getPlantSpacingFromFirebase(cropName: String, callback: (Double) -> Unit) {
        // In a real implementation, you would fetch this from Firebase
        // For now, using hardcoded value
        val spacing = 0.25 // 0.25 sq meters per plant (example)
        callback(spacing)

        // Actual Firebase implementation would look like:
        /*
        db.collection("crops").document(cropName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val spacing = document.getDouble("spacing") ?: 0.25
                    callback(spacing)
                } else {
                    Log.d("FIREBASE", "No spacing data for $cropName")
                    callback(0.25) // Default spacing
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FIREBASE", "Error getting spacing data: ${exception.message}")
                callback(0.25) // Default spacing on error
            }
        */
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

    // Function to modify Word template
    private fun modifyWordDocument(
        templatePath: String,
        outputPath: String,
        imagePath: String,
        replacementsMap: Map<String, String>
    ) {
        try {
            val doc = XWPFDocument(FileInputStream(templatePath))

            // Replace text in paragraphs
            for (paragraph in doc.paragraphs) {
                val text = paragraph.text
                var modifiedText = text

                for ((key, value) in replacementsMap) {
                    if (modifiedText.contains(key)) {
                        modifiedText = modifiedText.replace(key, value)
                    }
                }

                if (text != modifiedText) {
                    for (run in paragraph.runs) {
                        run.setText("", 0) // Clear existing text
                    }
                    paragraph.createRun().setText(modifiedText) // Set new text
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
        "APY04NmgEqQehQXgnh2xO1VyywB4SZdkTD7S0yQPbEiyJsp4lB3TYcQFXbcRPvSgIa8WUuqu6lq82R"
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
                Toast.makeText(this, "Failed to convert document to PDF. Trying next API key.", Toast.LENGTH_LONG).show()
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
        }
    }

    // Function to clear cache
    private fun clearCache() {
        try {
            // Delete the temporary Word document
            val outputDocxPath = getExternalFilesDir(null)?.absolutePath + "/output.docx"
            val docxFile = File(outputDocxPath)
            if (docxFile.exists()) {
                docxFile.delete()
                Log.d("CACHE", "Deleted temporary Word document: $outputDocxPath")
            }

            // Delete the temporary PDF file
            val pdfFilePath = getExternalFilesDir(null)?.absolutePath + "/output.pdf"
            val pdfFile = File(pdfFilePath)
            if (pdfFile.exists()) {
                pdfFile.delete()
                Log.d("CACHE", "Deleted temporary PDF file: $pdfFilePath")
            }

            // Optionally, delete the entire cache directory
            val cacheDir = getExternalFilesDir(null)
            if (cacheDir != null && cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
                Log.d("CACHE", "Cleared entire cache directory: ${cacheDir.absolutePath}")
            }

            // Show success message
            runOnUiThread {
                Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CACHE", "Failed to clear cache: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show()
            }
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

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destinationFile = File(downloadsDir, fileName)

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()  // Create Downloads folder if not exists
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

        } catch (e: IOException) {
            Log.e("ERROR", "Failed to copy PDF to Downloads: ${e.message}")

            runOnUiThread {
                Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_LONG).show()
            }
        }
    }
}