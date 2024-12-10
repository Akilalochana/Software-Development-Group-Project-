package com.example.agrolankaplantseedrecommendation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.math.Vector3
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var arFragment: ArFragment
    private var measurePoints = mutableListOf<AnchorNode>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        setupAR()

        findViewById<Button>(R.id.plant_suggestion_button).setOnClickListener {
            startActivity(Intent(this, PlantSuggestionActivity::class.java))
        }
    }

    private fun setupAR() {
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane, _: android.view.MotionEvent ->
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            MaterialFactory.makeOpaqueWithColor(this, com.google.ar.sceneform.rendering.Color(android.graphics.Color.RED))
                .thenAccept { material ->
                    val sphere = ShapeFactory.makeSphere(0.1f, Vector3.zero(), material)
                    val node = TransformableNode(arFragment.transformationSystem)
                    node.renderable = sphere
                    node.setParent(anchorNode)
                    arFragment.arSceneView.scene.addChild(anchorNode)
                    node.select()
                }

            measurePoints.add(anchorNode)

            if (measurePoints.size >= 3) {
                calculateArea()
            }
        }
    }

    private fun calculateArea() {
        val area = calculatePolygonArea(measurePoints)
        Toast.makeText(this, "Area: ${String.format("%.2f", area)}m²", Toast.LENGTH_SHORT).show()
    }

    private fun calculatePolygonArea(points: List<AnchorNode>): Double {
        var area = 0.0
        for (i in points.indices) {
            val j = (i + 1) % points.size
            val p1 = points[i].anchor?.pose
            val p2 = points[j].anchor?.pose
            if (p1 != null && p2 != null) {
                area += p1.tx() * p2.tz() - p2.tx() * p1.tz()
            }
        }
        return abs(area) / 2.0
    }
}

//First commit

