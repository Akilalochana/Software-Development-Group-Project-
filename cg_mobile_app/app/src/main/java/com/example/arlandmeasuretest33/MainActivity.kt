package com.example.arlandmeasuretest33

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import com.google.ar.sceneform.rendering.ModelRenderable

class
MainActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get garden information from intent
        val gardenName = intent.getStringExtra("GARDEN_NAME") ?: ""
        val selectedDistrict = intent.getStringExtra("SELECTED_DISTRICT") ?: ""

        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        measurementText = findViewById(R.id.measurementText)

        setupUI()
        initializeARScene()

        if (intent.getBooleanExtra("START_AR", false)) {
            val plantType = intent.getStringExtra("PLANT_TYPE")
            // Start your AR feature with the plantType
            // Add your AR initialization code here
        }
    }

    private fun setupUI() {
        findViewById<Button>(R.id.clearButton)?.setOnClickListener {
            clearMeasurement()
        }

        findViewById<Button>(R.id.switchModeButton)?.setOnClickListener {
            navigateToReport()
        }

        findViewById<Button>(R.id.showPlantsButton)?.setOnClickListener {
            if (points.size == 4) {
                val area = calculateQuadrilateralArea(points)
                showPlantGrid(area)
            }
        }
    }

    private fun navigateToReport() {
        val intent = Intent(this, Report::class.java)

        // Get the area and perimeter from AR measurements
        val area = calculateQuadrilateralArea(points)
        val perimeter = calculatePerimeter(points)

        // Pass AR measurements to Report activity
        intent.putExtra("AR_AREA", area)
        intent.putExtra("AR_PERIMETER", perimeter)
        intent.putExtra("PLANT_TYPE", "Carrot")
        intent.putExtra("SELECTED_DISTRICT", "Mannar")

        startActivity(intent)
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

            MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(Color.RED))
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

            MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(Color.BLUE))
                .thenAccept { material ->
                    val cube = ShapeFactory.makeCube(Vector3(0.01f, 0.01f, distance), Vector3.zero(), material)
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
                measurementText?.text = String.format("Area: %.2f m², Perimeter: %.2f m", area, perimeter)
                
                // After measuring, show the plant grid
                if (!isShowingPlants) {
                    showPlantGrid(area)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                measurementText?.text = "Error calculating measurements"
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
            // Clear existing plants first
            clearPlants()
            
            // Calculate number of plants that can fit
            val numPlants = (area / plantSpacing).toInt().coerceAtLeast(1) // Ensure at least 1 plant
            
            // Calculate grid dimensions to cover the entire area
            val boundingWidth = points.maxOf { it.x } - points.minOf { it.x }
            val boundingHeight = points.maxOf { it.z } - points.minOf { it.z }
            
            val cols = (boundingWidth / plantSpacing).toInt().coerceAtLeast(1)
            val rows = (boundingHeight / plantSpacing).toInt().coerceAtLeast(1)
            
            // Calculate starting point (use minimum x,z as starting point)
            val startX = points.minOf { it.x }
            val startZ = points.minOf { it.z }
            val groundY = points.map { it.y }.average().toFloat()

            // Load the plant 3D model
            loadPlantModel(startX, startZ, groundY, rows, cols)
            
        } catch (e: Exception) {
            println("Error in showPlantGrid: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadPlantModel(startX: Float, startZ: Float, groundY: Float, rows: Int, cols: Int) {
        try {
            println("Attempting to load 3D model from assets")
            
            ModelRenderable.builder()
                .setSource(this, java.util.concurrent.Callable {
                    assets.open("models/plant.glb")
                })
                .build()
                .thenAccept { renderable ->
                    println("Successfully loaded 3D model")
                    val plantsPlaced = createPlantGrid(renderable, startX, startZ, groundY, rows, cols, plantSpacing)
                    isShowingPlants = true
                    // Update measurement text to include plant count
                    runOnUiThread {
                        measurementText?.text = "${measurementText?.text}\nPlants placed: $plantsPlaced"
                    }
                }
                .exceptionally { throwable ->
                    println("Error loading 3D model: ${throwable.message}")
                    throwable.printStackTrace()
                    // Try alternative loading method if first method fails
                    tryAlternativeModelLoading(startX, startZ, groundY, rows, cols)
                    null
                }
        } catch (e: Exception) {
            println("Exception in loadPlantModel: ${e.message}")
            e.printStackTrace()
            tryAlternativeModelLoading(startX, startZ, groundY, rows, cols)
        }
    }

    private fun tryAlternativeModelLoading(startX: Float, startZ: Float, groundY: Float, rows: Int, cols: Int) {
        try {
            println("Attempting alternative model loading method")
            val modelUri = Uri.parse("file:///android_asset/models/plant.glb")
            ModelRenderable.builder()
                .setSource(this, modelUri)
                .build()
                .thenAccept { renderable ->
                    println("Successfully loaded 3D model using alternative method")
                    val plantsPlaced = createPlantGrid(renderable, startX, startZ, groundY, rows, cols, plantSpacing)
                    isShowingPlants = true
                    runOnUiThread {
                        measurementText?.text = "${measurementText?.text}\nPlants placed: $plantsPlaced"
                    }
                }
                .exceptionally { throwable ->
                    println("Error in alternative loading: ${throwable.message}")
                    throwable.printStackTrace()
                    runOnUiThread {
                        createFallbackPlantModel(startX, startZ, groundY, rows, cols)
                    }
                    null
                }
        } catch (e: Exception) {
            println("Exception in alternative loading: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                createFallbackPlantModel(startX, startZ, groundY, rows, cols)
            }
        }
    }

    private fun createFallbackPlantModel(startX: Float, startZ: Float, groundY: Float, rows: Int, cols: Int) {
        try {
            // Get plant type from intent, default to "carrot" if not specified
            val currentPlantType = intent.getStringExtra("PLANT_TYPE")?.toLowerCase() ?: "carrot"

            // Define plant characteristics based on type
            val plantCharacteristics = when (currentPlantType) {
                "carrot" -> PlantStyle(
                    height = 0.15f,
                    stemRadius = 0.006f,
                    leafSize = 0.03f,  // Smaller, more delicate leaves
                    leafColor = com.google.ar.sceneform.rendering.Color(0.1f, 0.6f, 0.15f),  // Slightly darker green
                    hasFlower = false
                )
                "cabbage" -> PlantStyle(
                    height = 0.12f,
                    stemRadius = 0.008f,
                    leafSize = 0.06f,
                    leafColor = com.google.ar.sceneform.rendering.Color(0.15f, 0.55f, 0.15f),
                    hasFlower = false
                )
                "tomato" -> PlantStyle(
                    height = 0.3f,  // Slightly taller
                    stemRadius = 0.008f,
                    leafSize = 0.04f,  // Slightly smaller leaves for better proportion
                    leafColor = com.google.ar.sceneform.rendering.Color(0.15f, 0.55f, 0.15f),
                    hasFlower = true,
                    flowerColor = com.google.ar.sceneform.rendering.Color(0.9f, 0.1f, 0.1f)
                )
                "brinjal" -> PlantStyle(
                    height = 0.2f,
                    stemRadius = 0.009f,
                    leafSize = 0.06f,
                    leafColor = com.google.ar.sceneform.rendering.Color(0.12f, 0.5f, 0.12f),
                    hasFlower = true,
                    flowerColor = com.google.ar.sceneform.rendering.Color(0.6f, 0.3f, 0.8f)
                )
                else -> PlantStyle() // Default style
            }

            // Create materials with natural colors
            val stemMaterial = MaterialFactory.makeOpaqueWithColor(this, 
                com.google.ar.sceneform.rendering.Color(0.15f, 0.4f, 0.15f))
            val leafMaterial = MaterialFactory.makeOpaqueWithColor(this, plantCharacteristics.leafColor)
            val flowerMaterial = if (plantCharacteristics.hasFlower) {
                MaterialFactory.makeOpaqueWithColor(this, plantCharacteristics.flowerColor)
            } else null

            class PlantNode : Node() {
                val parts = mutableListOf<Node>()
                
                fun addPart(renderable: Renderable, position: Vector3, rotation: Quaternion = Quaternion.identity(), 
                           scale: Vector3 = Vector3.one(), randomRotation: Boolean = false) {
                    Node().apply {
                        this.renderable = renderable
                        this.localPosition = position
                        if (randomRotation) {
                            // Add slight random rotation for natural variation
                            val randomAngleX = (-15..15).random().toFloat()
                            val randomAngleY = (0..360).random().toFloat()
                            val randomAngleZ = (-15..15).random().toFloat()
                            this.localRotation = Quaternion.multiply(
                                Quaternion.axisAngle(Vector3(1f, 0f, 0f), randomAngleX),
                                Quaternion.multiply(
                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), randomAngleY),
                                    Quaternion.axisAngle(Vector3(0f, 0f, 1f), randomAngleZ)
                                )
                            )
                        } else {
                            this.localRotation = rotation
                        }
                        this.localScale = scale
                        setParent(this@PlantNode)
                        parts.add(this)
                    }
                }
            }

            stemMaterial.thenAccept { stemMat ->
                leafMaterial.thenAccept { leafMat ->
                    // Create basic shapes
                    val mainStem = ShapeFactory.makeCylinder(
                        plantCharacteristics.stemRadius,
                        plantCharacteristics.height,
                        Vector3.zero(),
                        stemMat
                    )

                    // Create leaf shapes based on plant type
                    val leafShape = when (currentPlantType) {
                        "cabbage" -> ShapeFactory.makeCube(
                            Vector3(plantCharacteristics.leafSize, plantCharacteristics.leafSize, plantCharacteristics.leafSize),
                            Vector3.zero(),
                            leafMat
                        )
                        else -> createLeafShape(plantCharacteristics.leafSize, leafMat)
                    }

                    fun createPlantNode(): PlantNode {
                        return PlantNode().apply {
                            // Add main stem
                            addPart(mainStem, Vector3(0f, plantCharacteristics.height / 2, 0f))

                            // Add leaves based on plant type
                            when (currentPlantType) {
                                "cabbage" -> {
                                    // Create multiple layers of leaves for a more realistic cabbage
                                    // Bottom layer - larger leaves
                                    for (i in 0..16) {  // Even more bottom leaves
                                        val angle = (i * 22.5f).toFloat()  // Better distribution
                                        val radius = 0.065f + (0..25).random() / 1000f  // Larger spread
                                        val height = 0.005f + (0..15).random() / 1000f
                                        val leafScale = Vector3(1.4f, 0.6f, 1.4f)  // Wider, flatter leaves
                                        
                                        // Create rotation for bottom leaves
                                        val bottomLeafRotation = Quaternion.multiply(
                                            Quaternion.axisAngle(Vector3(1f, 0f, 0f), 15f),
                                            Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                        )
                                        
                                        addPart(leafShape,
                                            Vector3(radius * Math.cos(angle.toDouble()).toFloat(),
                                                   height,
                                                   radius * Math.sin(angle.toDouble()).toFloat()),
                                            bottomLeafRotation,
                                            leafScale,
                                            false
                                        )
                                    }
                                    
                                    // Middle layer - medium leaves with more overlap
                                    for (i in 0..12) {
                                        val angle = (i * 30f + 15f).toFloat()
                                        val radius = 0.045f + (0..15).random() / 1000f
                                        val height = 0.035f + (0..20).random() / 1000f
                                        val leafScale = Vector3(1.1f, 0.7f, 1.1f)
                                        
                                        // Add more curve to middle leaves
                                        val midLeafRotation = Quaternion.multiply(
                                            Quaternion.axisAngle(Vector3(1f, 0f, 0f), 25f),
                                            Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                        )
                                        
                                        addPart(leafShape,
                                            Vector3(radius * Math.cos(angle.toDouble()).toFloat(),
                                                   height,
                                                   radius * Math.sin(angle.toDouble()).toFloat()),
                                            midLeafRotation,
                                            leafScale,
                                            false
                                        )
                                    }
                                    
                                    // Top layer - more curved inward leaves
                                    for (i in 0..10) {
                                        val angle = (i * 36f).toFloat()
                                        val radius = 0.025f + (0..10).random() / 1000f
                                        val height = 0.06f + (0..15).random() / 1000f
                                        val leafScale = Vector3(0.8f, 0.5f, 0.8f)
                                        // More pronounced curve for top leaves
                                        val topLeafRotation = Quaternion.multiply(
                                            Quaternion.axisAngle(Vector3(1f, 0f, 0f), 55f),
                                            Quaternion.multiply(
                                                Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                                                Quaternion.axisAngle(Vector3(0f, 0f, 1f), 15f)
                                            )
                                        )
                                        addPart(leafShape,
                                            Vector3(radius * Math.cos(angle.toDouble()).toFloat(),
                                                   height,
                                                   radius * Math.sin(angle.toDouble()).toFloat()),
                                            topLeafRotation,
                                            leafScale,
                                            false
                                        )
                                    }
                                    
                                    // Dense center leaves
                                    for (i in 0..5) {
                                        val height = 0.075f + (0..10).random() / 1000f
                                        val leafScale = Vector3(0.6f, 0.4f, 0.6f)
                                        val centerRotation = Quaternion.axisAngle(
                                            Vector3(1f, 0f, 0f),
                                            70f + (0..20).random().toFloat()
                                        )
                                        addPart(leafShape,
                                            Vector3(0f, height, 0f),
                                            centerRotation,
                                            leafScale,
                                            false
                                        )
                                    }
                                    
                                    // Add slight color variation to some leaves for realism
                                    val variantLeafCount = 8
                                    for (i in 0 until variantLeafCount) {
                                        val angle = (i * 45f).toFloat()
                                        val radius = 0.03f + (0..35).random() / 1000f
                                        val height = 0.02f + (0..60).random() / 1000f
                                        
                                        // Create slightly different colored leaf material
                                        MaterialFactory.makeOpaqueWithColor(
                                            this@MainActivity,
                                            com.google.ar.sceneform.rendering.Color(
                                                0.15f + (0..10).random() / 100f,
                                                0.55f + (0..15).random() / 100f,
                                                0.15f
                                            )
                                        ).thenAccept { variantMat ->
                                            val variantLeaf = ShapeFactory.makeCube(
                                                Vector3(plantCharacteristics.leafSize * 1.1f, 
                                                       plantCharacteristics.leafSize * 0.1f, 
                                                       plantCharacteristics.leafSize * 0.5f),
                                                Vector3.zero(),
                                                variantMat
                                            )
                                            
                                            val variantRotation = Quaternion.multiply(
                                                Quaternion.axisAngle(Vector3(1f, 0f, 0f), 30f + (0..30).random()),
                                                Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                            )
                                            
                                            addPart(variantLeaf,
                                                Vector3(radius * Math.cos(angle.toDouble()).toFloat(),
                                                       height,
                                                       radius * Math.sin(angle.toDouble()).toFloat()),
                                                variantRotation,
                                                Vector3(1f, 1f, 1f),
                                                false
                                            )
                                        }
                                    }
                                }
                                "tomato", "brinjal" -> {
                                    // Create main stem and branches with a more natural structure
                                    val branchCount = 6  // More branches
                                    for (i in 0 until branchCount) {
                                        val height = (i + 1) * (plantCharacteristics.height / (branchCount + 1))
                                        
                                        // Create two branches at each level, opposite to each other
                                        for (side in 0..1) {
                                            val baseAngle = (i * 60f + (side * 180f)).toFloat()
                                            val angle = baseAngle + (-15..15).random()
                                            
                                            // Main branch
                                            val branchLength = 0.08f + (0..20).random() / 1000f
                                            val branchStem = ShapeFactory.makeCylinder(
                                                plantCharacteristics.stemRadius * 0.7f,
                                                branchLength,
                                                Vector3.zero(),
                                                stemMat
                                            )
                                            
                                            // Position and rotate branch
                                            addPart(branchStem,
                                                Vector3(0f, height, 0f),
                                                Quaternion.multiply(
                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                                                    Quaternion.axisAngle(Vector3(0f, 0f, 1f), 30f)  // Angle branches upward
                                                )
                                            )

                                            // Add compound leaves (tomato plants have compound leaves)
                                            val leafletCount = 5  // Typical tomato leaf has 5-7 leaflets
                                            for (leaflet in 0 until leafletCount) {
                                                val leafletSize = plantCharacteristics.leafSize * 0.7f  // Smaller leaflets
                                                val leafletShape = createLeafShape(leafletSize, leafMat)
                                                
                                                // Position leaflets along the branch
                                                val leafletPosition = Vector3(
                                                    0.02f * Math.cos((angle + leaflet * 72f).toDouble()).toFloat(),
                                                    height + 0.02f + (leaflet * 0.01f),
                                                    0.02f * Math.sin((angle + leaflet * 72f).toDouble()).toFloat()
                                                )
                                                
                                                // Add natural variation to leaflet orientation
                                                val leafletRotation = Quaternion.multiply(
                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle + leaflet * 72f),
                                                    Quaternion.multiply(
                                                        Quaternion.axisAngle(Vector3(1f, 0f, 0f), 45f + (-10..10).random()),
                                                        Quaternion.axisAngle(Vector3(0f, 0f, 1f), (-20..20).random().toFloat())
                                                    )
                                                )
                                                
                                                addPart(leafletShape, leafletPosition, leafletRotation)
                                            }
                                        }
                                    }

                                    // Add tomatoes with better placement and variation
                                    if (plantCharacteristics.hasFlower && flowerMaterial != null) {
                                        flowerMaterial.thenAccept { flowerMat ->
                                            if (currentPlantType == "tomato") {
                                                // Create tomatoes of varying sizes and ripeness
                                                val tomatoColors = listOf(
                                                    com.google.ar.sceneform.rendering.Color(0.9f, 0.2f, 0.1f),  // Ripe red
                                                    com.google.ar.sceneform.rendering.Color(0.9f, 0.2f, 0.1f),  // Ripe red (duplicated to increase probability)
                                                    com.google.ar.sceneform.rendering.Color(0.9f, 0.2f, 0.1f),  // Ripe red (duplicated to increase probability)
                                                    com.google.ar.sceneform.rendering.Color(0.9f, 0.4f, 0.2f),  // Almost ripe
                                                    com.google.ar.sceneform.rendering.Color(0.7f, 0.7f, 0.2f)   // Green
                                                )
                                                
                                                // Add tomatoes in clusters
                                                val clusterCount = 5  // More clusters
                                                for (cluster in 0 until clusterCount) {
                                                    val clusterHeight = plantCharacteristics.height * (0.35f + (cluster * 0.13f))
                                                    val baseAngle = (cluster * 72f).toFloat()  // More evenly distributed
                                                    
                                                    // Create 2-4 tomatoes per cluster
                                                    val tomatoCount = 2 + (0..2).random()
                                                    for (i in 0 until tomatoCount) {
                                                        // Vary tomato sizes more realistically
                                                        val tomatoSize = 0.018f + (0..15).random() / 1000f
                                                        
                                                        // Create custom material for each tomato for color variation
                                                        MaterialFactory.makeOpaqueWithColor(this@MainActivity, tomatoColors.random())
                                                            .thenAccept { tomatoMat ->
                                                                val tomato = ShapeFactory.makeSphere(tomatoSize, Vector3.zero(), tomatoMat)
                                                                
                                                                // Position tomatoes in cluster
                                                                val angle = baseAngle + (-30..30).random()
                                                                val radius = 0.05f + (0..15).random() / 1000f
                                                                
                                                                // Add drooping effect
                                                                val tomatoRotation = Quaternion.multiply(
                                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                                                                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 45f)
                                                                )
                                                                
                                                                // Create position vector
                                                                val tomatoPosition = Vector3(
                                                                    radius * Math.cos(angle.toDouble()).toFloat(),
                                                                    clusterHeight + (i * 0.02f),
                                                                    radius * Math.sin(angle.toDouble()).toFloat()
                                                                )
                                                                
                                                                // Add the tomato with slight offset for more natural clustering
                                                                val offsetX = (-5..5).random() / 1000f
                                                                val offsetY = (-3..3).random() / 1000f
                                                                val offsetZ = (-5..5).random() / 1000f
                                                                
                                                                val finalPosition = Vector3(
                                                                    tomatoPosition.x + offsetX,
                                                                    tomatoPosition.y + offsetY,
                                                                    tomatoPosition.z + offsetZ
                                                                )
                                                                
                                                                addPart(tomato, finalPosition, tomatoRotation)
                                                                
                                                                // Add a small stem to some tomatoes
                                                                if ((0..2).random() == 1) {
                                                                    val stemSize = 0.002f
                                                                    val stemHeight = 0.005f
                                                                    val stemMaterial = MaterialFactory.makeOpaqueWithColor(
                                                                        this@MainActivity,
                                                                        com.google.ar.sceneform.rendering.Color(0.2f, 0.4f, 0.1f)
                                                                    )
                                                                    
                                                                    stemMaterial.thenAccept { stemMat ->
                                                                        val tomatoStem = ShapeFactory.makeCylinder(
                                                                            stemSize,
                                                                            stemHeight,
                                                                            Vector3.zero(),
                                                                            stemMat
                                                                        )
                                                                        
                                                                        val stemPosition = Vector3(
                                                                            finalPosition.x,
                                                                            finalPosition.y + tomatoSize,
                                                                            finalPosition.z
                                                                        )
                                                                        
                                                                        addPart(tomatoStem, stemPosition, Quaternion.identity())
                                                                    }
                                                                }
                                                            }
                                                    }
                                                }
                                            } else if (currentPlantType == "brinjal") {
                                                // Create brinjals (eggplants)
                                                // Brinjals are typically larger and fewer than tomatoes
                                                val brinjalsCount = 3 + (0..2).random()
                                                
                                                for (i in 0 until brinjalsCount) {
                                                    val height = plantCharacteristics.height * (0.4f + (i * 0.15f))
                                                    val angle = (i * 120f + (-30..30).random()).toFloat()
                                                    val radius = 0.06f + (0..10).random() / 1000f
                                                    
                                                    // Brinjals are elongated, so we'll use a cylinder with rounded ends
                                                    val brinjal = ShapeFactory.makeSphere(
                                                        0.025f + (0..10).random() / 1000f, 
                                                        Vector3.zero(), 
                                                        flowerMat
                                                    )
                                                    
                                                    // Position with slight droop
                                                    val brinjalRotation = Quaternion.multiply(
                                                        Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                                                        Quaternion.axisAngle(Vector3(1f, 0f, 0f), 60f)  // More pronounced droop
                                                    )
                                                    
                                                    val brinjPosition = Vector3(
                                                        radius * Math.cos(angle.toDouble()).toFloat(),
                                                        height,
                                                        radius * Math.sin(angle.toDouble()).toFloat()
                                                    )
                                                    
                                                    addPart(brinjal, brinjPosition, brinjalRotation)
                                                    
                                                    // Add stem and calyx (the green part at the top of eggplant)
                                                    val stemMaterial = MaterialFactory.makeOpaqueWithColor(
                                                        this@MainActivity,
                                                        com.google.ar.sceneform.rendering.Color(0.2f, 0.5f, 0.2f)
                                                    )
                                                    
                                                    stemMaterial.thenAccept { stemMat ->
                                                        // Stem
                                                        val stem = ShapeFactory.makeCylinder(
                                                            0.003f,
                                                            0.01f,
                                                            Vector3.zero(),
                                                            stemMat
                                                        )
                                                        
                                                        // Position stem at top of brinjal
                                                        val stemPosition = Vector3(
                                                            brinjPosition.x + 0.02f * Math.sin(angle.toDouble()).toFloat(),
                                                            brinjPosition.y + 0.02f,
                                                            brinjPosition.z - 0.02f * Math.cos(angle.toDouble()).toFloat()
                                                        )
                                                        
                                                        addPart(stem, stemPosition, brinjalRotation)
                                                        
                                                        // Add calyx (green cap)
                                                        for (j in 0 until 5) {
                                                            val calyxAngle = j * 72f
                                                            val calyxLeaf = ShapeFactory.makeCube(
                                                                Vector3(0.01f, 0.002f, 0.015f),
                                                                Vector3.zero(),
                                                                stemMat
                                                            )
                                                            
                                                            val calyxRotation = Quaternion.multiply(
                                                                brinjalRotation,
                                                                Quaternion.multiply(
                                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), calyxAngle),
                                                                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 30f)
                                                                )
                                                            )
                                                            
                                                            addPart(calyxLeaf, stemPosition, calyxRotation)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Root vegetable arrangement (carrot-like plants)
                                    // Create the root/vegetable part
                                    val rootHeight = 0.08f
                                    val rootTopRadius = 0.015f
                                    val rootBottomRadius = 0.004f
                                    
                                    // Create root material with orange color for carrots
                                    MaterialFactory.makeOpaqueWithColor(this@MainActivity, 
                                        com.google.ar.sceneform.rendering.Color(0.95f, 0.45f, 0.1f))
                                        .thenAccept { rootMat ->
                                            // Create a tapered root using multiple cylinders
                                            val segments = 4
                                            for (i in 0 until segments) {
                                                val progress = i.toFloat() / (segments - 1)
                                                val currentRadius = rootTopRadius * (1 - progress) + rootBottomRadius * progress
                                                val segmentHeight = rootHeight / segments
                                                val yOffset = i * segmentHeight
                                                
                                                val rootSegment = ShapeFactory.makeCylinder(
                                                    currentRadius,
                                                    segmentHeight,
                                                    Vector3.zero(),
                                                    rootMat
                                                )
                                                
                                                // Position each segment
                                                addPart(rootSegment,
                                                    Vector3(0f, (rootHeight * 0.3f) - yOffset, 0f),
                                                    Quaternion.axisAngle(Vector3(0f, 0f, 1f), 180f)
                                                )
                                            }
                                            
                                            // Add small root tendrils at the bottom
                                            val tendrilRadius = 0.001f
                                            val tendrilHeight = 0.015f
                                            val tendrilCount = 5
                                            
                                            for (i in 0 until tendrilCount) {
                                                val tendril = ShapeFactory.makeCylinder(
                                                    tendrilRadius,
                                                    tendrilHeight,
                                                    Vector3.zero(),
                                                    rootMat
                                                )
                                                
                                                val angle = (i * 72f).toFloat()
                                                val tendrilRotation = Quaternion.multiply(
                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                                                    Quaternion.axisAngle(Vector3(0f, 0f, 1f), 45f)
                                                )
                                                
                                                addPart(tendril,
                                                    Vector3(0.01f * Math.cos(angle.toDouble()).toFloat(),
                                                           rootHeight * 0.1f,
                                                           0.01f * Math.sin(angle.toDouble()).toFloat()),
                                                    tendrilRotation
                                                )
                                            }
                                    }

                                    // Add foliage (leaves)
                                    val leafCount = 8  // More leaves
                                    val leafClusters = 3  // Multiple clusters
                                    
                                    for (cluster in 0 until leafClusters) {
                                        val clusterHeight = 0.05f + (cluster * 0.03f)
                                        val clusterAngleOffset = (cluster * 40f).toFloat()
                                        
                                        for (i in 0 until leafCount) {
                                            val height = clusterHeight + (0..10).random() / 1000f
                                            val angle = (i * (360f / leafCount) + clusterAngleOffset + (-10..10).random()).toFloat()
                                            val radius = 0.02f + (0..10).random() / 1000f
                                            
                                            // Create more natural-looking leaves
                                            val leafRotation = Quaternion.multiply(
                                                Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                                                Quaternion.axisAngle(Vector3(1f, 0f, 0f), 30f + (-10..10).random())
                                            )
                                            
                                            // Vary leaf sizes slightly
                                            val leafScale = Vector3(
                                                1f + (0..20).random() / 100f,
                                                1f,
                                                1f + (0..20).random() / 100f
                                            )
                                            
                                            addPart(leafShape,
                                                Vector3(radius * Math.cos(angle.toDouble()).toFloat(),
                                                       height,
                                                       radius * Math.sin(angle.toDouble()).toFloat()),
                                                leafRotation,
                                                leafScale,
                                                false
                                            )
                                        }
                                    }
                                    
                                    // Add thin stems connecting leaves to main stem
                                    for (i in 0 until 6) {
                                        val stemHeight = 0.04f + (i * 0.02f)
                                        val angle = (i * 60f).toFloat()
                                        
                                        val thinStem = ShapeFactory.makeCylinder(
                                            0.001f,
                                            0.03f,
                                            Vector3.zero(),
                                            stemMat
                                        )
                                        
                                        addPart(thinStem,
                                            Vector3(0.01f * Math.cos(angle.toDouble()).toFloat(),
                                                   stemHeight,
                                                   0.01f * Math.sin(angle.toDouble()).toFloat()),
                                            Quaternion.multiply(
                                                Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                                                Quaternion.axisAngle(Vector3(0f, 0f, 1f), 30f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Create plant grid
                    var plantsPlaced = 0
                    for (row in 0 until rows) {
                        for (col in 0 until cols) {
                            val x = startX + (col * plantSpacing)
                            val z = startZ + (row * plantSpacing)
                            
                            if (isPointInside(x, z)) {
                                createPlantNode().apply {
                                    setParent(arFragment?.arSceneView?.scene)
                                    localPosition = Vector3(x, groundY, z)
                                    localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 
                                        (Math.random() * 360).toFloat())
                                    val scale = 0.9f + (0..20).random() / 100f
                                    localScale = Vector3(scale, scale, scale)
                                    plantNodes.add(this)
                                    plantsPlaced++
                                }
                            }
                        }
                    }
                    
                    runOnUiThread {
                        measurementText?.text = "${measurementText?.text}\nPlants placed: $plantsPlaced ($currentPlantType plants)"
                    }
                }
            }
        } catch (e: Exception) {
            println("Error creating plant model: ${e.message}")
            e.printStackTrace()
        }
    }

    private data class PlantStyle(
        val height: Float = 0.2f,
        val stemRadius: Float = 0.008f,
        val leafSize: Float = 0.05f,
        val leafColor: com.google.ar.sceneform.rendering.Color = com.google.ar.sceneform.rendering.Color(0.2f, 0.8f, 0.2f),
        val hasFlower: Boolean = false,
        val flowerColor: com.google.ar.sceneform.rendering.Color = com.google.ar.sceneform.rendering.Color(1f, 1f, 0.2f)
    )

    private fun createLeafShape(size: Float, material: com.google.ar.sceneform.rendering.Material): Renderable {
        // Create a more natural leaf shape using a flattened, elongated cube
        return ShapeFactory.makeCube(
            Vector3(size, size * 0.1f, size * 0.4f),
            Vector3.zero(),
            material
        )
    }

    private fun createPlantGrid(renderable: Renderable, startX: Float, startZ: Float, groundY: Float, 
                              rows: Int, cols: Int, spacing: Float): Int {
        var plantsPlaced = 0
        try {
            println("Creating plant grid: $rows rows × $cols cols starting at ($startX, $groundY, $startZ)")
            
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    // Calculate position for this plant
                    val x = startX + (col * spacing)
                    val z = startZ + (row * spacing)
                    
                    // Only place plant if position is inside the boundary
                    if (isPointInside(x, z)) {
                        // Create plant node
                        Node().apply {
                            setParent(arFragment?.arSceneView?.scene)
                            this.renderable = renderable
                            localPosition = Vector3(x, groundY, z)
                            // Add random rotation for variety
                            localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), (Math.random() * 360).toFloat())
                            // Add slight random scale variation for natural look
                            val scale = 0.9f + (0..20).random() / 100f
                            localScale = Vector3(scale, scale, scale)
                            plantNodes.add(this)
                            plantsPlaced++
                            
                            println("Added plant at ($x, $groundY, $z) with scale $scale")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error in createPlantGrid: ${e.message}")
            e.printStackTrace()
        }
        return plantsPlaced
    }

    // Function to check if a point is inside the measured quadrilateral
    private fun isPointInside(x: Float, z: Float): Boolean {
        // Implementation of ray casting algorithm
        var inside = false
        var j = points.size - 1
        for (i in points.indices) {
            if ((points[i].z > z) != (points[j].z > z) &&
                x < (points[j].x - points[i].x) * (z - points[i].z) / 
                (points[j].z - points[i].z) + points[i].x) {
                inside = !inside
            }
            j = i
        }
        return inside
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
            
            // Clear plants
            clearPlants()

            points.clear()
            measurementText?.text = ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}