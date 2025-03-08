package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PricePredictionActivity : AppCompatActivity() {
    private lateinit var plantNameTextView: TextView
    private lateinit var currentPriceTextView: TextView
    private lateinit var nextWeekPriceTextView: TextView
    private lateinit var nextTwoWeeksPriceTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var lastUpdatedTextView: TextView
    private lateinit var accuracyTextView: TextView
    private lateinit var goToArButton: Button

    private val db = FirebaseFirestore.getInstance()
    private var currentPlantType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_price_prediction)

        // Initialize views
        plantNameTextView = findViewById(R.id.plantNameTextView)
        currentPriceTextView = findViewById(R.id.currentPriceTextView)
        nextWeekPriceTextView = findViewById(R.id.nextWeekPriceTextView)
        nextTwoWeeksPriceTextView = findViewById(R.id.nextTwoWeeksPriceTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        errorTextView = findViewById(R.id.errorTextView)
        lastUpdatedTextView = findViewById(R.id.lastUpdatedTextView)
        accuracyTextView = findViewById(R.id.accuracyTextView)
        goToArButton = findViewById(R.id.goToArButton)

        // Get plant type from intent
        val plantType = intent.getStringExtra("PLANT_TYPE") ?: ""
        currentPlantType = plantType

        if (plantType.isNotEmpty()) {
            plantNameTextView.text = formatPlantName(plantType)
            loadPricePredictions(plantType)

            // Set up AR button click listener
            goToArButton.setOnClickListener {
                startARFeature(plantType)
            }
        } else {
            showError("No plant selected")
            goToArButton.visibility = View.GONE
        }
    }

    private fun formatPlantName(plantName: String): String {
        // Capitalize first letter of each word
        return plantName.split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase() }
        }
    }

    private fun loadPricePredictions(plantType: String) {
        showLoading(true)

        // Normalize the plant name to match the database collection structure
        val normalizedPlantName = getNormalizedPlantName(plantType)

        db.collection("commodities").document(normalizedPlantName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        // Get the data from the document
                        // Handle last_updated field more carefully - it might be a Timestamp or other type
                        val lastUpdated = try {
                            // Try different approaches to get the last_updated value
                            when {
                                document.contains("last_updated") -> {
                                    document.get("last_updated")?.toString() ?: "Unknown"
                                }
                                else -> "Unknown"
                            }
                        } catch (e: Exception) {
                            "Unknown"
                        }

                        val modelMetrics = document.get("model_metrics") as? Map<String, Any>
                        val predictions = document.get("predictions") as? Map<String, Any>

                        // Update UI with the data
                        if (predictions != null && predictions.isNotEmpty()) {
                            // Sort dates to display predictions in chronological order
                            val sortedDates = predictions.keys.sorted()

                            // Set the prices for upcoming weeks
                            if (sortedDates.isNotEmpty()) {
                                // Current/today's price (closest date)
                                val currentDate = sortedDates.first()
                                val currentPrice = predictions[currentDate]?.toString() ?: "N/A"
                                currentPriceTextView.text = "Rs. $currentPrice / kg"

                                // Next week prediction
                                if (sortedDates.size > 1) {
                                    val nextWeekDate = sortedDates[1]
                                    val nextWeekPrice = predictions[nextWeekDate]?.toString() ?: "N/A"
                                    nextWeekPriceTextView.text = "Rs. $nextWeekPrice / kg"
                                }

                                // Two weeks prediction
                                if (sortedDates.size > 2) {
                                    val nextTwoWeeksDate = sortedDates[2]
                                    val nextTwoWeeksPrice = predictions[nextTwoWeeksDate]?.toString() ?: "N/A"
                                    nextTwoWeeksPriceTextView.text = "Rs. $nextTwoWeeksPrice / kg"
                                }
                            }
                        } else {
                            currentPriceTextView.text = "No price data available"
                            nextWeekPriceTextView.text = "No prediction available"
                            nextTwoWeeksPriceTextView.text = "No prediction available"
                        }

                        // Display model accuracy
                        if (modelMetrics != null) {
                            val r2Value = modelMetrics["R²"]?.toString()?.toDoubleOrNull() ?: 0.0
                            val accuracyPercentage = r2Value * 100
                            accuracyTextView.text = "Model Accuracy: ${String.format("%.1f", accuracyPercentage)}%"
                        } else {
                            accuracyTextView.text = "Model Accuracy: N/A"
                        }

                        // Display last updated date
                        lastUpdatedTextView.text = "Last updated: $lastUpdated"

                        // Show the AR button
                        goToArButton.visibility = View.VISIBLE

                        showLoading(false)
                    } catch (e: Exception) {
                        showError("Error parsing data: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    showError("No price predictions available for $plantType")
                }
            }
            .addOnFailureListener { e ->
                showError("Failed to load data: ${e.message}")
                e.printStackTrace()
            }
    }

    private fun getNormalizedPlantName(plantType: String): String {
        // Convert plant names to match your Firestore document names
        return when (plantType.lowercase()) {
            "bitter melon" -> "Bitter Melon"
            "winged bean" -> "Winged Bean"
            "red spinach" -> "Red Spinach"
            "long purple eggplant" -> "Long Purple Eggplant"
            "beetroot" -> "Beetroot"
            "brinjal" -> "Brinjal"
            "carrot" -> "Carrot"
            "cabbage" -> "Cabbage"
            "leeks" -> "Leeks"
            "potato" -> "Potato"
            "onion" -> "Onion"
            "manioc" -> "Manioc"
            "taro" -> "Taro"
            "eggplant" -> "Eggplant"
            "pumpkin" -> "Pumpkin"
            "knolkhol" -> "Knol-Khol"
            "drumstick" -> "Drumsticks"
            else -> plantType.replaceFirstChar { it.uppercase() }
        }
    }

    private fun startARFeature(plantType: String) {
        try {
            val intent = Intent(this, MainActivity::class.java)  // Changed back to MainActivity
            intent.putExtra("PLANT_TYPE", plantType)
            intent.putExtra("START_AR", true)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        errorTextView.visibility = View.GONE

        val contentVisibility = if (isLoading) View.GONE else View.VISIBLE
        currentPriceTextView.visibility = contentVisibility
        nextWeekPriceTextView.visibility = contentVisibility
        nextTwoWeeksPriceTextView.visibility = contentVisibility
        lastUpdatedTextView.visibility = contentVisibility
        accuracyTextView.visibility = contentVisibility
        goToArButton.visibility = if (isLoading || currentPlantType.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        loadingProgressBar.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = message

        // Hide other content
        currentPriceTextView.visibility = View.GONE
        nextWeekPriceTextView.visibility = View.GONE
        nextTwoWeeksPriceTextView.visibility = View.GONE
        lastUpdatedTextView.visibility = View.GONE
        accuracyTextView.visibility = View.GONE
        goToArButton.visibility = View.GONE
    }
}