package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class PricePredictionActivity : AppCompatActivity() {
    private lateinit var plantNameTextView: TextView
    private lateinit var plantImageView: ImageView
    // Removed currentPriceTextView
    private lateinit var nextWeekPriceTextView: TextView
    private lateinit var nextTwoWeeksPriceTextView: TextView
    private lateinit var futureDateOneTextView: TextView
    private lateinit var futurePriceOneTextView: TextView
    private lateinit var futureDateTwoTextView: TextView
    private lateinit var futurePriceTwoTextView: TextView
    private lateinit var futureDateThreeTextView: TextView
    private lateinit var futurePriceThreeTextView: TextView
    private lateinit var futureDateFourTextView: TextView
    private lateinit var futurePriceFourTextView: TextView
    private lateinit var futureDateFiveTextView: TextView
    private lateinit var futurePriceFiveTextView: TextView
    private lateinit var currentDateTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var goToArButton: Button

    private val db = FirebaseFirestore.getInstance()
    private var currentPlantType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_price_prediction)

        // Initialize views
        plantNameTextView = findViewById(R.id.plantNameTextView)
        plantImageView = findViewById(R.id.plantImageView)
        // Removed currentPriceTextView initialization
        nextWeekPriceTextView = findViewById(R.id.nextWeekPriceTextView)
        nextTwoWeeksPriceTextView = findViewById(R.id.nextTwoWeeksPriceTextView)
        futureDateOneTextView = findViewById(R.id.futureDateOneTextView)
        futurePriceOneTextView = findViewById(R.id.futurePriceOneTextView)
        futureDateTwoTextView = findViewById(R.id.futureDateTwoTextView)
        futurePriceTwoTextView = findViewById(R.id.futurePriceTwoTextView)
        futureDateThreeTextView = findViewById(R.id.futureDateThreeTextView)
        futurePriceThreeTextView = findViewById(R.id.futurePriceThreeTextView)
        futureDateFourTextView = findViewById(R.id.futureDateFourTextView)
        futurePriceFourTextView = findViewById(R.id.futurePriceFourTextView)
        futureDateFiveTextView = findViewById(R.id.futureDateFiveTextView)
        futurePriceFiveTextView = findViewById(R.id.futurePriceFiveTextView)
        currentDateTextView = findViewById(R.id.currentDateTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        errorTextView = findViewById(R.id.errorTextView)
        goToArButton = findViewById(R.id.continueButton)

        // Get plant type from intent
        val plantType = intent.getStringExtra("PLANT_TYPE") ?: ""
        currentPlantType = plantType

        if (plantType.isNotEmpty()) {
            plantNameTextView.text = formatPlantName(plantType)

            // Set plant image based on type
            setPlantImage(plantType)

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

    /**
     * Helper function to format any Firestore numeric value to two decimals, or "N/A" if invalid.
     */
    private fun formatTwoDecimals(value: Any?): String {
        val doubleVal = value?.toString()?.toDoubleOrNull()
        return if (doubleVal != null) {
            String.format("%.2f", doubleVal)
        } else {
            "N/A"
        }
    }

    private fun setPlantImage(plantType: String) {
        // Set appropriate drawable based on plant type
        val resourceId = when (plantType.lowercase()) {
            "carrot" -> R.drawable.img_price_preditcion
            "bitter melon" -> R.drawable.img_price_preditcion
            "winged bean" -> R.drawable.img_price_preditcion
            "red spinach" -> R.drawable.img_price_preditcion
            "long purple eggplant" -> R.drawable.img_price_preditcion
            "beetroot" -> R.drawable.img_price_preditcion
            "brinjal" -> R.drawable.img_price_preditcion
            "cabbage" -> R.drawable.img_price_preditcion
            "leeks" -> R.drawable.img_price_preditcion
            "potato" -> R.drawable.img_price_preditcion
            "onion" -> R.drawable.img_price_preditcion
            "manioc" -> R.drawable.img_price_preditcion
            "taro" -> R.drawable.img_price_preditcion
            "pumpkin" -> R.drawable.img_price_preditcion
            "knolkhol" -> R.drawable.img_price_preditcion
            "drumstick" -> R.drawable.img_price_preditcion
            else -> R.drawable.img_price_preditcion // Default placeholder image
        }

        plantImageView.setImageResource(resourceId)
    }

    private fun formatPlantName(plantName: String): String {
        // Capitalize first letter of each word
        return plantName.split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase() }
        }
    }

    private fun loadPricePredictions(plantType: String) {
        showLoading(true)

        val normalizedPlantName = getNormalizedPlantName(plantType)

        db.collection("commodities").document(normalizedPlantName)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val predictions = document.get("predictions") as? Map<String, Any>

                        if (predictions != null && predictions.isNotEmpty()) {
                            // Sort dates to display predictions in chronological order
                            val sortedDates = predictions.keys.sorted()

                            // Populate all the prediction UI elements
                            if (sortedDates.isNotEmpty()) {
                                // Today (Week 0) - Removed current price setting
                                if (sortedDates.size > 0) {
                                    val currentDate = sortedDates[0]
                                    // Removed currentPriceTextView.text setting

                                    // Update the current date display
                                    val formattedCurrentDate = formatFirestoreDate(currentDate)
                                    currentDateTextView.text = formattedCurrentDate
                                }

                                // Next week (Week 1)
                                if (sortedDates.size > 1) {
                                    val nextWeekDate = sortedDates[1]
                                    val nextWeekPrice = formatTwoDecimals(predictions[nextWeekDate])
                                    nextWeekPriceTextView.text = "Rs. $nextWeekPrice"
                                }

                                // Two weeks (Week 2)
                                if (sortedDates.size > 2) {
                                    val nextTwoWeeksDate = sortedDates[2]
                                    val nextTwoWeeksPrice = formatTwoDecimals(predictions[nextTwoWeeksDate])
                                    nextTwoWeeksPriceTextView.text = "Rs. $nextTwoWeeksPrice"
                                }

                                // Week 3
                                if (sortedDates.size > 3) {
                                    val additionalDate1 = formatFirestoreDate(sortedDates[3])
                                    val additionalPrice1 = formatTwoDecimals(predictions[sortedDates[3]])
                                    futureDateOneTextView.text = additionalDate1
                                    futurePriceOneTextView.text = "Rs. $additionalPrice1"
                                }

                                // Week 4
                                if (sortedDates.size > 4) {
                                    val additionalDate2 = formatFirestoreDate(sortedDates[4])
                                    val additionalPrice2 = formatTwoDecimals(predictions[sortedDates[4]])
                                    futureDateTwoTextView.text = additionalDate2
                                    futurePriceTwoTextView.text = "Rs. $additionalPrice2"
                                }

                                // Week 5
                                if (sortedDates.size > 5) {
                                    val additionalDate3 = formatFirestoreDate(sortedDates[5])
                                    val additionalPrice3 = formatTwoDecimals(predictions[sortedDates[5]])
                                    futureDateThreeTextView.text = additionalDate3
                                    futurePriceThreeTextView.text = "Rs. $additionalPrice3"
                                }

                                // Week 6
                                if (sortedDates.size > 6) {
                                    val additionalDate4 = formatFirestoreDate(sortedDates[6])
                                    val additionalPrice4 = formatTwoDecimals(predictions[sortedDates[6]])
                                    futureDateFourTextView.text = additionalDate4
                                    futurePriceFourTextView.text = "Rs. $additionalPrice4"
                                }

                                // Week 7
                                if (sortedDates.size > 7) {
                                    val additionalDate5 = formatFirestoreDate(sortedDates[7])
                                    val additionalPrice5 = formatTwoDecimals(predictions[sortedDates[7]])
                                    futureDateFiveTextView.text = additionalDate5
                                    futurePriceFiveTextView.text = "Rs. $additionalPrice5"
                                }
                            }
                        } else {
                            // Handle no data scenario
                            // Removed currentPriceTextView.text = "No data"
                            nextWeekPriceTextView.text = "No data"
                            nextTwoWeeksPriceTextView.text = "No data"
                            futurePriceOneTextView.text = "No data"
                            futurePriceTwoTextView.text = "No data"
                            futurePriceThreeTextView.text = "No data"
                            futurePriceFourTextView.text = "No data"
                            futurePriceFiveTextView.text = "No data"
                        }

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

    private fun formatFirestoreDate(firestoreDate: String): String {
        // Convert from "yyyy-MM-dd" to "dd-MM-yyyy"
        return try {
            val parts = firestoreDate.split("-")
            if (parts.size == 3) {
                "${parts[2]}-${parts[1]}-${parts[0]}"
            } else {
                firestoreDate
            }
        } catch (e: Exception) {
            firestoreDate
        }
    }

    private fun getNormalizedPlantName(plantType: String): String {
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
            val intent = Intent(this, MainActivity::class.java)
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
        // Removed currentPriceTextView.visibility = contentVisibility
        nextWeekPriceTextView.visibility = contentVisibility
        nextTwoWeeksPriceTextView.visibility = contentVisibility
        futureDateOneTextView.visibility = contentVisibility
        futurePriceOneTextView.visibility = contentVisibility
        futureDateTwoTextView.visibility = contentVisibility
        futurePriceTwoTextView.visibility = contentVisibility
        futureDateThreeTextView.visibility = contentVisibility
        futurePriceThreeTextView.visibility = contentVisibility
        futureDateFourTextView.visibility = contentVisibility
        futurePriceFourTextView.visibility = contentVisibility
        futureDateFiveTextView.visibility = contentVisibility
        futurePriceFiveTextView.visibility = contentVisibility

        goToArButton.visibility = if (isLoading || currentPlantType.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        loadingProgressBar.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = message

        // Hide other content
        // Removed currentPriceTextView.visibility = View.GONE
        nextWeekPriceTextView.visibility = View.GONE
        nextTwoWeeksPriceTextView.visibility = View.GONE
        futureDateOneTextView.visibility = View.GONE
        futurePriceOneTextView.visibility = View.GONE
        futureDateTwoTextView.visibility = View.GONE
        futurePriceTwoTextView.visibility = View.GONE
        futureDateThreeTextView.visibility = View.GONE
        futurePriceThreeTextView.visibility = View.GONE
        futureDateFourTextView.visibility = View.GONE
        futurePriceFourTextView.visibility = View.GONE
        futureDateFiveTextView.visibility = View.GONE
        futurePriceFiveTextView.visibility = View.GONE

        goToArButton.visibility = View.GONE
    }
}