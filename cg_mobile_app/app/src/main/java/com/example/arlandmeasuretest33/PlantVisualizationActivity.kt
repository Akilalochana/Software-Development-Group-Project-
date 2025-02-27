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
        val border = GradientDrawable()
        border.shape = GradientDrawable.RECTANGLE
        border.setStroke(4, Color.parseColor("#4CAF50")) // Green border
        border.setColor(Color.parseColor("#F1F8E9")) // Light green background
        border.cornerRadius = resources.displayMetrics.density * 12
        gardenContainer.background = border

        // Animate garden border appearance
        gardenContainer.alpha = 0f
        gardenContainer.scaleX = 0.8f
        gardenContainer.scaleY = 0.8f

        val fadeIn = ObjectAnimator.ofFloat(gardenContainer, View.ALPHA, 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(gardenContainer, View.SCALE_X, 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(gardenContainer, View.SCALE_Y, 0.8f, 1f)

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            duration = 500
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
        val plantsCount = (area / 0.25).toInt()
        val gridSize = sqrt(plantsCount.toDouble()).toInt()
        
        gridLayout.rowCount = gridSize
        gridLayout.columnCount = gridSize
        
        val iconResource = getPlantIcon(plantType)
        val iconSize = (resources.displayMetrics.density * 48).toInt()
        val margin = (resources.displayMetrics.density * 4).toInt()
        
        for (i in 0 until gridSize * gridSize) {
            val cardView = CardView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = iconSize + margin * 2
                    height = iconSize + margin * 2
                    setMargins(margin, margin, margin, margin)
                }
                radius = (resources.displayMetrics.density * 8)
                cardElevation = resources.displayMetrics.density * 2
                alpha = 0f
                setCardBackgroundColor(Color.WHITE)
            }
            
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = android.view.Gravity.CENTER
                }
                setImageResource(iconResource)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(margin, margin, margin, margin)
            }
            
            cardView.addView(imageView)
            gridLayout.addView(cardView)
            
            // Complex animation sequence
            AnimatorSet().apply {
                val fadeIn = ObjectAnimator.ofFloat(cardView, View.ALPHA, 0f, 1f)
                val scaleX = ObjectAnimator.ofFloat(cardView, View.SCALE_X, 0.5f, 1.1f, 1f)
                val scaleY = ObjectAnimator.ofFloat(cardView, View.SCALE_Y, 0.5f, 1.1f, 1f)
                val rotation = ObjectAnimator.ofFloat(cardView, View.ROTATION, -15f, 15f, 0f)
                
                playTogether(fadeIn, scaleX, scaleY, rotation)
                duration = 800
                startDelay = (i * 100).toLong()
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
} 