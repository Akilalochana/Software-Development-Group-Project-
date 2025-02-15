package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.Button

class LocationSelectionActivity : AppCompatActivity() {
    private lateinit var gardenNameInput: TextInputEditText
    private lateinit var districtSpinner: AutoCompleteTextView
    private lateinit var continueButton: MaterialButton
    private var selectedPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_selection)

        initializeViews()
        setupDistrictSpinner()
        setupListeners()
    }

    private fun initializeViews() {
        gardenNameInput = findViewById(R.id.gardenNameInput)
        districtSpinner = findViewById(R.id.districtSpinner)
        continueButton = findViewById(R.id.continueButton)
    }

    private fun setupDistrictSpinner() {
        val districts = arrayOf(
            "Colombo",
            "Anuradhapura",
            "Kandy",
            "Nuwara Eliya"
        )

        val adapter = object : ArrayAdapter<String>(
            this,
            R.layout.item_location_dropdown,
            districts
        ) {
            override fun getDropDownView(
                position: Int,
                convertView: android.view.View?,
                parent: android.view.ViewGroup
            ): android.view.View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                
                // Add padding for better spacing
                view.setPadding(32, 24, 32, 24)
                
                if (position == selectedPosition) {
                    view.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                    view.setTextColor(getColor(R.color.garden_green))
                    view.typeface = android.graphics.Typeface.DEFAULT_BOLD
                } else {
                    view.setBackgroundColor(android.graphics.Color.WHITE)
                    view.setTextColor(android.graphics.Color.BLACK)
                    view.typeface = android.graphics.Typeface.DEFAULT
                }

                // Add divider except for last item
                if (position < districts.size - 1) {
                    view.setBackgroundResource(R.drawable.dropdown_item_background)
                }
                
                return view
            }
        }
        
        districtSpinner.setAdapter(adapter)
        
        districtSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedPosition = position
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupListeners() {
        continueButton.setOnClickListener {
            val gardenName = gardenNameInput.text.toString()
            val selectedDistrict = districtSpinner.text.toString()

            try {
                // Create intent to start PlantRecommendationActivity
                val intent = Intent(this, PlantRecommendationActivity::class.java)
                
                // Pass any necessary data from location selection
                intent.putExtra("SELECTED_DISTRICT", selectedDistrict)
                intent.putExtra("GARDEN_NAME", gardenName)
                
                // Start the PlantRecommendationActivity
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error navigating to recommendations", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSelectedDistrict(): String {
        // Get the selected district from your UI
        // Replace this with your actual implementation
        return ""
    }

    private fun getGardenName(): String {
        // Get the garden name from your UI
        // Replace this with your actual implementation
        return ""
    }
} 