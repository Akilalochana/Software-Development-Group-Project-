package com.example.arlandmeasuretest33

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private val placedAnchors = ArrayList<Anchor>()
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val measurementNodes = ArrayList<Node>()
    private val points = ArrayList<Vector3>()
    private var measurementText: TextView? = null
    private var isDrawing = false

    // New properties for plant grid
    private var plantSpacing = 0.25f // Default spacing in meters
    private var plantType: String? = null
    private val plantNodes = ArrayList<Node>()
    private var isShowingPlants = false
    private var isShowingGrid = false
    private val gridNodes = ArrayList<Node>()
    private var gridPositions = ArrayList<Vector3>()
    private var instructionsText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get garden information from intent
        val gardenName = intent.getStringExtra("GARDEN_NAME") ?: ""
        val selectedDistrict = intent.getStringExtra("SELECTED_DISTRICT") ?: ""
        val plantType = intent.getStringExtra("PLANT_TYPE") ?: ""
        val plantImageUrl = intent.getStringExtra("PLANT_IMAGE_URL") ?: ""

        // Log all intent extras for debugging with more visibility
        Log.d("MainActivity", "======= INTENT EXTRAS RECEIVED =======")
        Log.d("MainActivity", "GARDEN_NAME = '$gardenName'")
        Log.d("MainActivity", "SELECTED_DISTRICT = '$selectedDistrict'")
        Log.d("MainActivity", "PLANT_TYPE = '$plantType'")
        Log.d("MainActivity", "PLANT_IMAGE_URL = '$plantImageUrl'")
        Log.d("MainActivity", "======================================")

        // Log all other extras if any
        for (key in intent.extras?.keySet() ?: emptySet()) {
            if (key !in listOf("GARDEN_NAME", "SELECTED_DISTRICT", "PLANT_TYPE", "PLANT_IMAGE_URL")) {
                Log.d("MainActivity", "   $key = ${intent.extras?.get(key)}")
            }
        }

        Log.d("MainActivity", "Will use in AR: District=$selectedDistrict, Plant=$plantType, Image URL=$plantImageUrl")

        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        measurementText = findViewById(R.id.measurementText)
        instructionsText = findViewById(R.id.instructionsText)

        setupUI()
        initializeARScene()

        // Set plant type from intent
        this.plantType = plantType

        // If you want to test the popup directly for debugging, uncomment this:
        // testPlantInfoPopup()

        if (intent.getBooleanExtra("START_AR", false)) {
            // Start your AR feature with the plantType
            // Add your AR initialization code here
        }
    }
    
    private fun testPlantInfoPopup() {
        // Set some test values
        plantType = intent.getStringExtra("PLANT_TYPE") ?: "carrot"
        
        // Create a few points for area calculation
        points.clear()
        points.add(Vector3(0f, 0f, 0f))
        points.add(Vector3(1f, 0f, 0f))
        points.add(Vector3(1f, 0f, 1f))
        points.add(Vector3(0f, 0f, 1f))
        
        // Show popup
        showPlantInfoPopup()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.clearButton)?.setOnClickListener {
            clearMeasurement()
        }

        findViewById<Button>(R.id.switchModeButton)?.setOnClickListener {
            navigateToReport()
        }

        updateGridButton()
    }

    private fun updateGridButton() {
        val gridButton = findViewById<Button>(R.id.btn_show_plants)
        
        if (isShowingGrid) {
            gridButton?.text = "Show Plants"
        } else if (isShowingPlants) {
            gridButton?.text = "Clear Plants"
        } else {
            gridButton?.text = "Green Grid"
        }
    }

    private fun navigateToReport() {
        // Get the area and perimeter from AR measurements
        val area = calculateQuadrilateralArea(points)
        val perimeter = calculatePerimeter(points)

        // Save measurements to Firestore
        saveMeasurementsToFirestore(area, perimeter)

        val intent = Intent(this, Report::class.java)

        // Pass AR measurements to Report activity
        intent.putExtra("AR_AREA", area)
        intent.putExtra("AR_PERIMETER", perimeter)
        intent.putExtra("PLANT_TYPE", "Carrot")
        intent.putExtra("SELECTED_DISTRICT", "Mannar")
        println("Dumidu")
        startActivity(intent)
    }
    private fun showPlantInfoPopup() {
        try {
            // Calculate measurements from current state
            val area = if (points.size == 4) calculateQuadrilateralArea(points) else 0f
            val plantCount = plantNodes.size
            
            // Get plant type directly from intent, with stronger fallbacks
            val plantTypeName = intent.getStringExtra("PLANT_TYPE") ?: "Unknown"
            val plantImageUrl = intent.getStringExtra("PLANT_IMAGE_URL")
            
            // Print all relevant information for debugging
            Log.d("PlantInfoPopup", """
                Data for popup:
                Plant Type: $plantTypeName
                Image URL: $plantImageUrl
                Plant Count: $plantCount
                Area: $area
            """.trimIndent())

            // Create dialog
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.plant_info_popup)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            // Set animation
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // Get references to views
            val plantImageView = dialog.findViewById<ImageView>(R.id.plantImageView)
            val plantTypeValue = dialog.findViewById<TextView>(R.id.plantTypeValue)
            val plantCountValue = dialog.findViewById<TextView>(R.id.plantCountValue)
            val areaValue = dialog.findViewById<TextView>(R.id.areaValue)
            val locationValue = dialog.findViewById<TextView>(R.id.locationValue)
            val locationIcon = dialog.findViewById<ImageView>(R.id.locationIcon)
            val locationLabel = dialog.findViewById<TextView>(R.id.locationLabel)
            val animationView = dialog.findViewById<LottieAnimationView>(R.id.plantAnimation)

            // Hide location section
            locationValue.visibility = View.GONE
            locationIcon.visibility = View.GONE
            locationLabel.visibility = View.GONE

            // Set text values immediately
            plantTypeValue.text = plantTypeName
            plantCountValue.text = plantCount.toString()
            areaValue.text = String.format("%.2f m²", area)
            
            // Load image directly from the intent URL
            if (!plantImageUrl.isNullOrEmpty()) {
                Log.d("PlantInfoPopup", "Loading image from URL: $plantImageUrl")
                
                try {
                    // Leave ImageView empty while loading
                    Glide.with(this)
                        .load(plantImageUrl)
                        .into(plantImageView)
                } catch (e: Exception) {
                    Log.e("PlantInfoPopup", "Error loading image: ${e.message}")
                    // Keep ImageView empty on error
                }
            } else {
                // If no image URL in intent, try to get from Firestore
                Log.d("PlantInfoPopup", "No image URL in intent, fetching from Firestore")
                
                val db = FirebaseFirestore.getInstance()
                
                // First try 'Ampara' district which has more complete data
                val districts = listOf("Ampara", "Colombo")
                var imageLoaded = false
                
                // Try to find the plant image in each district
                for (district in districts) {
                    if (imageLoaded) break
                    
                    Log.d("PlantInfoPopup", "Searching for plant in $district district")
                    
                    // Use the plantTypeName for lookup but make it case insensitive
                    db.collection("districts")
                        .document(district)
                        .collection("crops")
                        .get()
                        .addOnSuccessListener { documents ->
                            if (imageLoaded) return@addOnSuccessListener // Skip if already loaded
                            
                            Log.d("PlantInfoPopup", "Found ${documents.size()} plants in $district district")
                            
                            for (document in documents) {
                                Log.d("PlantInfoPopup", "Checking document: ${document.id}")
                                
                                // Case-insensitive comparison
                                if (document.id.equals(plantTypeName, ignoreCase = true)) {
                                    val imageUrl = document.getString("image")
                                    Log.d("PlantInfoPopup", "Found matching plant '${document.id}', image URL: $imageUrl")
                                    
                                    if (!imageUrl.isNullOrEmpty()) {
                                        imageLoaded = true // Mark as loaded to prevent duplicate loading
                                        
                                        Glide.with(this)
                                            .load(imageUrl)
                                            .into(plantImageView)
                                        
                                        Log.d("PlantInfoPopup", "Successfully loaded image for ${document.id}")
                                        return@addOnSuccessListener
                                    }
                                }
                            }
                            
                            // Special case handling if still not found
                            if (!imageLoaded) {
                                handleSpecialCasePlants(plantTypeName, plantImageView)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("PlantInfoPopup", "Error fetching plant data from $district: ${e.message}")
                            
                            // Try special case handling
                            if (!imageLoaded) {
                                handleSpecialCasePlants(plantTypeName, plantImageView)
                            }
                        }
                }
            }

            // Set plant-specific animation
            when (plantTypeName.lowercase()) {
                "carrot" -> animationView.setAnimation(R.raw.carrot_growing)
                "cabbage" -> animationView.setAnimation(R.raw.cabbage_growing)
                "onion" -> animationView.setAnimation(R.raw.plant_growing)
                "brinjal" -> animationView.setAnimation(R.raw.plant_growing)
                else -> animationView.setAnimation(R.raw.plant_growing)
            }

            // Button click listener
            dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PlantInfoPopup", "Error showing popup: ${e.message}")
            Toast.makeText(this, "Error showing plant information", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSpecialCasePlants(plantName: String, imageView: ImageView) {
        // Hardcoded URLs for known plants that may have image loading issues
        val specialPlantUrls = mapOf(
            "onion" to "https://plantix.net/en/library/assets/custom/crop-images/onion.jpeg",
            "brinjal" to "https://gourmetgarden.in/cdn/shop/products/Brinjal_Long_Purple_3_875d-466f-8625-3e8da2207e1d.jpg?v=1742037242",
            "potato" to "https://cdn.mos.cms.futurecdn.net/iC7HBvohbJqExqvbKcV3pP.jpg",
            "manioc" to "https://www.shutterstock.com/image-photo/cassava-root-vegetable-isolated-on-260nw-315185849.jpg"
        )
        
        // Case-insensitive lookup
        val url = specialPlantUrls.entries.firstOrNull { 
            plantName.equals(it.key, ignoreCase = true) 
        }?.value
        
        if (url != null) {
            Log.d("PlantInfoPopup", "Using hardcoded URL for $plantName: $url")
            
            Glide.with(imageView.context)
                .load(url)
                .into(imageView)
        } else {
            // If we don't have a special case for this plant, keep the ImageView empty
            Log.d("PlantInfoPopup", "No special handling for $plantName, leaving image empty")
        }
    }

    private fun initializeARScene() {
        arFragment?.setOnTapArPlaneListener { hitResult, _, _ ->
            if (!isDrawing) {
                handleQuadrilateralMeasurement(hitResult)
            }
        }
    }

    private fun handleQuadrilateralMeasurement(hitResult: HitResult) {
        if (points.size >= 4) {
            clearMeasurement()
            return
        }

        isDrawing = true
        placePoint(hitResult)

        if (points.size > 1) {
            drawLine(points[points.size - 2], points[points.size - 1])

            if (points.size == 4) {
                drawLine(points[3], points[0])
                calculateAndDisplayQuadrilateralArea()
            }
        }
        isDrawing = false
    }

    private fun placePoint(hitResult: HitResult) {
        try {
            val anchor = hitResult.createAnchor()
            placedAnchors.add(anchor)

            val anchorNode = AnchorNode(anchor).apply {
                setParent(arFragment?.arSceneView?.scene)
            }
            placedAnchorNodes.add(anchorNode)

            MaterialFactory.makeOpaqueWithColor(
                this,
                com.google.ar.sceneform.rendering.Color(Color.RED)
            )
                .thenAccept { material ->
                    val sphere = ShapeFactory.makeSphere(0.02f, Vector3.zero(), material)
                    TransformableNode(arFragment?.transformationSystem).apply {
                        renderable = sphere
                        setParent(anchorNode)
                    }
                }

            points.add(anchorNode.worldPosition)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawLine(from: Vector3, to: Vector3) {
        try {
            val difference = Vector3.subtract(to, from)
            val directionFromTopToBottom = difference.normalized()
            val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
            val distance = difference.length()

            MaterialFactory.makeOpaqueWithColor(
                this,
                com.google.ar.sceneform.rendering.Color(Color.BLUE)
            )
                .thenAccept { material ->
                    val cube = ShapeFactory.makeCube(
                        Vector3(0.01f, 0.01f, distance),
                        Vector3.zero(),
                        material
                    )
                    Node().apply {
                        setParent(arFragment?.arSceneView?.scene)
                        localPosition = Vector3.add(from, difference.scaled(0.5f))
                        localRotation = rotationFromAToB
                        renderable = cube
                        measurementNodes.add(this)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateAndDisplayQuadrilateralArea() {
        if (points.size == 4) {
            try {
                val area = calculateQuadrilateralArea(points)
                val perimeter = calculatePerimeter(points)
                measurementText?.text =
                    String.format("Area: %.2f m², Perimeter: %.2f m", area, perimeter)
                
                // Make measurement text visible when area is calculated
                measurementText?.visibility = View.VISIBLE

                // Remove automatic plant grid display
                // Plants will only be shown when the Show Plants button is clicked
            } catch (e: Exception) {
                e.printStackTrace()
                measurementText?.text = "Error calculating measurements"
                measurementText?.visibility = View.VISIBLE
            }
        }
    }

    private fun calculateQuadrilateralArea(points: List<Vector3>): Float {
        var area = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            area += points[i].x * points[j].z - points[j].x * points[i].z
        }
        return Math.abs(area / 2f)
    }

    private fun calculatePerimeter(points: List<Vector3>): Float {
        var perimeter = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            perimeter += Vector3.subtract(points[j], points[i]).length()
        }
        return perimeter
    }

    private fun showPlantGrid(area: Float) {
        try {
            // If grid is not showing, show it first
            if (!isShowingGrid && !isShowingPlants) {
                showGreenGrid()
                return
            }
            
            // If we're showing plants, just clear them
            if (isShowingPlants) {
                clearPlants()
                updateGridButton()
                return
            }
            
            // Clear existing plants first
            clearPlants()

            // Get plant type from intent
            plantType = intent.getStringExtra("PLANT_TYPE")?.lowercase() ?: "carrot"

            // Create plant renderable based on plant type
            createPlantRenderable(
                plantType ?: "default",
                0f, 0f, 0f, // These values are not used as we use gridPositions
                0, 0,       // These values are not used as we use gridPositions
                plantSpacing
            )

            // Remove the blue boundary lines (original measurement polygon)
            measurementNodes.forEach { node ->
                node.setParent(null)
            }
            measurementNodes.clear()
            
            // Remove the red marker points
            placedAnchorNodes.forEach { node ->
                node.setParent(null)
                node.anchor?.detach()
            }
            placedAnchorNodes.clear()
            placedAnchors.clear()
            
            // Hide the instructions text
            runOnUiThread {
                instructionsText?.visibility = View.GONE
            }

            isShowingPlants = true
            updateGridButton()

            // Make measurement text visible when plants are shown
            measurementText?.visibility = View.VISIBLE

            // Show popup with plant information
            showPlantInfoPopup()
        } catch (e: Exception) {
            println("Error in showPlantGrid: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showGreenGrid() {
        try {
            if (points.size != 4) {
                Toast.makeText(this, "Please define a valid area with 4 points first", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Clear any existing grid
            clearGrid()
            
            // Get plant type from intent
            plantType = intent.getStringExtra("PLANT_TYPE")?.lowercase() ?: "carrot"
            
            // Determine spacing based on plant type
            val spacing = when (plantType) {
                "cabbage" -> 0.5f  // 50cm spacing for cabbage
                "carrot" -> 0.2f   // 20cm spacing for carrot
                else -> 0.25f      // Default spacing
            }
            
            // Update plant spacing for use in later plant placement
            plantSpacing = spacing
            
            // Calculate the ground height based on points
            val groundY = points.map { it.y }.average().toFloat() + 0.005f // Slightly above ground
            
            // Find largest rectangle that fits inside the polygon
            val rectangle = findLargestRectangleInPolygon(points, spacing)
            
            if (rectangle == null) {
                Toast.makeText(this, "Could not find a suitable area for planting", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Extract rectangle dimensions
            val startX = rectangle.first.x
            val startZ = rectangle.first.z
            val width = rectangle.second.x
            val height = rectangle.second.z
            
            // Calculate how many grid cells can fit
            val cols = (width / spacing).toInt()
            val rows = (height / spacing).toInt()
            
            if (cols <= 0 || rows <= 0) {
                Toast.makeText(this, "Area too small for selected plant type", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Calculate actual width and height of the grid area
            val actualWidth = cols * spacing
            val actualHeight = rows * spacing
            
            // Calculate offset to center the grid within the available rectangle
            val offsetX = startX + (width - actualWidth) / 2
            val offsetZ = startZ + (height - actualHeight) / 2
            
            // Store grid positions for later use in plant placement
            val gridPositions = ArrayList<Vector3>()
            
            // Create grid cells
            val gridColor = com.google.ar.sceneform.rendering.Color(0.2f, 0.8f, 0.2f, 0.3f) // Transparent green
            
            MaterialFactory.makeTransparentWithColor(this, gridColor)
                .thenAccept { material ->
                    // Create a grid of square cells
                    var cellsPlaced = 0
                    
                    for (row in 0 until rows) {
                        for (col in 0 until cols) {
                            // Calculate center of cell
                            val x = offsetX + (col * spacing) + (spacing / 2)
                            val z = offsetZ + (row * spacing) + (spacing / 2)
                            
                            // Double-check that this cell's corners are inside the polygon
                            val halfSpacing = spacing / 2 * 0.95f // Slightly smaller for margin
                            if (isPointInside(x - halfSpacing, z - halfSpacing) && 
                                isPointInside(x + halfSpacing, z - halfSpacing) &&
                                isPointInside(x - halfSpacing, z + halfSpacing) &&
                                isPointInside(x + halfSpacing, z + halfSpacing)) {
                                
                                // Create a square for each grid cell - exactly the size of the plant spacing
                                val gridCell = ShapeFactory.makeCube(
                                    Vector3(spacing * 0.95f, 0.001f, spacing * 0.95f),  // Slightly smaller for visual separation
                                    Vector3.zero(),
                                    material
                                )
                                
                                val cellNode = Node()
                                cellNode.setParent(arFragment?.arSceneView?.scene)
                                cellNode.localPosition = Vector3(x, groundY, z)
                                cellNode.renderable = gridCell
                                
                                gridNodes.add(cellNode)
                                
                                // Store this position for later plant placement
                                gridPositions.add(Vector3(x, groundY, z))
                                cellsPlaced++
                            }
                        }
                    }
                    
                    // Store the grid positions for plant placement
                    this.gridPositions = gridPositions
                    
                    if (cellsPlaced == 0) {
                        Toast.makeText(this, "No valid grid cells found in the selected area", Toast.LENGTH_SHORT).show()
                        return@thenAccept
                    }
                    
                    // Remove the blue boundary lines (original measurement polygon)
                    measurementNodes.forEach { node ->
                        node.setParent(null)
                    }
                    measurementNodes.clear()
                    
                    // Remove the red marker points
                    placedAnchorNodes.forEach { node ->
                        node.setParent(null)
                        node.anchor?.detach()
                    }
                    placedAnchorNodes.clear()
                    placedAnchors.clear()
                    
                    // Hide the instructions text
                    runOnUiThread {
                        instructionsText?.visibility = View.GONE
                    }
                    
                    // Update button and state
                    isShowingGrid = true
                    updateGridButton()
                    
                    // Draw rectangle outline for visualization
                    drawRectangleOutline(rectangle.first, width, height, groundY)
                    
                    // Show measurement text
                    measurementText?.text = "Grid: $cellsPlaced cells (${cols}×${rows}), Plant spacing: ${String.format("%.2f", spacing)}m"
                    measurementText?.visibility = View.VISIBLE
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error creating grid: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error creating grid", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Draw outline of the rectangle for visualization
    private fun drawRectangleOutline(startPoint: Vector3, width: Float, height: Float, groundY: Float) {
        // Create a green outline material using MaterialFactory instead
        val outlineColor = com.google.ar.sceneform.rendering.Color(0.1f, 0.8f, 0.1f, 1.0f)
        
        MaterialFactory.makeOpaqueWithColor(this, outlineColor)
            .thenAccept { material ->
                // Create outline lines
                val lineThickness = 0.01f
                
                // Bottom line (startPoint to startPoint+width)
                createOutlineLine(
                    startPoint, 
                    Vector3(startPoint.x + width, groundY, startPoint.z),
                    lineThickness,
                    material
                )
                
                // Right line (startPoint+width to startPoint+width+height)
                createOutlineLine(
                    Vector3(startPoint.x + width, groundY, startPoint.z),
                    Vector3(startPoint.x + width, groundY, startPoint.z + height),
                    lineThickness,
                    material
                )
                
                // Top line (startPoint+width+height to startPoint+height)
                createOutlineLine(
                    Vector3(startPoint.x + width, groundY, startPoint.z + height),
                    Vector3(startPoint.x, groundY, startPoint.z + height),
                    lineThickness,
                    material
                )
                
                // Left line (startPoint+height to startPoint)
                createOutlineLine(
                    Vector3(startPoint.x, groundY, startPoint.z + height),
                    startPoint,
                    lineThickness,
                    material
                )
            }
    }
    
    private fun createOutlineLine(from: Vector3, to: Vector3, thickness: Float, material: com.google.ar.sceneform.rendering.Material) {
        val difference = Vector3.subtract(to, from)
        val directionFromTopToBottom = difference.normalized()
        val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
        val distance = difference.length()
        
        val cube = ShapeFactory.makeCube(
            Vector3(thickness, thickness, distance),
            Vector3.zero(),
            material
        )
        
        val lineNode = Node()
        lineNode.setParent(arFragment?.arSceneView?.scene)
        lineNode.localPosition = Vector3.add(from, difference.scaled(0.5f))
        lineNode.localRotation = rotationFromAToB
        lineNode.renderable = cube
        gridNodes.add(lineNode) // Add to grid nodes so it gets cleared with the grid
    }
    
    // Find the largest rectangle that can fit inside the polygon
    private fun findLargestRectangleInPolygon(points: List<Vector3>, spacing: Float): Pair<Vector3, Vector3>? {
        if (points.size < 3) return null
        
        // Define the search area: just use the bounding box of the points
        val minX = points.minOf { it.x }
        val minZ = points.minOf { it.z }
        val maxX = points.maxOf { it.x }
        val maxZ = points.maxOf { it.z }
        val groundY = points.map { it.y }.average().toFloat()
        
        // Calculate the center of the polygon
        val centerX = (minX + maxX) / 2
        val centerZ = (minZ + maxZ) / 2
        
        // Add a small inset to ensure we stay inside the boundary
        val boundaryInset = spacing * 0.25f // Inset by 25% of cell spacing
        val adjustedMinX = minX + boundaryInset
        val adjustedMinZ = minZ + boundaryInset
        val adjustedMaxX = maxX - boundaryInset
        val adjustedMaxZ = maxZ - boundaryInset
        
        // Check if the adjusted area is too small
        if (adjustedMaxX <= adjustedMinX || adjustedMaxZ <= adjustedMinZ) {
            return null
        }
        
        // Allow a maximum of 5000 iterations (should be sufficient for most cases)
        val maxIterations = 5000
        val minDimension = spacing * 2 // Minimum rectangle size (2x2 grid cells)
        
        var bestArea = 0f
        var bestRect: Pair<Vector3, Vector3>? = null
        var bestCenterDistance = Float.MAX_VALUE // Track how centered the rectangle is
        
        // Try different rectangles to find the best-fitting one
        var iterations = 0
        
        // Step size controls how many points we test - use plant spacing divided by 4 for finer control
        val stepSize = spacing / 4
        
        // Prefer to search from center outward for better centering
        val sortedXPoints = generateSequence(centerX) { prev -> 
            val next = prev + stepSize
            if (next <= adjustedMaxX) next else null 
        }.toList() + generateSequence(centerX - stepSize) { prev ->
            val next = prev - stepSize
            if (next >= adjustedMinX) next else null
        }
        
        val sortedZPoints = generateSequence(centerZ) { prev -> 
            val next = prev + stepSize
            if (next <= adjustedMaxZ) next else null 
        }.toList() + generateSequence(centerZ - stepSize) { prev ->
            val next = prev - stepSize
            if (next >= adjustedMinZ) next else null
        }
        
        for (startX in sortedXPoints) {
            if (iterations >= maxIterations) break
            
            for (startZ in sortedZPoints) {
                if (iterations >= maxIterations) break
                
                // Try to grow rectangle from this corner
                val startPoint = Vector3(startX, groundY, startZ)
                
                // Only proceed if the corner point is inside the polygon
                if (isPointInside(startPoint.x, startPoint.z)) {
                    // Try different symmetrical width/height combinations
                    var width = minDimension
                    while (startX + width / 2 <= adjustedMaxX && startX - width / 2 >= adjustedMinX && iterations < maxIterations) {
                        var height = minDimension
                        while (startZ + height / 2 <= adjustedMaxZ && startZ - height / 2 >= adjustedMinZ && iterations < maxIterations) {
                            iterations++
                            
                            // Recalculate actual start point for a centered rectangle
                            val actualStartX = startX - width / 2
                            val actualStartZ = startZ - height / 2
                            val actualStartPoint = Vector3(actualStartX, groundY, actualStartZ)
                            
                            // Check if all four corners and additional points are inside the polygon
                            if (isRectangleInside(actualStartPoint, width, height)) {
                                val area = width * height
                                
                                // Calculate how centered this rectangle is
                                val rectCenterX = actualStartX + width/2
                                val rectCenterZ = actualStartZ + height/2
                                val centerDistance = Math.abs(rectCenterX - centerX) + Math.abs(rectCenterZ - centerZ)
                                
                                // Prefer larger rectangles, but with a slight preference for more centered ones
                                if (area > bestArea * 0.95f) { // Allow up to 5% smaller if better centered
                                    if (area > bestArea || centerDistance < bestCenterDistance) {
                                        bestArea = area
                                        bestRect = Pair(actualStartPoint, Vector3(width, 0f, height))
                                        bestCenterDistance = centerDistance
                                    }
                                }
                            }
                            
                            height += stepSize * 2 // Increase by 2x step size to maintain symmetry
                        }
                        width += stepSize * 2 // Increase by 2x step size to maintain symmetry
                    }
                }
            }
        }
        
        // If no centered rectangle was found, fall back to original algorithm
        if (bestRect == null) {
            Log.d("MainActivity", "No centered rectangle found, falling back to original algorithm")
            bestArea = 0f
            iterations = 0
            
            var startX = adjustedMinX
            while (startX <= adjustedMaxX && iterations < maxIterations) {
                var startZ = adjustedMinZ
                while (startZ <= adjustedMaxZ && iterations < maxIterations) {
                    // Try to grow rectangle from this corner
                    val startPoint = Vector3(startX, groundY, startZ)
                    
                    // Only proceed if the corner point is inside the polygon
                    if (isPointInside(startPoint.x, startPoint.z)) {
                        // Try different width/height combinations
                        var width = minDimension
                        while (startX + width <= adjustedMaxX && iterations < maxIterations) {
                            var height = minDimension
                            while (startZ + height <= adjustedMaxZ && iterations < maxIterations) {
                                iterations++
                                
                                // Check if all four corners and additional points are inside the polygon
                                if (isRectangleInside(startPoint, width, height)) {
                                    val area = width * height
                                    if (area > bestArea) {
                                        bestArea = area
                                        bestRect = Pair(startPoint, Vector3(width, 0f, height))
                                    }
                                }
                                
                                height += stepSize
                            }
                            width += stepSize
                        }
                    }
                    startZ += stepSize
                }
                startX += stepSize
            }
        }
        
        return bestRect
    }
    
    // Check if a rectangle is completely inside the polygon
    private fun isRectangleInside(startPoint: Vector3, width: Float, height: Float): Boolean {
        // Check corners
        if (!isPointInside(startPoint.x, startPoint.z) ||
            !isPointInside(startPoint.x + width, startPoint.z) ||
            !isPointInside(startPoint.x, startPoint.z + height) ||
            !isPointInside(startPoint.x + width, startPoint.z + height)) {
            return false
        }
        
        // Add more check points along edges for better boundary detection
        // Check points along the top and bottom edges
        val numEdgeChecks = 4
        for (i in 1 until numEdgeChecks) {
            val ratio = i.toFloat() / numEdgeChecks
            // Top edge
            if (!isPointInside(startPoint.x + width * ratio, startPoint.z)) {
                return false
            }
            // Bottom edge
            if (!isPointInside(startPoint.x + width * ratio, startPoint.z + height)) {
                return false
            }
            // Left edge
            if (!isPointInside(startPoint.x, startPoint.z + height * ratio)) {
                return false
            }
            // Right edge
            if (!isPointInside(startPoint.x + width, startPoint.z + height * ratio)) {
                return false
            }
        }
        
        return true
    }

    private fun isPointInside(x: Float, z: Float): Boolean {
        if (points.size < 3) return false

        // Add a small epsilon to ensure the grid stays slightly inside the boundaries
        val epsilon = 0.01f

        // Ray casting algorithm to determine if point is inside polygon
        var inside = false
        val p = Vector3(x, points[0].y, z)  // Use consistent y-value

        for (i in points.indices) {
            val j = (i + 1) % points.size

            val pi = points[i]
            val pj = points[j]

            // Check if point is on an edge - consider it outside to keep grid fully inside
            if (distanceToLineSegment(pi, pj, p) < epsilon) {
                return false
            }

            // Check if ray from point crosses this edge
            if (((pi.z > p.z) != (pj.z > p.z)) &&
                (p.x < (pj.x - pi.x) * (p.z - pi.z) / (pj.z - pi.z) + pi.x)) {
                inside = !inside
            }
        }

        return inside
    }

    private fun distanceToLineSegment(a: Vector3, b: Vector3, p: Vector3): Float {
        val abx = b.x - a.x
        val abz = b.z - a.z
        val lengthSquared = abx * abx + abz * abz

        // If line segment is a point, return distance to that point
        if (lengthSquared == 0f) {
            return Vector3.subtract(p, a).length()
        }

        // Calculate projection of p onto line segment
        var t = ((p.x - a.x) * abx + (p.z - a.z) * abz) / lengthSquared
        t = t.coerceIn(0f, 1f)

        val projection = Vector3(
            a.x + t * abx,
            p.y,  // Keep the same y-value
            a.z + t * abz
        )

        return Vector3.subtract(p, projection).length()
    }

    private fun clearGrid() {
        gridNodes.forEach { node ->
            node.setParent(null)
        }
        gridNodes.clear()
        gridPositions.clear()
        isShowingGrid = false
    }

    private fun createPlantRenderable(
        plantType: String, startX: Float, startZ: Float, groundY: Float,
        rows: Int, cols: Int, spacing: Float
    ) {
        when (plantType) {
            "carrot" -> createCarrotRenderable(startX, startZ, groundY, rows, cols, spacing)
            "cabbage" -> createCabbageRenderable(startX, startZ, groundY, rows, cols, spacing)

            else -> createDefaultRenderable(startX, startZ, groundY, rows, cols, spacing)
        }
    }

    private fun createCarrotRenderable(
        startX: Float,
        startZ: Float,
        groundY: Float,
        rows: Int,
        cols: Int,
        spacing: Float
    ) {
        var plantsPlaced = 0

        // Enhanced color palette for more vibrant carrots
        val deepOrange = com.google.ar.sceneform.rendering.Color(1.0f, 0.4f, 0.0f)      // Deep carrot orange
        val lightGreen = com.google.ar.sceneform.rendering.Color(0.52f, 0.94f, 0.4f)    // Bright green for leaves
        val mediumGreen = com.google.ar.sceneform.rendering.Color(0.42f, 0.9f, 0.35f)   // Medium green for leaves
        val stemGreen = com.google.ar.sceneform.rendering.Color(0.48f, 0.82f, 0.3f)     // Stem color

        MaterialFactory.makeOpaqueWithColor(this, deepOrange)
            .thenAccept { orangeMaterial ->
                MaterialFactory.makeOpaqueWithColor(this, lightGreen)
                    .thenAccept { lightLeafMaterial ->
                        MaterialFactory.makeOpaqueWithColor(this, mediumGreen)
                            .thenAccept { mediumLeafMaterial ->
                                MaterialFactory.makeOpaqueWithColor(this, stemGreen)
                                    .thenAccept { stemMaterial ->
                                        // Instead of calculating a new grid, use the stored grid positions
                                        for (position in gridPositions) {
                                            // Create parent node at the exact grid cell position
                                            val plantNode = Node()
                                            plantNode.setParent(arFragment?.arSceneView?.scene)
                                            plantNode.localPosition = position

                                            // Apply small random rotation for natural appearance
                                            plantNode.localRotation = Quaternion.axisAngle(
                                                Vector3(0f, 1f, 0f),
                                                (Math.random() * 360).toFloat()
                                            )

                                            // Slight scale variation
                                            val scale = 0.85f + (Math.random() * 0.15f).toFloat()
                                            plantNode.localScale = Vector3(scale, scale, scale)

                                            // Create carrot parts
                                            val coreNode = Node()
                                            coreNode.setParent(plantNode)
                                            coreNode.localPosition = Vector3(0f, -0.02f, 0f)
                                            coreNode.renderable = ShapeFactory.makeCylinder(
                                                0.004f,
                                                0.08f,
                                                Vector3(0f, 0f, 0f),
                                                orangeMaterial
                                            )

                                            // Create layered carrot shape
                                            createCarrotLayers(plantNode, orangeMaterial)

                                            // Create stem
                                            val stemNode = Node()
                                            stemNode.setParent(plantNode)
                                            stemNode.localPosition = Vector3(0f, 0.025f, 0f)
                                            stemNode.renderable = ShapeFactory.makeCylinder(
                                                0.003f,
                                                0.015f,
                                                Vector3(0f, 0f, 0f),
                                                stemMaterial
                                            )

                                            // Create foliage
                                            createSpreadFoliage(plantNode, lightLeafMaterial, mediumLeafMaterial)

                                            plantNodes.add(plantNode)
                                            plantsPlaced++
                                        }

                                        runOnUiThread {
                                            measurementText?.text = "Carrots planted: $plantsPlaced"
                                        }
                                    }
                            }
                    }
            }
    }
    private fun createCarrotLayers(
        parentNode: Node,
        orangeMaterial: com.google.ar.sceneform.rendering.Material
    ) {
        // Create a more realistic carrot shape using stacked conical layers

        // Top layer (widest)
        val topLayer = ShapeFactory.makeCylinder(
            0.023f, // top radius
            0.012f, // height
            Vector3(0f, 0f, 0f),
            orangeMaterial
        )

        val topNode = Node()
        topNode.setParent(parentNode)
        topNode.localPosition = Vector3(0f, 0.015f, 0f)
        topNode.renderable = topLayer

        // Middle layer
        val middleLayer = ShapeFactory.makeCylinder(
            0.018f, // top radius
            0.02f, // height
            Vector3(0f, 0f, 0f),
            orangeMaterial
        )

        val middleNode = Node()
        middleNode.setParent(parentNode)
        middleNode.localPosition = Vector3(0f, -0.001f, 0f)
        middleNode.renderable = middleLayer

        // Lower layer
        val lowerLayer = ShapeFactory.makeCylinder(
            0.012f, // top radius
            0.025f, // height
            Vector3(0f, 0f, 0f),
            orangeMaterial
        )

        val lowerNode = Node()
        lowerNode.setParent(parentNode)
        lowerNode.localPosition = Vector3(0f, -0.023f, 0f)
        lowerNode.renderable = lowerLayer

        // Tip (pointy end)
        val tipLayer = ShapeFactory.makeCylinder(
            0.006f, // top radius
            0.015f, // height
            Vector3(0f, 0f, 0f),
            orangeMaterial
        )

        val tipNode = Node()
        tipNode.setParent(parentNode)
        tipNode.localPosition = Vector3(0f, -0.043f, 0f)
        tipNode.renderable = tipLayer
    }

    private fun createLeafCluster(
        parentNode: Node,
        leafCount: Int,
        lightLeafMaterial: com.google.ar.sceneform.rendering.Material,
        mediumLeafMaterial: com.google.ar.sceneform.rendering.Material
    ) {
        for (i in 0 until leafCount) {
            val leafNode = Node()
            leafNode.setParent(parentNode)

            // Spread leaves in a fan-like pattern
            val angle = (i * 25f - (leafCount * 12.5f) + (Math.random() * 10 - 5)).toFloat()

            // Position leaves slightly apart
            leafNode.localPosition = Vector3(
                (Math.random() * 0.01 - 0.005).toFloat(),
                i * 0.005f,  // Increased upward stacking for height
                (Math.random() * 0.01 - 0.005).toFloat()
            )

            // Rotate leaves to fan out with more upright orientation
            leafNode.localRotation = Quaternion.multiply(
                Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                Quaternion.axisAngle(Vector3(1f, 0f, 0f), (Math.random() * 10 - 20).toFloat()) // More upright angle
            )

            // Create leaf shape - longer and taller for carrot leaves
            val leafMaterial = if (Math.random() > 0.5) lightLeafMaterial else mediumLeafMaterial
            val leafLength = 0.035f + (Math.random() * 0.02f).toFloat() // Increased length
            val leafWidth = 0.004f + (Math.random() * 0.003f).toFloat()

            val leaf = ShapeFactory.makeCube(
                Vector3(leafWidth, 0.001f, leafLength),
                Vector3(0f, 0f, leafLength * 0.5f),
                leafMaterial
            )

            leafNode.renderable = leaf

            // Add smaller subleaves for some leaves
            if (Math.random() > 0.6) {
                val subLeafCount = 1 + (Math.random() * 2).toInt()
                for (j in 0 until subLeafCount) {
                    val subLeafNode = Node()
                    subLeafNode.setParent(leafNode)

                    // Position along main leaf
                    val leafOffset = 0.4f + (j * 0.25f) + (Math.random() * 0.1f).toFloat()
                    subLeafNode.localPosition = Vector3(
                        (Math.random() * 0.004 - 0.002).toFloat(),
                        0.001f,
                        leafLength * leafOffset
                    )

                    // Angle subleaf outward
                    val subLeafAngle = 35f + (Math.random() * 30).toFloat()
                    subLeafNode.localRotation = Quaternion.axisAngle(
                        Vector3(0f, 1f, 0f),
                        if (j % 2 == 0) subLeafAngle else -subLeafAngle
                    )

                    // Taller subleaves
                    val subLeafLength = leafLength * (0.5f + (Math.random() * 0.2f).toFloat())
                    val subLeafWidth = leafWidth * (0.7f + (Math.random() * 0.3f).toFloat())

                    val subLeaf = ShapeFactory.makeCube(
                        Vector3(subLeafWidth, 0.001f, subLeafLength),
                        Vector3(0f, 0f, subLeafLength * 0.4f),
                        leafMaterial
                    )

                    subLeafNode.renderable = subLeaf
                }
            }
        }
    }

    private fun createSpreadFoliage(
        parentNode: Node,
        lightLeafMaterial: com.google.ar.sceneform.rendering.Material,
        mediumLeafMaterial: com.google.ar.sceneform.rendering.Material
    ) {
        // Create a foliage cluster at the top of the carrot
        val foliageNode = Node()
        foliageNode.setParent(parentNode)
        foliageNode.localPosition = Vector3(0f, 0.035f, 0f)

        // Create 3-4 clusters of leaves in different directions
        val clusterCount = 3 + (Math.random() * 2).toInt()

        for (i in 0 until clusterCount) {
            val clusterNode = Node()
            clusterNode.setParent(foliageNode)

            // Spread clusters around the center at slightly different angles
            val angle = (i * (360f / clusterCount) + (Math.random() * 30 - 15)).toFloat()
            val tilt = 20f + (Math.random() * 30).toFloat() // More upright tilt

            clusterNode.localRotation = Quaternion.multiply(
                Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                Quaternion.axisAngle(Vector3(1f, 0f, 0f), tilt)
            )

            // Create a cluster of 4-6 leaves per direction (more leaves)
            val leafCount = 4 + (Math.random() * 3).toInt()
            createLeafCluster(clusterNode, leafCount, lightLeafMaterial, mediumLeafMaterial)
        }

        // Add more upright leaves at the center
        val centerCluster = Node()
        centerCluster.setParent(foliageNode)
        centerCluster.localRotation = Quaternion.axisAngle(
            Vector3(
                (Math.random() * 0.3 - 0.15).toFloat(),
                (Math.random() * 0.3 - 0.15).toFloat(),
                (Math.random() * 0.3 - 0.15).toFloat()
            ).normalized(),
            (Math.random() * 15 + 5).toFloat() // More upright central leaves
        )

        // Create 3-4 central leaves (increased)
        createLeafCluster(centerCluster, 3 + (Math.random() * 2).toInt(),
            lightLeafMaterial, mediumLeafMaterial)
    }

    private fun createCabbageRenderable(
        startX: Float,
        startZ: Float,
        groundY: Float,
        rows: Int,
        cols: Int,
        spacing: Float
    ) {
        var plantsPlaced = 0

        // Realistic cabbage color palette
        val darkGreen = com.google.ar.sceneform.rendering.Color(0.12f, 0.4f, 0.12f)
        val mediumGreen = com.google.ar.sceneform.rendering.Color(0.18f, 0.5f, 0.18f)
        val lightGreen = com.google.ar.sceneform.rendering.Color(0.25f, 0.65f, 0.2f)
        val stemColor = com.google.ar.sceneform.rendering.Color(0.16f, 0.42f, 0.1f)
        val innerColor = com.google.ar.sceneform.rendering.Color(0.6f, 0.8f, 0.6f)

        MaterialFactory.makeOpaqueWithColor(this, darkGreen)
            .thenAccept { darkMaterial ->
                MaterialFactory.makeOpaqueWithColor(this, mediumGreen)
                    .thenAccept { mediumMaterial ->
                        MaterialFactory.makeOpaqueWithColor(this, lightGreen)
                            .thenAccept { lightMaterial ->
                                MaterialFactory.makeOpaqueWithColor(this, stemColor)
                                    .thenAccept { stemMaterial ->
                                        MaterialFactory.makeOpaqueWithColor(this, innerColor)
                                            .thenAccept { innerMaterial ->
                                                // Use the stored grid positions instead of calculating a new grid
                                                for (position in gridPositions) {
                                                    // Create cabbage at the exact grid cell position
                                                    val cabbageNode = Node()
                                                    cabbageNode.setParent(arFragment?.arSceneView?.scene)
                                                    cabbageNode.localPosition = position

                                                    // Minimal rotation - cabbage heads up with small variation
                                                    cabbageNode.localRotation = Quaternion.axisAngle(
                                                        Vector3(0f, 1f, 0f),
                                                        (Math.random() * 15).toFloat()
                                                    )

                                                    // Consistent scale with minimal variation
                                                    val scale = 0.97f + (Math.random() * 0.06f).toFloat()
                                                    cabbageNode.localScale = Vector3(scale, scale, scale)

                                                    // Create stem
                                                    val stemNode = Node()
                                                    stemNode.setParent(cabbageNode)
                                                    stemNode.localPosition = Vector3(0f, -0.02f, 0f)
                                                    stemNode.renderable = ShapeFactory.makeCylinder(
                                                        0.015f,  // radius
                                                        0.06f,   // height
                                                        Vector3(0f, 0f, 0f),
                                                        stemMaterial
                                                    )

                                                    // Create core sphere (cabbage heart)
                                                    val coreNode = Node()
                                                    coreNode.setParent(cabbageNode)
                                                    coreNode.localPosition = Vector3(0f, 0.045f, 0f)
                                                    coreNode.renderable = ShapeFactory.makeSphere(
                                                        0.05f,  // radius
                                                        Vector3(0f, 0f, 0f),
                                                        innerMaterial
                                                    )

                                                    // Add cabbage leaves (layers)
                                                    // Layer 1 - Bottom outer leaves
                                                    for (i in 0 until 8) {
                                                        createCabbageLeaf(cabbageNode, i * 45f, 0.09f, -0.01f, 10f, darkMaterial)
                                                    }

                                                    // Layer 2 - Mid-outer leaves
                                                    for (i in 0 until 8) {
                                                        createCabbageLeaf(cabbageNode, i * 45f + 22.5f, 0.07f, 0.015f, 20f, darkMaterial)
                                                    }

                                                    // Layer 3 - Middle leaves
                                                    for (i in 0 until 8) {
                                                        createCabbageLeaf(cabbageNode, i * 45f, 0.05f, 0.04f, 30f, mediumMaterial)
                                                    }

                                                    // Layer 4 - Inner leaves
                                                    for (i in 0 until 6) {
                                                        createCabbageLeaf(cabbageNode, i * 60f + 30f, 0.035f, 0.065f, 45f, mediumMaterial)
                                                    }

                                                    // Layer 5 - Top inner leaves
                                                    for (i in 0 until 4) {
                                                        createCabbageLeaf(cabbageNode, i * 90f, 0.02f, 0.085f, 60f, lightMaterial)
                                                    }

                                                    plantNodes.add(cabbageNode)
                                                    plantsPlaced++
                                                }

                                                runOnUiThread {
                                                    measurementText?.text = "Cabbages planted: $plantsPlaced"
                                                }
                                            }
                                    }
                            }
                    }
            }
    }
    
    // Helper method to create cabbage leaf
    private fun createCabbageLeaf(parent: Node, angle: Float, radius: Float, height: Float, tilt: Float, material: com.google.ar.sceneform.rendering.Material) {
        val leafRotation = Quaternion.multiply(
            Quaternion.axisAngle(Vector3(1f, 0f, 0f), tilt + (Math.random() * 3).toFloat()),
            Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
        )

        val randomRadius = radius + (Math.random() * 0.006f).toFloat()
        val randomHeight = height + (Math.random() * 0.006f).toFloat()

        // Create leaf
        val leafNode = Node()
        leafNode.setParent(parent)
        leafNode.localPosition = Vector3(
            randomRadius * Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
            randomHeight,
            randomRadius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        leafNode.localRotation = leafRotation

        // Size based on layer (parameters)
        val leafWidth = when {
            radius > 0.08f -> 0.08f
            radius > 0.06f -> 0.07f
            radius > 0.04f -> 0.06f
            radius > 0.02f -> 0.05f
            else -> 0.04f
        }
        
        val leafLength = when {
            radius > 0.08f -> 0.06f
            radius > 0.06f -> 0.055f
            radius > 0.04f -> 0.05f
            radius > 0.02f -> 0.04f
            else -> 0.03f
        }

        leafNode.renderable = ShapeFactory.makeCube(
            Vector3(leafWidth, 0.01f + (Math.random() * 0.006f).toFloat(), leafLength),
            Vector3(0f, 0f, 0f),
            material
        )
    }

    private fun createDefaultRenderable(
        startX: Float,
        startZ: Float,
        groundY: Float,
        rows: Int,
        cols: Int,
        spacing: Float
    ) {
        var plantsPlaced = 0

        // Enhanced natural color palette
        val darkGreen = com.google.ar.sceneform.rendering.Color(0.0f, 0.35f, 0.0f)
        val mediumGreen = com.google.ar.sceneform.rendering.Color(0.1f, 0.55f, 0.1f)
        val lightGreen = com.google.ar.sceneform.rendering.Color(0.2f, 0.65f, 0.2f)
        val stemBrown = com.google.ar.sceneform.rendering.Color(0.35f, 0.25f, 0.1f)

        MaterialFactory.makeOpaqueWithColor(this, darkGreen)
            .thenAccept { darkMaterial ->
                MaterialFactory.makeOpaqueWithColor(this, mediumGreen)
                    .thenAccept { mediumMaterial ->
                        MaterialFactory.makeOpaqueWithColor(this, lightGreen)
                            .thenAccept { lightMaterial ->
                                MaterialFactory.makeOpaqueWithColor(this, stemBrown)
                                    .thenAccept { stemMaterial ->
                                        // Use stored grid positions instead of calculating new ones
                                        for (position in gridPositions) {
                                            // Create plant at exact grid cell position
                                            val plantNode = Node()
                                            plantNode.setParent(arFragment?.arSceneView?.scene)
                                            plantNode.localPosition = position

                                            // Create stem
                                            createDetailedStem(plantNode, stemMaterial)

                                            // Add foliage
                                            createLayeredFoliage(
                                                plantNode,
                                                6, // Consistent leaf count
                                                darkMaterial,
                                                mediumMaterial,
                                                lightMaterial
                                            )

                                            plantNodes.add(plantNode)
                                            plantsPlaced++
                                        }

                                        runOnUiThread {
                                            measurementText?.text = "Plants placed: $plantsPlaced"
                                        }
                                    }
                            }
                    }
            }
    }

    private fun createDetailedStem(
        parentNode: Node,
        stemMaterial: com.google.ar.sceneform.rendering.Material
    ) {
        // Fixed dimensions for consistent appearance
        val stemHeight = 0.17f
        val stemRadius = 0.006f

        // Create main stem with consistent dimensions
        val mainStem = ShapeFactory.makeCylinder(
            stemRadius,
            stemHeight,
            Vector3(0f, stemHeight/2, 0f),
            stemMaterial
        )

        val stemNode = Node()
        stemNode.setParent(parentNode)

        // Minimal tilt for uniform upright appearance
        val tiltX = (Math.random() * 4 - 2).toFloat()
        val tiltZ = (Math.random() * 4 - 2).toFloat()
        stemNode.localRotation = Quaternion.multiply(
            Quaternion.axisAngle(Vector3(1f, 0f, 0f), tiltX),
            Quaternion.axisAngle(Vector3(0f, 0f, 1f), tiltZ)
        )

        stemNode.renderable = mainStem

        // Add exactly 2 side branches for uniformity
        for (i in 0 until 2) {
            val branchNode = Node()
            branchNode.setParent(stemNode)

            // Consistent branch positions
            val heightPosition = 0.5f + (i * 0.2f)
            branchNode.localPosition = Vector3(0f, stemHeight * heightPosition, 0f)

            // Consistent branch angles - opposite sides
            val branchAngle = i * 180f
            val branchTilt = 35f
            branchNode.localRotation = Quaternion.multiply(
                Quaternion.axisAngle(Vector3(0f, 1f, 0f), branchAngle),
                Quaternion.axisAngle(Vector3(1f, 0f, 0f), branchTilt)
            )

            // Fixed branch dimensions
            val branchLength = stemHeight * 0.35f
            val branchRadius = stemRadius * 0.7f

            val branch = ShapeFactory.makeCylinder(
                branchRadius,
                branchLength,
                Vector3(0f, branchLength/2, 0f),
                stemMaterial
            )

            branchNode.renderable = branch
        }
    }

    private fun createCurvedLeafShape(
        length: Float,
        width: Float,
        thickness: Float,
        material: com.google.ar.sceneform.rendering.Material
    ): Renderable {
        // Basic leaf shape with slight curvature
        val curveFactor = length * 0.15f

        return ShapeFactory.makeCube(
            Vector3(width, thickness, length),
            Vector3(0f, curveFactor, length * 0.3f),
            material
        )
    }

    private fun createLayeredFoliage(
        parentNode: Node,
        leafCount: Int,
        darkMaterial: com.google.ar.sceneform.rendering.Material,
        mediumMaterial: com.google.ar.sceneform.rendering.Material,
        lightMaterial: com.google.ar.sceneform.rendering.Material
    ) {
        // Create main foliage cluster at top of stem with fixed position
        val foliageNode = Node()
        foliageNode.setParent(parentNode)
        foliageNode.localPosition = Vector3(0f, 0.14f, 0f)

        // Fixed number of clusters for uniformity
        val clusterCount = 3
        val clusterSpacing = 0.018f

        // Organized leaf distribution with even angles
        for (c in 0 until clusterCount) {
            val clusterHeight = clusterSpacing * c
            val clusterNode = Node()
            clusterNode.setParent(foliageNode)
            clusterNode.localPosition = Vector3(0f, clusterHeight, 0f)

            // Evenly distribute clusters rotationally
            clusterNode.localRotation = Quaternion.axisAngle(
                Vector3(0f, 1f, 0f),
                c * (120f)
            )

            // Fixed leaf count per cluster
            val leavesPerCluster = 4
            for (i in 0 until leavesPerCluster) {
                // Alternate materials in a predictable pattern
                val material = when (i % 3) {
                    0 -> lightMaterial
                    1 -> mediumMaterial
                    else -> darkMaterial
                }

                createSmallerAestheticLeaf(
                    clusterNode,
                    i,
                    leavesPerCluster,
                    material
                )
            }
        }

        // Add exactly 3 evenly distributed lower leaves along stem
        for (i in 0 until 3) {
            val leafNode = Node()
            leafNode.setParent(parentNode)

            // Even spacing along stem
            val heightRatio = 0.3f + (i * 0.16f)
            leafNode.localPosition = Vector3(0f, 0.15f * heightRatio, 0f)

            // Evenly distributed orientation
            leafNode.localRotation = Quaternion.axisAngle(
                Vector3(0f, 1f, 0f),
                i * 120f
            )

            // Create smaller compound leaf
            createSmallerCompoundLeaf(leafNode, darkMaterial, mediumMaterial)
        }
    }

    private fun createSmallerAestheticLeaf(
        parentNode: Node,
        index: Int,
        totalLeaves: Int,
        material: com.google.ar.sceneform.rendering.Material
    ) {
        val leafNode = Node()
        leafNode.setParent(parentNode)

        // Position leaves in a precise semi-circle pattern
        val angleStep = 360f / totalLeaves
        val finalAngle = index * angleStep

        // Fixed tilt angles for uniformity
        val upwardTilt = 25f
        val outwardTilt = 15f

        // Precise rotation for uniform arrangement
        leafNode.localRotation = Quaternion.multiply(
            Quaternion.axisAngle(Vector3(0f, 1f, 0f), finalAngle),
            Quaternion.multiply(
                Quaternion.axisAngle(Vector3(1f, 0f, 0f), upwardTilt),
                Quaternion.axisAngle(Vector3(0f, 0f, 1f), outwardTilt)
            )
        )

        // Smaller, uniform leaf dimensions
        val leafLength = 0.05f
        val leafWidth = 0.02f
        val leafThickness = 0.002f

        // Create smaller leaf with consistent shape
        val leafShape = createCurvedLeafShape(leafLength, leafWidth, leafThickness, material)
        leafNode.renderable = leafShape

        // Add simplified vein detail
        addSimpleLeafDetails(leafNode, leafLength, leafWidth, material)
    }

    private fun addSimpleLeafDetails(
        leafNode: Node,
        leafLength: Float,
        leafWidth: Float,
        material: com.google.ar.sceneform.rendering.Material
    ) {
        // Add a single central vein
        val veinNode = Node()
        veinNode.setParent(leafNode)
        veinNode.localPosition = Vector3(0f, 0.001f, 0f)

        // Create a thin line as the central vein
        val vein = ShapeFactory.makeCube(
            Vector3(0.001f, 0.0005f, leafLength * 0.9f),
            Vector3(0f, 0f, leafLength * 0.3f),
            material
        )
        veinNode.renderable = vein

        // Add exactly 2 lateral veins on each side
        for (i in 0 until 2) {
            for (side in arrayOf(-1, 1)) {
                val lateralVeinNode = Node()
                lateralVeinNode.setParent(leafNode)

                // Fixed positions for consistent appearance
                val positionRatio = 0.3f + (i * 0.3f)
                lateralVeinNode.localPosition = Vector3(
                    0f,
                    0.001f,
                    leafLength * (positionRatio - 0.15f)
                )

                // Fixed angle for uniform appearance
                val angle = 30f
                lateralVeinNode.localRotation = Quaternion.axisAngle(
                    Vector3(0f, 1f, 0f),
                    angle * side
                )

                // Smaller, consistent vein size
                val lateralVeinLength = leafWidth * 0.6f
                val lateralVein = ShapeFactory.makeCube(
                    Vector3(0.001f, 0.0003f, lateralVeinLength),
                    Vector3(0f, 0f, lateralVeinLength * 0.5f),
                    material
                )
                lateralVeinNode.renderable = lateralVein
            }
        }
    }

    private fun createSmallerCompoundLeaf(
        parentNode: Node,
        darkMaterial: com.google.ar.sceneform.rendering.Material,
        mediumMaterial: com.google.ar.sceneform.rendering.Material
    ) {
        // Create thinner central stem for compound leaf
        val stemLength = 0.05f
        val stem = ShapeFactory.makeCube(
            Vector3(0.002f, 0.002f, stemLength),
            Vector3(0f, 0f, stemLength * 0.5f),
            darkMaterial
        )

        val stemNode = Node()
        stemNode.setParent(parentNode)
        stemNode.renderable = stem

        // Create exactly 3 pairs of leaflets along the stem
        for (i in 0 until 3) {
            val positionRatio = 0.3f + (i * 0.3f)

            // Left leaflet
            createSmallLeaflet(
                stemNode,
                positionRatio,
                -1,  // left side
                stemLength,
                mediumMaterial
            )

            // Right leaflet
            createSmallLeaflet(
                stemNode,
                positionRatio,
                1,   // right side
                stemLength,
                mediumMaterial
            )
        }

        // Terminal leaflet at end
        createSmallLeaflet(
            stemNode,
            1.0f,    // end position
            0,       // center
            stemLength,
            darkMaterial
        )
    }

    private fun createSmallLeaflet(
        parentNode: Node,
        positionRatio: Float,
        side: Int,  // -1 for left, 0 for center, 1 for right
        stemLength: Float,
        material: com.google.ar.sceneform.rendering.Material
    ) {
        val leafletNode = Node()
        leafletNode.setParent(parentNode)

        // Position along stem
        leafletNode.localPosition = Vector3(
            0f,
            0.001f,
            stemLength * positionRatio
        )

        // Fixed angles based on side
        val baseAngle = when(side) {
            -1 -> -40f    // left side
            1 -> 40f      // right side
            else -> 0f    // terminal leaflet
        }

        // Fixed upward tilt
        val upTilt = 25f

        leafletNode.localRotation = Quaternion.multiply(
            Quaternion.axisAngle(Vector3(0f, 1f, 0f), baseAngle),
            Quaternion.axisAngle(Vector3(1f, 0f, 0f), -upTilt)
        )

        // Create smaller uniform leaflets
        val leafletLength = 0.02f
        val leafletWidth = 0.01f

        val leaflet = ShapeFactory.makeCube(
            Vector3(leafletWidth, 0.001f, leafletLength),
            Vector3(0f, 0f, leafletLength * 0.4f),
            material
        )

        leafletNode.renderable = leaflet
    }

    private fun clearPlants() {
        plantNodes.forEach { node ->
            node.setParent(null)
        }
        plantNodes.clear()
        isShowingPlants = false
    }

    private fun clearMeasurement() {
        try {
            placedAnchors.forEach { it.detach() }
            placedAnchors.clear()

            placedAnchorNodes.forEach { node ->
                node.anchor?.detach()
                node.setParent(null)
            }
            placedAnchorNodes.clear()

            measurementNodes.forEach { node ->
                node.setParent(null)
            }
            measurementNodes.clear()

            // Clear plants and grid
            clearPlants()
            clearGrid()

            points.clear()
            measurementText?.text = ""
            
            // Hide measurement text when clearing
            measurementText?.visibility = View.GONE
            
            // Show the instructions text again
            instructionsText?.visibility = View.VISIBLE
            
            // Update button
            updateGridButton()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun saveMeasurementsToFirestore(area: Float, perimeter: Float) {
        // Get current user ID
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("MainActivity", "User not authenticated")
            Toast.makeText(this, "User not authenticated, measurements not saved", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        // Get garden name from SharedPreferences instead of intent
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val gardenName = sharedPreferences.getString("GARDEN_NAME", "") ?: ""

        if (gardenName.isBlank()) {
            Log.e("MainActivity", "Garden name is blank, cannot save measurements")
            Toast.makeText(this, "Garden name not found, measurements not saved", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity", "Using garden name from SharedPreferences: '$gardenName'")

        // Get Firestore reference
        val db = FirebaseFirestore.getInstance()

        // Reference the garden document directly
        val gardenDocRef = db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(gardenName)

        // Update the garden document with measurements
        val updates = hashMapOf<String, Any>(
            "area" to area,
            "perimeter" to perimeter,
            "measuredAt" to System.currentTimeMillis()
        )

        gardenDocRef.update(updates)
            .addOnSuccessListener {
                Log.d("MainActivity", "Measurements saved to Firestore for garden: $gardenName")
                Toast.makeText(this, "Garden measurements saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error saving measurements", e)
                Toast.makeText(this, "Error saving measurements: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun onShowPlantsClicked(view: View) {
        if (points.size == 4) {
            val area = calculateQuadrilateralArea(points)
            showPlantGrid(area)
        } else {
            Toast.makeText(this, "Please define a valid area with 4 points first", Toast.LENGTH_SHORT).show()
        }
    }

    // Add override for onBackPressed to fix navigation flow
    override fun onBackPressed() {
        // Check if we came from PricePredictionActivity
        val plantType = intent.getStringExtra("PLANT_TYPE") ?: ""
        val startAR = intent.getBooleanExtra("START_AR", false)
        
        if (startAR && plantType.isNotEmpty()) {
            // We came from PricePredictionActivity, go back there
            val intent = Intent(this, PricePredictionActivity::class.java)
            intent.putExtra("PLANT_TYPE", plantType)
            startActivity(intent)
            finish()
        } else {
            // Default back behavior
            super.onBackPressed()
        }
    }

}