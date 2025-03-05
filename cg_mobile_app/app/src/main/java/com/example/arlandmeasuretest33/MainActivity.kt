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
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.rendering.ModelRenderable

class MainActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private val placedAnchors = ArrayList<Anchor>()
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val measurementNodes = ArrayList<Node>()
    private val points = ArrayList<Vector3>()
    private var measurementText: TextView? = null
    private var toggleGridButton: Button? = null
    private var isDrawing = false
    
    // Grid visualization variables
    private val gridNodes = ArrayList<Node>()
    private var plantSpacing = 0.3f // Default spacing in meters
    private var plantModel: ModelRenderable? = null
    private var isGridVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get garden information from intent
        val gardenName = intent.getStringExtra("GARDEN_NAME") ?: ""
        val selectedDistrict = intent.getStringExtra("SELECTED_DISTRICT") ?: ""

        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        measurementText = findViewById(R.id.measurementText)
        toggleGridButton = findViewById(R.id.toggleGridButton)

        setupUI()
        initializeARScene()
        loadPlantModel() // Load 3D model

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

        toggleGridButton?.setOnClickListener {
            toggleGrid()
        }

        findViewById<Button>(R.id.switchModeButton)?.setOnClickListener {
            navigateToReport()
        }
    }

    private fun toggleGrid() {
        if (points.size == 4) {
            if (isGridVisible) {
                clearGrid()
            } else {
                createPlantGrid()
            }
            // Update button text based on grid visibility
            toggleGridButton?.text = if (isGridVisible) "Show Grid" else "Hide Grid"
        } else {
            // Show message if area is not yet defined
            measurementText?.text = "Please define the garden area first (4 points needed)"
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
                
                // Get plant type and spacing from intent
                val plantType = intent.getStringExtra("PLANT_TYPE") ?: "default"
                when (plantType.lowercase()) {
                    "carrot" -> plantSpacing = 0.15f
                    "cabbage" -> plantSpacing = 0.4f
                    "tomato" -> plantSpacing = 0.5f
                    "capsicum" -> plantSpacing = 0.3f
                    "brinjal" -> plantSpacing = 0.45f
                    "okra" -> plantSpacing = 0.3f
                    "radish" -> plantSpacing = 0.15f
                    "leeks" -> plantSpacing = 0.2f
                    "potato" -> plantSpacing = 0.3f
                    "onion" -> plantSpacing = 0.15f
                    "taro" -> plantSpacing = 0.5f
                    "manioc" -> plantSpacing = 0.8f
                    else -> plantSpacing = 0.3f
                }
                
                // Enable the toggle button
                toggleGridButton?.isEnabled = true
                
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

    private fun createPlantGrid() {
        if (points.size != 4) return
        
        clearGrid() // Clear existing grid
        
        // Calculate the grid dimensions based on the quadrilateral
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minZ = points.minOf { it.z }
        val maxZ = points.maxOf { it.z }
        
        // Calculate number of rows and columns based on plant spacing
        val gridWidth = maxX - minX
        val gridLength = maxZ - minZ
        val numCols = (gridWidth / plantSpacing).toInt()
        val numRows = (gridLength / plantSpacing).toInt()
        
        // Create grid points
        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val x = minX + (col * plantSpacing)
                val z = minZ + (row * plantSpacing)
                val position = Vector3(x, points[0].y, z)
                
                // Check if the point is inside the quadrilateral
                if (isPointInQuadrilateral(position)) {
                    placePlantModel(position)
                }
            }
        }
        
        isGridVisible = true
        // Update measurement text to show number of plants
        val plantCount = gridNodes.size
        measurementText?.text = "${measurementText?.text}\nNumber of Plants: $plantCount"
    }
    
    private fun clearGrid() {
        gridNodes.forEach { node ->
            node.setParent(null)
        }
        gridNodes.clear()
        isGridVisible = false
    }
    
    private fun isPointInQuadrilateral(point: Vector3): Boolean {
        var inside = false
        var j = points.size - 1
        
        for (i in points.indices) {
            if ((points[i].z > point.z) != (points[j].z > point.z) &&
                point.x < (points[j].x - points[i].x) * (point.z - points[i].z) /
                (points[j].z - points[i].z) + points[i].x) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
    
    private fun placePlantModel(position: Vector3) {
        if (plantModel == null) {
            // Log that we're using placeholder
            android.util.Log.d("AR_DEBUG", "Using cylinder placeholder because plantModel is null")
            // Create a simple cylinder as a placeholder for the plant
            MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(0f, 0.8f, 0f))
                .thenAccept { material ->
                    val cylinder = ShapeFactory.makeCylinder(0.05f, 0.2f, Vector3.zero(), material)
                    val node = Node().apply {
                        setParent(arFragment?.arSceneView?.scene)
                        localPosition = position
                        renderable = cylinder
                    }
                    gridNodes.add(node)
                }
        } else {
            // Log that we're using the actual model
            android.util.Log.d("AR_DEBUG", "Using actual 3D model")
            // Use the actual plant 3D model with rotation and scale
            val node = Node().apply {
                setParent(arFragment?.arSceneView?.scene)
                localPosition = position
                localScale = Vector3(0.1f, 0.1f, 0.1f)  // Reduced scale to make model smaller
                renderable = plantModel
                // Adjust rotation based on model orientation
                localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 270f)
            }
            gridNodes.add(node)
        }
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
            
            // Clear the grid
            clearGrid()

            points.clear()
            measurementText?.text = ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPlantModel() {
        try {
            // Using a simple, Android-resource-friendly name
            val resourceId = R.raw.plant3d
            android.util.Log.d("AR_DEBUG", "Loading 3D model with resource ID: $resourceId")
            
            ModelRenderable.builder()
                .setSource(this, resourceId)
                .build()
                .thenAccept { renderable ->
                    plantModel = renderable
                    // Log success
                    android.util.Log.d("AR_DEBUG", "Successfully loaded 3D model")
                    // Update UI to show success
                    runOnUiThread {
                        measurementText?.text = "3D model loaded successfully"
                    }
                }
                .exceptionally { throwable ->
                    // Log the specific error
                    android.util.Log.e("AR_DEBUG", "Error loading 3D model: ${throwable.message}")
                    throwable.printStackTrace()
                    
                    // Show detailed error in UI
                    runOnUiThread {
                        val errorMessage = when {
                            throwable.message?.contains("resource") == true -> 
                                "Model file not found. Make sure 'plant3d.glb' is in the raw folder"
                            throwable.message?.contains("permission") == true -> 
                                "Permission denied accessing model"
                            throwable.message?.contains("format") == true -> 
                                "Invalid model format. Make sure the GLB file is valid"
                            else -> "Error loading 3D model: ${throwable.message}"
                        }
                        measurementText?.text = errorMessage
                        android.util.Log.e("AR_DEBUG", errorMessage)
                    }
                    null
                }
        } catch (e: Exception) {
            // Log any other errors with more detail
            android.util.Log.e("AR_DEBUG", "Exception while loading 3D model: ${e.message}")
            android.util.Log.e("AR_DEBUG", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            e.printStackTrace()
            
            // Show error in UI
            runOnUiThread {
                measurementText?.text = "Error: ${e.message}"
            }
        }
    }
}