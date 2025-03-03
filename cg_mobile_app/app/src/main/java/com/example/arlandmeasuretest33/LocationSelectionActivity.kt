package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AlphaAnimation
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.Toast

class LocationSelectionActivity : AppCompatActivity() {
    private lateinit var gardenNameInput: TextInputEditText
    private lateinit var districtSpinner: AutoCompleteTextView
    private lateinit var continueButton: MaterialButton
    private lateinit var tipsContent: TextView  // Reference to the tips content TextView

    private val tips = arrayOf(
        "Colombo is known for its diverse climate. Ensure your garden gets enough sunlight.",
        "Anuradhapura has a hot climate. Regular watering is key to keeping plants healthy.",
        "Kandy's cooler temperatures are great for certain plants. Keep them in a shaded area.",
        "Nuwara Eliya has a cooler climate, ideal for a variety of flowers. Consider adding some roses!",
        "Galle's coastal climate requires salt-tolerant plants. Be mindful of the soil's salinity.",
        "Jaffna is known for its dry weather. Make sure your garden has good drainage.",
        "Batticaloa's high humidity helps tropical plants thrive. Keep them well-watered.",
        "Trincomalee's dry conditions make it perfect for desert plants like cacti.",
        "Matara has a tropical climate, perfect for growing fruits like papaya and bananas.",
        "Badulla's cooler, high-altitude climate is great for strawberries and other berries."
    )

    private val handler = Handler()  // Handler to post updates to UI
    private var currentTipIndex = 0  // To track the current tip being displayed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_selection)

        // Initialize views
        initializeViews()
        setupDistrictSpinner()
        setupListeners()  // Call the setupListeners method to activate the button
        startTipsRotation()  // Start the tips rotation process immediately
    }

    private fun initializeViews() {
        gardenNameInput = findViewById(R.id.gardenNameInput)
        districtSpinner = findViewById(R.id.districtSpinner)
        continueButton = findViewById(R.id.continueButton)
        tipsContent = findViewById(R.id.tipsContent)  // Initialize the tips content TextView

        // Set initial tip text so it's visible immediately
        tipsContent.text = tips[currentTipIndex]
    }

    private fun setupDistrictSpinner() {
        val districts = arrayOf(
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo",
            "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara",
            "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar",
            "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya",
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"
        )

        val adapter = ArrayAdapter<String>(
            this,
            R.layout.item_location_dropdown,
            districts
        )

        districtSpinner.setAdapter(adapter)
    }

    private fun startTipsRotation() {
        // Use a Runnable to change the tips every 5 seconds (5000 ms)
        val runnable = object : Runnable {
            override fun run() {
                // Apply fade-out animation before changing the text
                val fadeOut = AlphaAnimation(1.0f, 0.0f)
                fadeOut.duration = 1500 // Slow down the fade-out to 1.5 seconds
                fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        // Change the text after the fade-out completes
                        tipsContent.text = tips[currentTipIndex]

                        // Apply fade-in animation after changing the text
                        val fadeIn = AlphaAnimation(0.0f, 1.0f)
                        fadeIn.duration = 1500 // Slow down the fade-in to 1.5 seconds
                        tipsContent.startAnimation(fadeIn)
                    }

                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })

                // Start fade-out animation
                tipsContent.startAnimation(fadeOut)

                // Update the index, cycling back to 0 when the end of the array is reached
                currentTipIndex = (currentTipIndex + 1) % tips.size

                // Post the next update after 5 seconds (5000 ms)
                handler.postDelayed(this, 5000)
            }
        }

        // Start the rotation immediately when the activity is created
        handler.post(runnable)
    }

    private fun setupListeners() {
        continueButton.setOnClickListener {
            val gardenName = gardenNameInput.text.toString()
            val selectedDistrict = districtSpinner.text.toString()

            try {
                // Store the selected location for weather feature
                val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putString("SELECTED_LOCATION", selectedDistrict)
                    apply()
                }

                // Continue to plant recommendations as before
                val intent = Intent(this, PlantRecommendationActivity::class.java).apply {
                    putExtra("SELECTED_DISTRICT", selectedDistrict)
                    putExtra("GARDEN_NAME", gardenName)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error navigating to recommendations", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
}