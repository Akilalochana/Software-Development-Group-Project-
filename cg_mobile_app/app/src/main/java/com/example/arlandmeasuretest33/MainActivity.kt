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
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView

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

        findViewById<Button>(R.id.btn_show_plants)?.setOnClickListener {
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
    private fun showPlantInfoPopup() {
        try {
            // Calculate measurements from current state
            val area = if (points.size == 4) calculateQuadrilateralArea(points) else 0f
            val plantCount = plantNodes.size
            val plantTypeName = when (plantType?.lowercase()) {
                "carrot" -> "Carrot"
                "cabbage" -> "Cabbage"
                else -> "Generic Plant"
            }
            val district = intent.getStringExtra("SELECTED_DISTRICT") ?: "Unknown Location"

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

            // Set values
            dialog.findViewById<TextView>(R.id.plantTypeValue).text = plantTypeName
            dialog.findViewById<TextView>(R.id.plantCountValue).text = plantCount.toString()
            dialog.findViewById<TextView>(R.id.areaValue).text = String.format("%.2f m²", area)
            dialog.findViewById<TextView>(R.id.locationValue).text = district

            // Set plant-specific animation if available
            val animationView = dialog.findViewById<LottieAnimationView>(R.id.plantAnimation)
            when (plantType?.lowercase()) {
                "carrot" -> animationView.setAnimation(R.raw.carrot_growing)
                "cabbage" -> animationView.setAnimation(R.raw.cabbage_growing)
                else -> animationView.setAnimation(R.raw.plant_growing)
            }

            // Handle button click
            dialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error showing plant information", Toast.LENGTH_SHORT).show()
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

            // Get plant type from intent
            plantType = intent.getStringExtra("PLANT_TYPE")?.lowercase() ?: "carrot"

            // Calculate grid dimensions
            val boundingWidth = points.maxOf { it.x } - points.minOf { it.x }
            val boundingHeight = points.maxOf { it.z } - points.minOf { it.z }

            val cols = (boundingWidth / plantSpacing).toInt().coerceAtLeast(1)
            val rows = (boundingHeight / plantSpacing).toInt().coerceAtLeast(1)

            val startX = points.minOf { it.x }
            val startZ = points.minOf { it.z }
            val groundY = points.map { it.y }.average().toFloat()

            // Create plant renderable based on plant type
            createPlantRenderable(
                plantType ?: "default",
                startX,
                startZ,
                groundY,
                rows,
                cols,
                plantSpacing
            )

            isShowingPlants = true

            // Show popup with plant information
            showPlantInfoPopup()
        } catch (e: Exception) {
            println("Error in showPlantGrid: ${e.message}")
            e.printStackTrace()
        }
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
        val stemGreen = com.google.ar.sceneform.rendering.Color(0.48f, 0.82f, 0.3f)     // Stem color     // Much brighter stem      // Brighter stem

        MaterialFactory.makeOpaqueWithColor(this, deepOrange)
            .thenAccept { orangeMaterial ->
                MaterialFactory.makeOpaqueWithColor(this, lightGreen)
                    .thenAccept { lightLeafMaterial ->
                        MaterialFactory.makeOpaqueWithColor(this, mediumGreen)
                            .thenAccept { mediumLeafMaterial ->
                                MaterialFactory.makeOpaqueWithColor(this, stemGreen)
                                    .thenAccept { stemMaterial ->
                                        // Plant spacing configuration
                                        val carrotSpacing = spacing * 0.85f // More compact

                                        // Calculate grid dimensions
                                        val adjustedCols = (cols * spacing / carrotSpacing).toInt().coerceAtLeast(1)
                                        val adjustedRows = (rows * spacing / carrotSpacing).toInt().coerceAtLeast(1)

                                        // Place plants in grid
                                        for (row in 0 until adjustedRows) {
                                            for (col in 0 until adjustedCols) {
                                                val jitter = (Math.random() * 0.04 - 0.02).toFloat()
                                                val x = startX + (col * carrotSpacing) + jitter
                                                val z = startZ + (row * carrotSpacing) + jitter

                                                if (isPointInside(x, z)) {
                                                    // Create parent node
                                                    val plantNode = Node()
                                                    plantNode.setParent(arFragment?.arSceneView?.scene)
                                                    plantNode.localPosition = Vector3(x, groundY + 0.005f, z)

                                                    // Minimal rotation for consistency
                                                    plantNode.localRotation = Quaternion.axisAngle(
                                                        Vector3(0f, 1f, 0f),
                                                        (Math.random() * 360).toFloat()
                                                    )

                                                    // Increased overall scale for more realistic size
                                                    val scale = 0.85f + (Math.random() * 0.15f).toFloat()
                                                    plantNode.localScale = Vector3(scale, scale, scale)

                                                    // Create center thin cylinder as core
                                                    val carrotCore = ShapeFactory.makeCylinder(
                                                        0.004f,
                                                        0.08f,
                                                        Vector3(0f, 0f, 0f),
                                                        orangeMaterial
                                                    )

                                                    val coreNode = Node()
                                                    coreNode.setParent(plantNode)
                                                    coreNode.localPosition = Vector3(0f, -0.02f, 0f)
                                                    coreNode.renderable = carrotCore

                                                    // Create layered carrot shape (top to bottom)
                                                    createCarrotLayers(plantNode, orangeMaterial)

                                                    // Create thin stem connecting to leaves
                                                    val stemHeight = 0.015f
                                                    val stem = ShapeFactory.makeCylinder(
                                                        0.003f,
                                                        stemHeight,
                                                        Vector3(0f, 0f, 0f),
                                                        stemMaterial
                                                    )

                                                    val stemNode = Node()
                                                    stemNode.setParent(plantNode)
                                                    stemNode.localPosition = Vector3(0f, 0.025f, 0f)
                                                    stemNode.renderable = stem

                                                    // Create spread out foliage
                                                    createSpreadFoliage(plantNode, lightLeafMaterial, mediumLeafMaterial)

                                                    plantNodes.add(plantNode)
                                                    plantsPlaced++
                                                }
                                            }
                                        }
                                        runOnUiThread {
                                            measurementText?.text = "${measurementText?.text}\nCarrots planted: $plantsPlaced"
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

        // Set exact dimensions as requested: 40cm x 50cm (0.4m x 0.5m)
        val cabbageWidthSpacing = 0.4f  // 40cm width
        val cabbageLengthSpacing = 0.5f  // 50cm length

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
                                                // Calculate number of rows and columns based on exact dimensions
                                                val adjustedCols = (cols * spacing / cabbageWidthSpacing).toInt().coerceAtLeast(1)
                                                val adjustedRows = (rows * spacing / cabbageLengthSpacing).toInt().coerceAtLeast(1)

                                                // Place cabbages in grid with proper rectangular spacing
                                                for (row in 0 until adjustedRows) {
                                                    for (col in 0 until adjustedCols) {
                                                        val x = startX + (col * cabbageWidthSpacing) + (Math.random() * 0.02 - 0.01).toFloat()
                                                        val z = startZ + (row * cabbageLengthSpacing) + (Math.random() * 0.02 - 0.01).toFloat()

                                                        if (isPointInside(x, z)) {
                                                            // Create parent node
                                                            val cabbageNode = Node()
                                                            cabbageNode.setParent(arFragment?.arSceneView?.scene)
                                                            cabbageNode.localPosition = Vector3(x, groundY, z)

                                                            // Minimal rotation - cabbage heads up with very small variation
                                                            cabbageNode.localRotation = Quaternion.axisAngle(
                                                                Vector3(0f, 1f, 0f),
                                                                (Math.random() * 15).toFloat() // More uniform orientation
                                                            )

                                                            // Consistent scale with minimal variation
                                                            val scale = 0.97f + (Math.random() * 0.06f).toFloat()
                                                            cabbageNode.localScale = Vector3(scale, scale, scale)

                                                            // Create stem
                                                            val stem = ShapeFactory.makeCylinder(
                                                                0.015f,  // radius
                                                                0.06f,   // height
                                                                Vector3(0f, 0f, 0f),
                                                                stemMaterial
                                                            )
                                                            val stemNode = Node()
                                                            stemNode.setParent(cabbageNode)
                                                            stemNode.localPosition = Vector3(0f, -0.02f, 0f)
                                                            stemNode.renderable = stem

                                                            // Create core sphere (cabbage heart)
                                                            val core = ShapeFactory.makeSphere(
                                                                0.05f,  // radius
                                                                Vector3(0f, 0f, 0f),
                                                                innerMaterial
                                                            )
                                                            val coreNode = Node()
                                                            coreNode.setParent(cabbageNode)
                                                            coreNode.localPosition = Vector3(0f, 0.045f, 0f)
                                                            coreNode.renderable = core

                                                            // Layer 1 - Bottom outer leaves (largest, nearly flat)
                                                            for (i in 0 until 8) {
                                                                val angle = (i * 45f).toFloat()
                                                                val radius = 0.09f + (Math.random() * 0.008f).toFloat()
                                                                val height = -0.01f + (Math.random() * 0.004f).toFloat()

                                                                val leafRotation = Quaternion.multiply(
                                                                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 10f + (Math.random() * 3).toFloat()),
                                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                                                )

                                                                ShapeFactory.makeCube(
                                                                    Vector3(0.08f, 0.01f, 0.06f),
                                                                    Vector3(0f, 0f, 0f),
                                                                    darkMaterial
                                                                ).also { leafRenderable ->
                                                                    Node().apply {
                                                                        setParent(cabbageNode)
                                                                        localPosition = Vector3(
                                                                            radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                                                                            height,
                                                                            radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
                                                                        )
                                                                        localRotation = leafRotation
                                                                        renderable = leafRenderable
                                                                    }
                                                                }
                                                            }

                                                            // Remaining layers (2-5) with similar implementation...
                                                            // Layer 2 - Mid-outer leaves
                                                            for (i in 0 until 8) {
                                                                val angle = (i * 45f + 22.5f).toFloat()
                                                                val radius = 0.07f + (Math.random() * 0.006f).toFloat()
                                                                val height = 0.015f + (Math.random() * 0.006f).toFloat()

                                                                val leafRotation = Quaternion.multiply(
                                                                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 20f + (Math.random() * 3).toFloat()),
                                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                                                )

                                                                ShapeFactory.makeCube(
                                                                    Vector3(0.07f, 0.012f, 0.055f),
                                                                    Vector3(0f, 0f, 0f),
                                                                    darkMaterial
                                                                ).also { leafRenderable ->
                                                                    Node().apply {
                                                                        setParent(cabbageNode)
                                                                        localPosition = Vector3(
                                                                            radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                                                                            height,
                                                                            radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
                                                                        )
                                                                        localRotation = leafRotation
                                                                        renderable = leafRenderable
                                                                    }
                                                                }
                                                            }

                                                            // Layer 3 - Middle leaves
                                                            for (i in 0 until 8) {
                                                                val angle = (i * 45f).toFloat()
                                                                val radius = 0.05f + (Math.random() * 0.006f).toFloat()
                                                                val height = 0.04f + (Math.random() * 0.006f).toFloat()

                                                                val leafRotation = Quaternion.multiply(
                                                                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 30f + (Math.random() * 3).toFloat()),
                                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                                                )

                                                                ShapeFactory.makeCube(
                                                                    Vector3(0.06f, 0.014f, 0.05f),
                                                                    Vector3(0f, 0f, 0f),
                                                                    mediumMaterial
                                                                ).also { leafRenderable ->
                                                                    Node().apply {
                                                                        setParent(cabbageNode)
                                                                        localPosition = Vector3(
                                                                            radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                                                                            height,
                                                                            radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
                                                                        )
                                                                        localRotation = leafRotation
                                                                        renderable = leafRenderable
                                                                    }
                                                                }
                                                            }

                                                            // Layer 4 - Inner leaves
                                                            for (i in 0 until 6) {
                                                                val angle = (i * 60f + 30f).toFloat()
                                                                val radius = 0.035f + (Math.random() * 0.004f).toFloat()
                                                                val height = 0.065f + (Math.random() * 0.006f).toFloat()

                                                                val leafRotation = Quaternion.multiply(
                                                                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 45f + (Math.random() * 3).toFloat()),
                                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                                                )

                                                                ShapeFactory.makeCube(
                                                                    Vector3(0.05f, 0.015f, 0.04f),
                                                                    Vector3(0f, 0f, 0f),
                                                                    mediumMaterial
                                                                ).also { leafRenderable ->
                                                                    Node().apply {
                                                                        setParent(cabbageNode)
                                                                        localPosition = Vector3(
                                                                            radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                                                                            height,
                                                                            radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
                                                                        )
                                                                        localRotation = leafRotation
                                                                        renderable = leafRenderable
                                                                    }
                                                                }
                                                            }

                                                            // Layer 5 - Top inner leaves
                                                            for (i in 0 until 4) {
                                                                val angle = (i * 90f).toFloat()
                                                                val radius = 0.02f + (Math.random() * 0.003f).toFloat()
                                                                val height = 0.085f + (Math.random() * 0.004f).toFloat()

                                                                val leafRotation = Quaternion.multiply(
                                                                    Quaternion.axisAngle(Vector3(1f, 0f, 0f), 60f + (Math.random() * 3).toFloat()),
                                                                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle)
                                                                )

                                                                ShapeFactory.makeCube(
                                                                    Vector3(0.04f, 0.016f, 0.03f),
                                                                    Vector3(0f, 0f, 0f),
                                                                    lightMaterial
                                                                ).also { leafRenderable ->
                                                                    Node().apply {
                                                                        setParent(cabbageNode)
                                                                        localPosition = Vector3(
                                                                            radius * Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                                                                            height,
                                                                            radius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
                                                                        )
                                                                        localRotation = leafRotation
                                                                        renderable = leafRenderable
                                                                    }
                                                                }
                                                            }

                                                            plantNodes.add(cabbageNode)
                                                            plantsPlaced++
                                                        }
                                                    }
                                                }
                                                runOnUiThread {
                                                    measurementText?.text = "${measurementText?.text}\nCabbages planted: $plantsPlaced"
                                                }
                                            }
                                    }
                            }
                    }
            }
    }
    // Helper function to create a circular layer of leaves
    private fun createCircularLayer(
        parent: Node,
        fragmentCount: Int,
        radius: Float,
        height: Float,
        baseTilt: Float,
        leafLength: Float,
        leafWidth: Float,
        material: com.google.ar.sceneform.rendering.Material
    ) {
        for (i in 0 until fragmentCount) {
            val angle = (i * 360f / fragmentCount + (Math.random() * 5 - 2.5f)).toFloat()
            val actualRadius = radius * (0.95f + (Math.random() * 0.1f).toFloat())
            val actualHeight = height + (Math.random() * 0.01f).toFloat()

            // Vary the tilt slightly for each leaf
            val tiltVariation = (Math.random() * 10 - 5).toFloat()
            val sideVariation = (Math.random() * 16 - 8).toFloat()

            // Create fragment rotation with proper curvature
            val fragmentRotation = Quaternion.multiply(
                Quaternion.axisAngle(Vector3(1f, 0f, 0f), baseTilt + tiltVariation),
                Quaternion.multiply(
                    Quaternion.axisAngle(Vector3(0f, 1f, 0f), angle),
                    Quaternion.axisAngle(Vector3(0f, 0f, 1f), sideVariation)
                )
            )

            // Create leaf with random size variation
            val actualLeafLength = leafLength * (0.9f + (Math.random() * 0.2f).toFloat())
            val actualLeafWidth = leafWidth * (0.9f + (Math.random() * 0.2f).toFloat())

            ShapeFactory.makeCube(
                Vector3(
                    actualLeafLength,
                    0.005f + (Math.random() * 0.003f).toFloat(), // slight thickness variation
                    actualLeafWidth
                ),
                Vector3(0f, 0f, 0f),
                material
            ).also { fragmentRenderable ->
                Node().apply {
                    setParent(parent)
                    localPosition = Vector3(
                        actualRadius * Math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                        actualHeight,
                        actualRadius * Math.sin(Math.toRadians(angle.toDouble())).toFloat()
                    )
                    localRotation = fragmentRotation
                    renderable = fragmentRenderable
                }
            }
        }
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
                                        // Place plants in a grid pattern
                                        for (row in 0 until rows) {
                                            for (col in 0 until cols) {
                                                val x = startX + col * spacing
                                                val z = startZ + row * spacing

                                                // Only place plants if inside the polygon
                                                if (isPointInside(x, z)) {
                                                    // Create plant anchor
                                                    val plantAnchor = arFragment?.arSceneView?.session?.createAnchor(
                                                        com.google.ar.core.Pose.makeTranslation(
                                                            x, groundY, z
                                                        )
                                                    )

                                                    val anchorNode = AnchorNode(plantAnchor).apply {
                                                        setParent(arFragment?.arSceneView?.scene)
                                                    }

                                                    val plantNode = Node().apply {
                                                        setParent(anchorNode)
                                                    }

                                                    // Create stem first
                                                    createDetailedStem(plantNode, stemMaterial)

                                                    // Add foliage to the plant
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
                                            }
                                        }

                                        // Update UI to show number of plants placed
                                        runOnUiThread {
                                            measurementText?.text = String.format(
                                                "Plants placed: %d", plantsPlaced
                                            )
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



    private fun createLeaflet(
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

        // Angle based on side
        var baseAngle = when(side) {
            -1 -> -45f    // left side
            1 -> 45f      // right side
            else -> 0f    // terminal leaflet
        }

        // Add variation
        baseAngle += (Math.random() * 20 - 10).toFloat()

        // Tilt leaflet upward
        val upTilt = 20f + (Math.random() * 20)

        leafletNode.localRotation = Quaternion.multiply(
            Quaternion.axisAngle(Vector3(0f, 1f, 0f), baseAngle),
            Quaternion.axisAngle(Vector3(1f, 0f, 0f), -upTilt.toFloat())
        )

        // Create leaflet shape - smaller than main leaves
        val leafletLength = 0.03f + (Math.random() * 0.02f).toFloat()
        val leafletWidth = 0.015f + (Math.random() * 0.01f).toFloat()

        val leaflet = ShapeFactory.makeCube(
            Vector3(leafletWidth, 0.002f, leafletLength),
            Vector3(0f, 0f, leafletLength * 0.4f),
            material
        )

        leafletNode.renderable = leaflet
    }

    private fun isPointInside(x: Float, z: Float): Boolean {
        if (points.size < 3) return false

        // Ray casting algorithm to determine if point is inside polygon
        var inside = false
        val p = Vector3(x, points[0].y, z)  // Use consistent y-value

        for (i in points.indices) {
            val j = (i + 1) % points.size

            val pi = points[i]
            val pj = points[j]

            // Check if point is on an edge
            if (distanceToLineSegment(pi, pj, p) < 0.01f) {
                return true
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

    fun onShowPlantsClicked(view: View) {
        val intent = Intent(this, PlantVisualizationActivity::class.java)
        startActivity(intent)
    }

}