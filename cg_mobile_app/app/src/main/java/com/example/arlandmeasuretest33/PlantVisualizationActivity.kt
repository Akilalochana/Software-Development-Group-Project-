package com.example.arlandmeasuretest33

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.google.ar.sceneform.math.Quaternion
class PlantVisualizationActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment
    private var selectedPlant: PlantType = PlantType.CABBAGE
    private var measuredArea: Float = 0f

    enum class PlantType {
        CABBAGE, CARROT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_visualization)

        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment

        // Set up tap listener
        arFragment.setOnTapArPlaneListener { hitResult, plane, _ ->
            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                return@setOnTapArPlaneListener
            }

            // Measure area of the detected plane
            measuredArea = calculatePlaneArea(plane)

            // Place plants based on measured area
            placePlants(hitResult, plane)
        }
    }

    private fun calculatePlaneArea(plane: Plane): Float {
        val extentX = plane.extentX
        val extentZ = plane.extentZ
        return extentX * extentZ // Simple rectangular area calculation
    }

    private fun placePlants(hitResult: HitResult, plane: Plane) {
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)

        // Calculate number of plants based on area
        // Assumption: 1 cabbage needs 0.25 sq meters, 1 carrot needs 0.04 sq meters
        val numberOfPlants = when (selectedPlant) {
            PlantType.CABBAGE -> (measuredArea / 0.25f).roundToInt().coerceAtMost(20)
            PlantType.CARROT -> (measuredArea / 0.04f).roundToInt().coerceAtMost(50)
        }

        Toast.makeText(this, "Placing $numberOfPlants plants (Area: ${measuredArea.toInt()} m²)", Toast.LENGTH_SHORT).show()

        // Place plants in a grid pattern
        val spacing = when (selectedPlant) {
            PlantType.CABBAGE -> 0.5f
            PlantType.CARROT -> 0.2f
        }

        val gridSize = sqrt(numberOfPlants.toDouble()).roundToInt()
        val offset = (gridSize * spacing) / 2

        var plantCounter = 0

        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                if (plantCounter < numberOfPlants) {
                    when (selectedPlant) {
                        PlantType.CABBAGE -> createCabbage(anchorNode, row * spacing - offset, col * spacing - offset)
                        PlantType.CARROT -> createCarrot(anchorNode, row * spacing - offset, col * spacing - offset)
                    }
                    plantCounter++
                }
            }
        }
    }

    private fun createCabbage(parentNode: AnchorNode, x: Float, z: Float) {
        // Create different green shades for cabbage leaves
        val darkGreen = com.google.ar.sceneform.rendering.Color(0.0f, 0.5f, 0.0f)
        val mediumGreen = com.google.ar.sceneform.rendering.Color(0.1f, 0.6f, 0.1f)
        val lightGreen = com.google.ar.sceneform.rendering.Color(0.2f, 0.7f, 0.2f)

        val cabbageNode = Node()
        cabbageNode.setParent(parentNode)
        cabbageNode.localPosition = Vector3(x, 0.1f, z)

        // Create and add base leaves (larger flatter spheres)
        MaterialFactory.makeOpaqueWithColor(this, darkGreen).thenAccept { material ->
            val baseLeaves = ShapeFactory.makeSphere(0.18f, Vector3(0f, 0.02f, 0f), material)
            val baseLeavesNode = Node()
            baseLeavesNode.setParent(cabbageNode)
            baseLeavesNode.renderable = baseLeaves
            baseLeavesNode.localScale = Vector3(1.0f, 0.4f, 1.0f) // Flatten it
        }

        // Create and add middle leaves
        MaterialFactory.makeOpaqueWithColor(this, mediumGreen).thenAccept { material ->
            val middleLeaves = ShapeFactory.makeSphere(0.15f, Vector3(0f, 0f, 0f), material)
            val middleLeavesNode = Node()
            middleLeavesNode.setParent(cabbageNode)
            middleLeavesNode.renderable = middleLeaves
            middleLeavesNode.localPosition = Vector3(0f, 0.1f, 0f)
            middleLeavesNode.localScale = Vector3(0.9f, 0.6f, 0.9f)
        }

        // Create and add top leaves
        MaterialFactory.makeOpaqueWithColor(this, lightGreen).thenAccept { material ->
            val topLeaves = ShapeFactory.makeSphere(0.12f, Vector3(0f, 0f, 0f), material)
            val topLeavesNode = Node()
            topLeavesNode.setParent(cabbageNode)
            topLeavesNode.renderable = topLeaves
            topLeavesNode.localPosition = Vector3(0f, 0.15f, 0f)
        }

        // Add leaf detail using small spheres for texture
        for (i in 0 until 12) {
            val angle = (i * 30) * (Math.PI / 180)
            val radius = 0.13f + (i % 3) * 0.02f
            val leafX = radius * Math.cos(angle).toFloat()
            val leafZ = radius * Math.sin(angle).toFloat()
            val leafY = 0.08f + (i % 4) * 0.03f

            MaterialFactory.makeOpaqueWithColor(this, if (i % 2 == 0) lightGreen else mediumGreen)
                .thenAccept { material ->
                    val leafDetail = ShapeFactory.makeSphere(0.04f, Vector3(0f, 0f, 0f), material)
                    val leafNode = Node()
                    leafNode.setParent(cabbageNode)
                    leafNode.renderable = leafDetail
                    leafNode.localPosition = Vector3(leafX, leafY, leafZ)
                    leafNode.localScale = Vector3(1.0f, 0.6f, 1.0f)
                }
        }
    }

    private fun createCarrot(parentNode: AnchorNode, x: Float, z: Float) {
        val carrotNode = Node()
        carrotNode.setParent(parentNode)
        carrotNode.localPosition = Vector3(x, 0.0f, z)

        // Carrot root materials - gradient of orange shades
        val darkOrange = com.google.ar.sceneform.rendering.Color(0.8f, 0.3f, 0.0f)
        val lightOrange = com.google.ar.sceneform.rendering.Color(1.0f, 0.6f, 0.2f)
        val brownTop = com.google.ar.sceneform.rendering.Color(0.5f, 0.3f, 0.1f)

        // Create carrot root (tapered cylinder/cone)
        // Create carrot root using multiple cylinders
        for (i in 0 until 4) {
            val section = 1.0f - (i * 0.25f)  // Decreasing size factor
            val yOffset = i * 0.05f

            MaterialFactory.makeOpaqueWithColor(this, lightOrange).thenAccept { material ->
                val radius = 0.06f * section  // Decreasing radius
                val height = 0.05f

                val section = ShapeFactory.makeCylinder(
                    radius,
                    height,
                    Vector3(0f, 0f, 0f),
                    material
                )

                val sectionNode = Node()
                sectionNode.setParent(carrotNode)
                sectionNode.renderable = section
                sectionNode.localPosition = Vector3(0f, 0.11f - yOffset, 0f)
            }
        }

        // Create carrot top connection (small brown cylinder)
        MaterialFactory.makeOpaqueWithColor(this, brownTop).thenAccept { material ->
            val topConnection = ShapeFactory.makeCylinder(
                0.03f,
                0.02f,
                Vector3(0f, 0f, 0f),
                material
            )
            val connectionNode = Node()
            connectionNode.setParent(carrotNode)
            connectionNode.renderable = topConnection
            connectionNode.localPosition = Vector3(0f, 0.22f, 0f)
        }

        // Create carrot greens
        val leafGreen = com.google.ar.sceneform.rendering.Color(0.1f, 0.8f, 0.1f)
        val darkLeafGreen = com.google.ar.sceneform.rendering.Color(0.0f, 0.6f, 0.0f)

        // Create multiple stems with slight variations for realism
        for (i in 0 until 6) {
            val angle = (i * 60) * (Math.PI / 180)
            val stemX = 0.02f * Math.cos(angle).toFloat()
            val stemZ = 0.02f * Math.sin(angle).toFloat()
            val height = 0.15f + (i % 3) * 0.05f

            MaterialFactory.makeOpaqueWithColor(this, if (i % 2 == 0) leafGreen else darkLeafGreen)
                .thenAccept { material ->
                    val stem = ShapeFactory.makeCylinder(
                        0.005f, // Very thin
                        height,
                        Vector3(0f, height/2, 0f), // Center the cylinder
                        material
                    )
                    val stemNode = Node()
                    stemNode.setParent(carrotNode)
                    stemNode.renderable = stem
                    stemNode.localPosition = Vector3(stemX, 0.22f, stemZ)

                    // Rotate stem to fan out
                    val outwardTilt = 15f + (i * 3)
                    stemNode.localRotation = Quaternion.axisAngle(
                        Vector3(-stemZ * 10, 0f, stemX * 10), outwardTilt)

                    // Add small leaves at the top of each stem
                    for (j in 0 until 3) {
                        val leafHeight = height * (0.6f + j * 0.15f)
                        val leafNode = Node()
                        leafNode.setParent(stemNode)

                        MaterialFactory.makeOpaqueWithColor(this, if ((i+j) % 2 == 0) leafGreen else darkLeafGreen)
                            .thenAccept { leafMaterial ->
                                val leaf = ShapeFactory.makeCylinder(
                                    0.01f,
                                    0.04f,
                                    Vector3(0f, 0f, 0f),
                                    leafMaterial
                                )
                                leafNode.renderable = leaf
                                leafNode.localPosition = Vector3(0f, leafHeight, 0f)
                                leafNode.localRotation = Quaternion.axisAngle(Vector3(0f, 0f, 1f), 90f)
                                leafNode.localScale = Vector3(1f, 0.2f, 1f) // Flatten to look like a leaf
                            }
                    }
                }
        }
    }

    // UI methods to select plant type
    fun onCabbageSelected(view: View) {
        selectedPlant = PlantType.CABBAGE
        Toast.makeText(this, "Cabbage selected", Toast.LENGTH_SHORT).show()
    }

    fun onCarrotSelected(view: View) {
        selectedPlant = PlantType.CARROT
        Toast.makeText(this, "Carrot selected", Toast.LENGTH_SHORT).show()
    }
}