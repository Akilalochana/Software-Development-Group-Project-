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
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.*
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import com.google.firebase.firestore.FirebaseFirestore

val db = FirebaseFirestore.getInstance()

class Report : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var pdfImageView: ImageView
    private lateinit var downloadButton: Button
    private var pdfFilePath: String = ""

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
        progressBar = findViewById(R.id.progressBar)
        pdfImageView = findViewById(R.id.pdfImageView)
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
        progressBar.visibility = View.VISIBLE
        pdfImageView.visibility = View.GONE
        downloadButton.visibility = View.GONE

        // Generate data map
        val replacements = mapOf(
            "{{date}}" to "2025-02-17",
            "{{plant_type}}" to "NuwaraEli/Carrot",
            "{{description}}" to "The carrot (Daucus carota) is a root vegetable often claimed to be the perfect health food. It is crunchy, tasty, and highly nutritious.",
            "{{sqm}}" to "250x569",
            "{{number_of_plants}}" to "43",
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

            runOnUiThread {
                progressBar.visibility = View.GONE
                pdfImageView.visibility = View.VISIBLE
                downloadButton.visibility = View.VISIBLE

                // Load the first page of PDF into ImageView
                renderPdfToImage(pdfFilePath)
            }
        }.start()

        //Set Download Button Click Event
        downloadButton.setOnClickListener {
            downloadPdfToDownloads(pdfFilePath, "GeneratedPDF.pdf")
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

    // Function to convert Word to PDF
    private fun convertWordToPdf(inputDocx: String, outputPdf: String) {
        val client = OkHttpClient()
        val file = File(inputDocx)

        if (!file.exists()) {
            Log.e("ERROR", "DOCX file not found at $inputDocx")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody())
            .build()

        val request = Request.Builder()
            .url("https://api.apyhub.com/convert/word-file/pdf-file?output=test-sample.pdf&landscape=false")
            .post(requestBody)
            .header(
                "apy-token",
                "APY0teeUq8CAtaq9CVw6P98KFjL8i392G0AUt9XI8RjLkpscbE3Neerdd3N1emmCj1rI"
            )  // Replace with your API key
            .header("content-type", "multipart/form-data")
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected code $response")
                    }

                    // Save the converted PDF
                    val pdfFile = File(outputPdf)
                    pdfFile.writeBytes(response.body!!.bytes())

                    Log.d("SUCCESS", "✅ PDF Created Successfully: ${pdfFile.absolutePath}")

                    // Update UI
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        pdfImageView.visibility = View.VISIBLE
                        downloadButton.visibility = View.VISIBLE
                        renderPdfToImage(pdfFile.absolutePath)
                    }
                }
            } catch (e: IOException) {
                Log.e("ERROR", "Failed to convert Word to PDF: ${e.message}")
            }
        }.start()
    }


    // Function to render PDF first page to an ImageView
    private fun renderPdfToImage(pdfPath: String) {
        val file = File(pdfPath)

        if (!file.exists()) {
            Log.e("ERROR", "PDF file does not exist: $pdfPath")
            runOnUiThread {
                Toast.makeText(this, "PDF file not found. Please try again.", Toast.LENGTH_LONG)
                    .show()
            }
            return
        }

        try {
            val fileDescriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val page = pdfRenderer.openPage(0)

            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfImageView.setImageBitmap(bitmap)

            page.close()
            pdfRenderer.close()
        } catch (e: IOException) {
            Log.e("ERROR", "Failed to render PDF: ${e.message}")
        }
    }

    // Function to save the PDF to the Downloads folder
    private fun downloadPdfToDownloads(pdfPath: String, fileName: String) {
        try {
            val sourceFile = File(pdfPath)
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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

        } catch (e: IOException) {
            Log.e("ERROR", "Failed to copy PDF to Downloads: ${e.message}")

            runOnUiThread {
                Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_LONG).show()
            }
        }
    }
}