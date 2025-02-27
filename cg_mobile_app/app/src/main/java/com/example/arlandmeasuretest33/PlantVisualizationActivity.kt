package com.example.arlandmeasuretest33

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlin.math.sqrt
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.graphics.Color
import android.widget.Toast
import android.content.Intent
import android.widget.Button

class PlantVisualizationActivity : AppCompatActivity() {
    private lateinit var gridLayout: GridLayout
    private lateinit var titleText: TextView
    private lateinit var plantCountText: TextView
    private lateinit var areaText: TextView
    private lateinit var gardenContainer: FrameLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_visualization)
        
        // Initialize views
        gridLayout = findViewById(R.id.gridLayout)
        titleText = findViewById(R.id.titleText)
        plantCountText = findViewById(R.id.plantCountText)
        areaText = findViewById(R.id.areaText)
        gardenContainer = findViewById(R.id.gardenContainer)
        
        // Get area from AR measurement
        val area = intent.getFloatExtra("AREA", 0f)
        if (area <= 0f) {
            // If no area measurement, return to AR measurement
            Toast.makeText(this, "Please measure your garden area first", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        val plantType = intent.getStringExtra("PLANT_TYPE") ?: "carrot"
        
        updateHeaderInfo(area, plantType)
        createGardenBorder()
        createPlantGrid(area, plantType)
        setupContinueButton()
    }

    private fun createGardenBorder() {
        val border = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(8, Color.parseColor("#2E7D32")) // Darker green border
            val gradientColors = intArrayOf(
                Color.parseColor("#E8F5E9"), // Light green start
                Color.parseColor("#C8E6C9")  // Slightly darker green end
            )
            orientation = GradientDrawable.Orientation.TL_BR
            colors = gradientColors
            cornerRadius = resources.displayMetrics.density * 16
        }
        gardenContainer.background = border

        // Enhanced animation
        gardenContainer.alpha = 0f
        gardenContainer.scaleX = 0.8f
        gardenContainer.scaleY = 0.8f
        gardenContainer.translationY = 100f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(gardenContainer, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(gardenContainer, View.SCALE_X, 0.8f, 1f),
                ObjectAnimator.ofFloat(gardenContainer, View.SCALE_Y, 0.8f, 1f),
                ObjectAnimator.ofFloat(gardenContainer, View.TRANSLATION_Y, 100f, 0f)
            )
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
    
    private fun updateHeaderInfo(area: Float, plantType: String) {
        titleText.text = "${plantType.capitalize()} Garden Layout"
        // Calculate plants based on recommended spacing
        val plantsCount = calculateRecommendedPlants(area, plantType)
        plantCountText.text = "Recommended Plants: $plantsCount"
        areaText.text = "Measured Area: %.2f m²".format(area)
    }

    private fun calculateRecommendedPlants(area: Float, plantType: String): Int {
        // Different plants need different spacing
        val spacePerPlant = when (plantType.toLowerCase()) {
            "cabbage" -> 0.4f // 40cm x 40cm
            "tomato" -> 0.6f  // 60cm x 60cm
            "carrot" -> 0.1f  // 10cm x 10cm
            "radish" -> 0.15f // 15cm x 15cm
            "capsicum" -> 0.3f // 30cm x 30cm
            "brinjal" -> 0.5f // 50cm x 50cm
            "okra" -> 0.3f    // 30cm x 30cm
            else -> 0.25f     // default spacing
        }
        return (area / spacePerPlant).toInt()
    }
    
    private fun createPlantGrid(area: Float, plantType: String) {
        val plantsCount = calculateRecommendedPlants(area, plantType)
        
        // Calculate grid dimensions to fit exact number of plants
        val gridColumns = sqrt(plantsCount.toDouble()).ceil().toInt()
        val gridRows = (plantsCount + gridColumns - 1) / gridColumns // Ceiling division
        
        gridLayout.rowCount = gridRows
        gridLayout.columnCount = gridColumns
        
        val iconResource = getPlantIcon(plantType)
        val screenWidth = resources.displayMetrics.widthPixels
        val availableWidth = screenWidth - (32 * resources.displayMetrics.density).toInt()
        val iconSize = (availableWidth / gridColumns.coerceAtLeast(4)).coerceAtMost(
            (72 * resources.displayMetrics.density).toInt()
        )
        val margin = (iconSize * 0.1f).toInt()
        
        // Create exactly plantsCount number of icons
        for (i in 0 until plantsCount) {
            val row = i / gridColumns
            val col = i % gridColumns
            
            val cardView = CardView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = iconSize
                    height = iconSize
                    setMargins(margin, margin, margin, margin)
                    rowSpec = GridLayout.spec(row, 1f)
                    columnSpec = GridLayout.spec(col, 1f)
                }
                radius = iconSize * 0.2f
                cardElevation = resources.displayMetrics.density * 4
                alpha = 0f
                setCardBackgroundColor(Color.WHITE)
            }
            
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (iconSize * 0.8f).toInt(),
                    (iconSize * 0.8f).toInt()
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
                setImageResource(iconResource)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(margin, margin, margin, margin)
            }
            
            cardView.addView(imageView)
            gridLayout.addView(cardView)
            
            // Animate each plant icon
            AnimatorSet().apply {
                val fadeIn = ObjectAnimator.ofFloat(cardView, View.ALPHA, 0f, 1f)
                val scaleX = ObjectAnimator.ofFloat(cardView, View.SCALE_X, 0.5f, 1.2f, 1f)
                val scaleY = ObjectAnimator.ofFloat(cardView, View.SCALE_Y, 0.5f, 1.2f, 1f)
                val rotation = ObjectAnimator.ofFloat(cardView, View.ROTATION, -10f, 10f, 0f)
                
                playTogether(fadeIn, scaleX, scaleY, rotation)
                duration = 600
                startDelay = (i * 50).toLong()
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }
    
    private fun getPlantIcon(plantType: String): Int {
        return when (plantType.toLowerCase()) {
            "cabbage" -> R.drawable.plant_icon_cabbage
            "radish" -> R.drawable.plant_icon_radish
            "carrot" -> R.drawable.plant_icon_carrot
            "tomato" -> R.drawable.plant_icon_tomato
            "capsicum" -> R.drawable.plant_icon_capsicum
            "brinjal" -> R.drawable.plant_icon_brinjal
            "okra" -> R.drawable.plant_icon_okra
            else -> R.drawable.plant_icon_carrot
        }
    }

    private fun setupContinueButton() {
        findViewById<Button>(R.id.continueButton)?.setOnClickListener {
            val intent = Intent(this, Report::class.java)
            // Pass all necessary data to Report activity
            intent.putExtra("AREA", intent.getFloatExtra("AREA", 0f))
            intent.putExtra("PLANT_TYPE", intent.getStringExtra("PLANT_TYPE") ?: "carrot")
            startActivity(intent)
            finish() // Optional: close this activity when moving to report
        }
    }

    private fun Double.ceil(): Double = kotlin.math.ceil(this)
} 