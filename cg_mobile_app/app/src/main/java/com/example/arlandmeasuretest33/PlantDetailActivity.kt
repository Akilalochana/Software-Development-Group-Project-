package com.example.arlandmeasuretest33

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var saveButton: Button
    private lateinit var plantImage: ImageView
    private lateinit var plantName: TextView
    private lateinit var plantSubtitle: TextView
    private lateinit var plantDescription: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_detail)

        // Initialize views
        backButton = findViewById(R.id.backButton)
        saveButton = findViewById(R.id.saveButton)
        plantImage = findViewById(R.id.plantImage)
        plantName = findViewById(R.id.plantName)
        plantSubtitle = findViewById(R.id.plantSubtitle)
        plantDescription = findViewById(R.id.plantDescription)

        // Set up back button
        backButton.setOnClickListener {
            onBackPressed()
        }

        // Set up save button
        saveButton.setOnClickListener {
            // Save plant to favorites or user garden (to be implemented)
            android.widget.Toast.makeText(
                this,
                "Plant saved!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // Get plant data from intent
        val plantName = intent.getStringExtra("PLANT_NAME") ?: "Unknown Plant"
        val plantCategory = intent.getStringExtra("PLANT_CATEGORY") ?: "Unknown Category"

        // Display plant data
        displayPlantData(plantName, plantCategory)
    }

    private fun displayPlantData(name: String, category: String) {
        // Set image and text fields with placeholder data
        plantImage.setImageResource(R.drawable.ic_plant)
        plantName.text = name
        plantSubtitle.text = "Lectus mauris dolor (In rutrum)"
        plantDescription.text = """
            Nam eu varius sapien. Vestibulum nisi metus, aliquet et urna a, laoreet fermentum dui. Proin sed urna massa. Proin quis elementum mi imperdiet risim. Curabitur consequat, libero gravida.
            
            Plant details and care instructions will be added here. This will include information about watering, sunlight requirements, soil preferences, and other care tips.
        """.trimIndent()
    }
} 