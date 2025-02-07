package com.example.arlandmeasuretest33

import android.graphics.Color
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

class MainActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private val placedAnchors = ArrayList<Anchor>()
    private val placedAnchorNodes = ArrayList<AnchorNode>()
    private val points = ArrayList<Vector3>()
    private var measurementText: TextView? = null
    private var currentMeasurementMode = MeasurementMode.LINE

    enum class MeasurementMode {
        LINE, QUADRILATERAL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        measurementText = findViewById(R.id.measurementText)

        setupUI()
        initializeARScene()
    }

    private fun setupUI() {
        findViewById<Button>(R.id.switchModeButton)?.setOnClickListener {
            currentMeasurementMode = if (currentMeasurementMode == MeasurementMode.LINE) {
                MeasurementMode.QUADRILATERAL
            } else {
                MeasurementMode.LINE
            }
            clearMeasurement()
        }

        findViewById<Button>(R.id.clearButton)?.setOnClickListener {
            clearMeasurement()
        }
    }

    private fun initializeARScene() {
        arFragment?.setOnTapArPlaneListener { hitResult, _, _ ->
            when (currentMeasurementMode) {
                MeasurementMode.LINE -> handleLineMeasurement(hitResult)
                MeasurementMode.QUADRILATERAL -> handleQuadrilateralMeasurement(hitResult)
            }
        }
    }

    private fun handleLineMeasurement(hitResult: HitResult) {
        if (points.size >= 2) {
            clearMeasurement()
            return  // Add return to ensure we start fresh
        }
        placePoint(hitResult)
        if (points.size == 2) {
            val distance = calculateDistance(points[0], points[1])
            measurementText?.text = String.format("Distance: %.2f cm", distance * 100)
        }
    }


    private fun handleQuadrilateralMeasurement(hitResult: HitResult) {
        if (points.size >= 4) {
            clearMeasurement()
            return  // Add return to ensure we start fresh
        }

        placePoint(hitResult)

        // Draw lines between points
        if (points.size > 1) {
            drawLine(points[points.size - 2], points[points.size - 1])

            // If we have 4 points, complete the quadrilateral
            if (points.size == 4) {
                // Draw the closing line
                drawLine(points[3], points[0])
                calculateAndDisplayQuadrilateralArea()
            }
        }
    }

    private fun placePoint(hitResult: HitResult) {
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
    }

    private fun drawLine(from: Vector3, to: Vector3) {
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
                }
            }
    }

    private fun calculateDistance(from: Vector3, to: Vector3): Float {
        return Vector3.subtract(to, from).length()
    }

    private fun calculateAndDisplayQuadrilateralArea() {
        if (points.size == 4) {
            val area = calculateQuadrilateralArea(points)
            val perimeter = calculatePerimeter(points)
            measurementText?.text = String.format("Area: %.2f cm², Perimeter: %.2f cm", area * 10000, perimeter * 100)
        }
    }

    private fun calculateQuadrilateralArea(points: List<Vector3>): Float {
        // Using the shoelace formula (Surveyor's formula)
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
            perimeter += calculateDistance(points[i], points[j])
        }
        return perimeter
    }

    private fun clearMeasurement() {
        // Remove anchors first
        placedAnchors.forEach { it.detach() }
        placedAnchors.clear()

        // Clear the scene of all nodes except the ArFragment
        arFragment?.arSceneView?.scene?.let { scene ->
            val children = scene.children.toList()
            children.forEach { node ->
                if (node !is ArFragment) {
                    scene.removeChild(node)
                }
            }
        }

        // Clear anchor nodes
        placedAnchorNodes.forEach { node ->
            node.anchor?.detach()
            node.setParent(null)
        }
        placedAnchorNodes.clear()

        // Clear points and reset text
        points.clear()
        measurementText?.text = ""
    }
}