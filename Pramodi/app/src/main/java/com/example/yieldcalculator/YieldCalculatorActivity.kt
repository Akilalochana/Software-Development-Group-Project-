import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.math.Vector3
import kotlin.math.sqrt

class YieldCalculatorActivity : AppCompatActivity() {
    private lateinit var arFragment: ArFragment
    private var points: MutableList<Vector3> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yield_calculator)

        // Initialize AR fragment
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment

        // Tap to place points for area measurement
        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _, _ ->
            if (points.size < 2) {
                val point = hitResult.hitPose.translation
                points.add(Vector3(point[0], point[1], point[2]))
                placeMarker(hitResult)
                if (points.size == 2) {
                    calculateArea()
                }
            }
        }
    }

    private fun placeMarker(hitResult: HitResult) {
        MaterialFactory.makeOpaqueWithColor(this, Color(android.graphics.Color.RED))
            .thenAccept { material ->
                val model = com.google.ar.sceneform.rendering.ShapeFactory.makeSphere(0.02f, Vector3.zero(), material)
                val node = com.google.ar.sceneform.ux.TransformableNode(arFragment.transformationSystem)
                node.renderable = model
                node.setParent(arFragment.arSceneView.scene)
                node.worldPosition = Vector3(hitResult.hitPose.tx(), hitResult.hitPose.ty(), hitResult.hitPose.tz())
            }
    }

    private fun calculateArea() {
        if (points.size == 2) {
            val p1 = points[0]
            val p2 = points[1]

            // Distance formula to calculate the area (approximate square field)
            val distance = sqrt(
                ((p2.x - p1.x) * (p2.x - p1.x)) +
                        ((p2.z - p1.z) * (p2.z - p1.z))
            )

            // Assuming square area for simplicity
            val area = distance * distance

            // Pass area to yield calculation
            calculateYield(area)

            // Reset for another calculation
            points.clear()
        }
    }

    private fun calculateYield(area: Double) {
        val plantsPerSquareMeter = 4 // Average plant density
        val averageYieldPerPlant = 1.2 // Yield per plant in kilograms

        val totalPlants = area * plantsPerSquareMeter
        val totalYield = totalPlants * averageYieldPerPlant

        // Display results
        Toast.makeText(this, "Area: $area m²\nYield: ${"%.2f".format(totalYield)} kg", Toast.LENGTH_LONG).show()
    }
}
