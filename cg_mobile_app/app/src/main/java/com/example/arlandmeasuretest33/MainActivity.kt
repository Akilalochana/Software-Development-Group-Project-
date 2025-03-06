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
            
            // Get plant type from intent
            plantType = intent.getStringExtra("PLANT_TYPE") ?: "carrot"
            
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
            
            // Create a simple cylinder for immediate feedback
            MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(0f, 0.8f, 0f))
                .thenAccept { material ->
                    ShapeFactory.makeCylinder(
                        0.05f, // radius
                        0.2f,  // height
                        Vector3.zero(), // center position
                        material
                    ).also { renderable ->
                        val plantsPlaced = createPlantGrid(renderable, startX, startZ, groundY, rows, cols, plantSpacing)
                        isShowingPlants = true
                        // Update measurement text to include plant count
                        measurementText?.text = "${measurementText?.text}\nPlants placed: $plantsPlaced"
                    }
                }
                .exceptionally { throwable ->
                    println("Error creating plant renderable: ${throwable.message}")
                    throwable.printStackTrace()
                    null
                }
            
        } catch (e: Exception) {
            println("Error in showPlantGrid: ${e.message}")
            e.printStackTrace()
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
                (points[j].z - points[i].z) + points[i].x) {
                inside = !inside
            }
            j = i
        }
        return inside
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
                            // Add slight random scale variation
                            val scale = 0.8f + (Math.random() * 0.4f).toFloat()
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