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
            "tomato" -> createTomatoRenderable(startX, startZ, groundY, rows, cols, spacing)
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
    private fun createTomatoRenderable(
        startX: Float,
        startZ: Float,
        groundY: Float,
        rows: Int,
        cols: Int,
        spacing: Float
    ) {
        var plantsPlaced = 0

        // Create green material for stem
        MaterialFactory.makeOpaqueWithColor(
            this,
            com.google.ar.sceneform.rendering.Color(0.1f, 0.5f, 0.1f)
        )
            .thenAccept { stemMaterial ->
                // Create red material for tomatoes
                MaterialFactory.makeOpaqueWithColor(
                    this,
                    com.google.ar.sceneform.rendering.Color(0.9f, 0.1f, 0.1f)
                )
                    .thenAccept { tomatoMaterial ->
                        // Create stem (cylinder)
                        val stemRenderable = ShapeFactory.makeCylinder(
                            0.01f, // radius
                            0.25f, // height
                            Vector3(0f, 0f, 0f), // center
                            stemMaterial
                        )

                        // Place tomato plants in grid
                        for (row in 0 until rows) {
                            for (col in 0 until cols) {
                                val x = startX + (col * spacing)
                                val z = startZ + (row * spacing)

                                if (isPointInside(x, z)) {
                                    // Create parent node
                                    val plantNode = Node()
                                    plantNode.setParent(arFragment?.arSceneView?.scene)
                                    plantNode.localPosition = Vector3(x, groundY, z)

                                    // Add stem
                                    val stemNode = Node()
                                    stemNode.localPosition = Vector3(0f, 0.125f, 0f)
                                    stemNode.renderable = stemRenderable
                                    stemNode.setParent(plantNode)

                                    // Add 2-5 tomatoes
                                    val tomatoCount = 2 + (Math.random() * 4).toInt()
                                    for (i in 0 until tomatoCount) {
                                        // Create tomato (sphere)
                                        val size = 0.02f + (Math.random() * 0.02f).toFloat()
                                        val tomato = ShapeFactory.makeSphere(
                                            size, // radius
                                            Vector3(0f, 0f, 0f), // center
                                            tomatoMaterial
                                        )

                                        // Calculate position
                                        val angle =
                                            (i * (360f / tomatoCount) + (Math.random() * 30).toInt()).toFloat()
                                        val radius = 0.05f + (Math.random() * 0.02f).toFloat()
                                        val height = 0.1f + (Math.random() * 0.15f).toFloat()

                                        val tomatoNode = Node()
                                        tomatoNode.setParent(plantNode)
                                        tomatoNode.localPosition = Vector3(
                                            radius * Math.sin(Math.toRadians(angle.toDouble()))
                                                .toFloat(),
                                            height,
                                            radius * Math.cos(Math.toRadians(angle.toDouble()))
                                                .toFloat()
                                        )
                                        tomatoNode.renderable = tomato
                                    }

                                    plantNodes.add(plantNode)
                                    plantsPlaced++
                                }
                            }
                        }
                        measurementText?.text =
                            "${measurementText?.text}\nTomato plants planted: $plantsPlaced"
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

        // Create green material for generic plant
        MaterialFactory.makeOpaqueWithColor(
            this,
            com.google.ar.sceneform.rendering.Color(0.0f, 0.6f, 0.0f)
        )
            .thenAccept { plantMaterial ->
                // Place generic plants in grid
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val x = startX + (col * spacing)
                        val z = startZ + (row * spacing)

                        if (isPointInside(x, z)) {
                            // Create parent node
                            val plantNode = Node()
                            plantNode.setParent(arFragment?.arSceneView?.scene)
                            plantNode.localPosition = Vector3(x, groundY, z)

                            // Create stem
                            val stem = ShapeFactory.makeCylinder(
                                0.01f, // radius
                                0.2f,  // height
                                Vector3(0f, 0.1f, 0f), // center
                                plantMaterial
                            )

                            val stemNode = Node()
                            stemNode.renderable = stem
                            stemNode.setParent(plantNode)

                            // Create leaves (cubes)
                            for (i in 0 until 2) {
                                val leafNode = Node()
                                leafNode.setParent(plantNode)

                                // Position leaf
                                leafNode.localPosition = Vector3(0f, 0.15f, 0f)
                                leafNode.localRotation = Quaternion.axisAngle(
                                    Vector3(0f, 1f, 0f),
                                    i * 180f
                                )

                                // Create leaf
                                val leaf = ShapeFactory.makeCube(
                                    Vector3(0.08f, 0.01f, 0.04f),
                                    Vector3(0.04f, 0f, 0f),
                                    plantMaterial
                                )

                                leafNode.renderable = leaf
                            }

                            plantNodes.add(plantNode)
                            plantsPlaced++
                        }
                    }
                }
                measurementText?.text = "${measurementText?.text}\nPlants planted: $plantsPlaced"
            }
    }


    // Function to check if a point is inside the measured quadrilateral
    private fun isPointInside(x: Float, z: Float): Boolean {
        // Implementation of ray casting algorithm
        var inside = false
        var j = points.size - 1
        for (i in points.indices) {
            if ((points[i].z > z) != (points[j].z > z) &&
                x < (points[j].x - points[i].x) * (z - points[i].z) /
                (points[j].z - points[i].z) + points[i].x
            ) {
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

    fun onShowPlantsClicked(view: View) {
        val intent = Intent(this, PlantVisualizationActivity::class.java)
        startActivity(intent)
    }

}