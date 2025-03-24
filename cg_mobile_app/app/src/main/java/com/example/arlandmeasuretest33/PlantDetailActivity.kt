package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var shareButton: Button
    private lateinit var plantImage: ImageView
    private lateinit var plantName: TextView
    private lateinit var plantSubtitle: TextView
    private lateinit var plantDescription: TextView
    private lateinit var costValue: TextView
    private lateinit var growthPeriodValue: TextView
    private lateinit var yieldValue: TextView
    private lateinit var spaceValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)

        // Initialize views
        backButton = findViewById(R.id.backButton)
        shareButton = findViewById(R.id.shareButton)
        plantImage = findViewById(R.id.plantImage)
        plantName = findViewById(R.id.plantName)
        plantSubtitle = findViewById(R.id.plantSubtitle)
        plantDescription = findViewById(R.id.plantDescription)
        costValue = findViewById(R.id.costValue)
        growthPeriodValue = findViewById(R.id.growthPeriodValue)
        yieldValue = findViewById(R.id.yieldValue)
        spaceValue = findViewById(R.id.spaceValue)

        // Set up back button
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Set up share button
        shareButton.setOnClickListener {
            sharePlantInfo()
        }

        // Get plant data from intent
        val plantName = intent.getStringExtra("PLANT_NAME") ?: "Unknown Plant"
        val plantCategory = intent.getStringExtra("PLANT_CATEGORY") ?: "Unknown Category"

        // Display plant data
        displayPlantData(plantName, plantCategory)
    }
    
    private fun sharePlantInfo() {
        // Create share text content with plant information
        val shareText = buildString {
            append("🌱 PLANT INFORMATION REPORT 🌱\n\n")
            append("Plant: ${plantName.text}\n")
            append("Category: ${plantSubtitle.text}\n\n")
            
            append("📊 DETAILS:\n")
            append("• Cost per Unit: ${costValue.text}\n")
            append("• Growth Period: ${growthPeriodValue.text}\n")
            append("• Expected Yield: ${yieldValue.text}\n")
            append("• Space Required: ${spaceValue.text}\n\n")
            
            append("📝 DESCRIPTION:\n")
            append("${plantDescription.text}\n\n")
            
            append("Shared from Ceilão.Grid App")
        }
        
        // Create share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Plant Information: ${plantName.text}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun displayPlantData(name: String, category: String) {
        // Set image and text fields with placeholder data
        plantImage.setImageResource(R.drawable.aloe_vera)
        plantName.text = name
        plantSubtitle.text = category
        plantDescription.text = """
            Nam eu varius sapien. Vestibulum nisi metus, aliquet et urna a, laoreet fermentum dui. Proin sed urna massa. Proin quis elementum mi imperdiet risim. Curabitur consequat, libero gravida.
            
            Plant details and care instructions will be added here. This will include information about watering, sunlight requirements, soil preferences, and other care tips.
        """.trimIndent()
    }
} 