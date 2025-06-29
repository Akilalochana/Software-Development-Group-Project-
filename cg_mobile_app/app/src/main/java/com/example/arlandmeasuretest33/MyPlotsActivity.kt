package com.example.arlandmeasuretest33

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.arlandmeasuretest33.adapters.GardenAdapter
import com.example.arlandmeasuretest33.adapters.ModernGardenAdapter
import com.example.arlandmeasuretest33.models.Garden
import com.example.arlandmeasuretest33.models.Plant
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class MyPlotsActivity : AppCompatActivity() {

    private lateinit var gardensRecyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var emptyView: View
    private lateinit var createPlotButton: MaterialButton
    private lateinit var fabAddGarden: FloatingActionButton
    private lateinit var gardenAdapter: ModernGardenAdapter

    private val gardens = mutableListOf<Garden>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_plots)

        // Initialize views
        gardensRecyclerView = findViewById(R.id.gardensRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        createPlotButton = findViewById(R.id.createPlotButton)
        fabAddGarden = findViewById(R.id.fabAddGarden)        // Setup RecyclerView with modern design
        gardensRecyclerView.layoutManager = LinearLayoutManager(this)
        gardenAdapter = ModernGardenAdapter(
            gardens, 
            onGardenClick = { garden ->
                // Handle garden click - navigate to garden detail view
                Log.d("MyPlotsActivity", "Garden clicked: ${garden.name}")
                
                // Add more detailed logging before navigating
                Log.d("MyPlotsActivity", "Garden ID: ${garden.id}, Garden Name: ${garden.name}")
                Log.d("MyPlotsActivity", "Is Legacy Path: ${garden.id.equals(garden.name, ignoreCase = true)}")
                
                val intent = android.content.Intent(this, GardenDetailActivity::class.java).apply {
                    putExtra("GARDEN_ID", garden.id)
                    putExtra("GARDEN_NAME", garden.name)  // Also pass the name for additional reference
                    // Check if this is from the alternate/legacy path
                    putExtra("IS_LEGACY_PATH", garden.id.equals(garden.name, ignoreCase = true))
                }
                startActivity(intent)
            },
            onDeleteClick = { garden ->
                // Handle delete garden click
                showDeleteGardenConfirmation(garden)
            }
        )
        gardensRecyclerView.adapter = gardenAdapter

        // Setup back button
        findViewById<View>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Setup create new garden buttons
        createPlotButton.setOnClickListener {
            // Navigate to garden creation via LocationSelectionActivity
            Log.d("MyPlotsActivity", "Create plot button clicked, navigating to LocationSelectionActivity")
            val intent = android.content.Intent(this, LocationSelectionActivity::class.java)
            startActivity(intent)
        }

        fabAddGarden.setOnClickListener {
            // Same as create plot button
            Log.d("MyPlotsActivity", "FAB Add garden clicked, navigating to LocationSelectionActivity")
            val intent = android.content.Intent(this, LocationSelectionActivity::class.java)
            startActivity(intent)
        }

        // Load user gardens from Firebase
        loadUserGardens()
    }    private fun loadUserGardens() {
        if (currentUser == null) {
            showEmptyView()
            return
        }

        showLoadingView()

        val userId = currentUser.uid
        
        // First check if there are gardens in the user_gardens collection directly
        db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .get()
            .addOnSuccessListener { gardensSnapshot ->
                if (!gardensSnapshot.isEmpty) {
                    // Process gardens in the standard location
                    processGardenSnapshot(gardensSnapshot)
                } else {
                    // If no gardens in the standard location, check if the user has data in the Firestore structure
                    // shown in the screenshots (user_gardens/rd/plants/Manioc)
                    Log.d("MyPlotsActivity", "No gardens found in standard location, checking alternate structure")
                    checkUserGardensInAltPath(userId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyPlotsActivity", "Error loading user gardens", e)
                // Try alternate path as fallback
                checkUserGardensInAltPath(userId)
            }
    }
    
    private fun checkUserGardensInAltPath(userId: String) {
        // Check the structure from the screenshots (user_gardens/rd/plants/Manioc)
        Log.d("MyPlotsActivity", "Trying to access alternate path user_gardens")

        try {
            db.collection("user_gardens")
                .get()
                .addOnSuccessListener { userGardensSnapshot ->
                    if (userGardensSnapshot.isEmpty) {
                        Log.d("MyPlotsActivity", "No user_gardens collection found")
                        showEmptyView()
                        return@addOnSuccessListener
                    }

                    val userGardens = mutableListOf<Garden>()
                    var processedCount = 0

                    // Go through each user garden document
                    for (gardenDoc in userGardensSnapshot.documents) {
                        val gardenName = gardenDoc.id
                        Log.d("MyPlotsActivity", "Found garden in alternate path: $gardenName")

                        // Create a garden object
                        val garden = Garden(
                            id = gardenDoc.id,
                            name = gardenName,
                            createdDate = gardenDoc.getTimestamp("createdAt")?.toDate() ?: Date(),
                            areaSize = gardenDoc.getDouble("area") ?: gardenDoc.getDouble("areaSize") ?: 0.0
                        )

                        // Just add the garden without trying to access plants
                        // This avoids potential permission issues
                        userGardens.add(garden)
                        processedCount++

                        // Only try to access plants if we have the appropriate permissions
                        if (gardenDoc.contains("hasPlants") && gardenDoc.getBoolean("hasPlants") == true) {
                            Log.d("MyPlotsActivity", "Garden has plants, attempting to retrieve them")
                            tryLoadPlantsForGarden(garden, gardenName, userGardens)
                        }

                        // Process gardens even if we can't load plants
                        if (processedCount == userGardensSnapshot.size()) {
                            updateGardensUI(userGardens)
                        }
                    }

                    // Handle the case where there are no garden documents
                    if (userGardensSnapshot.isEmpty) {
                        showEmptyView()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MyPlotsActivity", "Error loading alternate user gardens path", e)
                    // Show a more helpful error message in the UI
                    handleFirestoreError(e)
                }
        } catch (e: Exception) {
            Log.e("MyPlotsActivity", "Exception when trying to access alternate path", e)
            showEmptyView()
        }
    }

    private fun tryLoadPlantsForGarden(garden: Garden, gardenName: String, userGardens: MutableList<Garden>) {
        try {
            db.collection("user_gardens")
                .document(gardenName)
                .collection("plants")
                .get()
                .addOnSuccessListener { plantsSnapshot ->
                    for (plantDoc in plantsSnapshot.documents) {
                        Log.d("MyPlotsActivity", "Found plant: ${plantDoc.id}")
                        // Get plant data
                        val plant = Plant(
                            id = plantDoc.id,
                            name = plantDoc.id,
                            plantedDate = plantDoc.getTimestamp("dateAdded")?.toDate() ?: Date(),
                            harvestDate = null,
                            growthPeriodDays = plantDoc.getLong("growthPeriod")?.toInt() ?: 0,
                            imageRef = plantDoc.getString("imageRef") ?: ""
                        )
                        garden.plants.add(plant)
                    }

                    // Update UI with the plants information
                    updateGardensUI(userGardens)
                }
                .addOnFailureListener { e ->
                    Log.e("MyPlotsActivity", "Error loading plants for garden $gardenName: ${e.message}", e)
                    // Still update UI without plants
                    updateGardensUI(userGardens)
                }
        } catch (e: Exception) {
            Log.e("MyPlotsActivity", "Exception when trying to load plants", e)
        }
    }

    private fun updateGardensUI(userGardens: List<Garden>) {
        gardens.clear()
        gardens.addAll(userGardens)
        gardenAdapter.notifyDataSetChanged()

        if (gardens.isNotEmpty()) {
            showGardensView()
        } else {
            showEmptyView()
        }
    }

    private fun handleFirestoreError(e: Exception) {
        Log.e("MyPlotsActivity", "Firestore error: ${e.message}", e)

        if (e.message?.contains("PERMISSION_DENIED") == true) {
            // Handle permission denied specifically
            runOnUiThread {
                emptyView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                gardensRecyclerView.visibility = View.GONE

                // If you have a text view in your empty view to show messages
                // findViewById<TextView>(R.id.emptyViewText).text = "Unable to access garden data. Please check your connection and permissions."
            }
        } else {
            showEmptyView()
        }
    }

    private fun processGardenSnapshot(gardensSnapshot: com.google.firebase.firestore.QuerySnapshot) {
        val userGardens = mutableListOf<Garden>()

        if (gardensSnapshot.isEmpty) {
            showEmptyView()
        } else {
            // Make sure currentUser is not null before proceeding
            if (currentUser == null) {
                showEmptyView()
                return
            }
            
            val userId = currentUser.uid
            
            for (gardenDoc in gardensSnapshot) {
                // Create a garden object from the document
                val gardenName = gardenDoc.id
                val createdDate = gardenDoc.getTimestamp("createdDate")?.toDate() ?: Date()
                val areaSize = gardenDoc.getDouble("areaSize") ?: gardenDoc.getDouble("area") ?: 0.0

                val garden = Garden(
                    id = gardenDoc.id,
                    name = gardenName,
                    createdDate = createdDate,
                    areaSize = areaSize
                )

                // Load plants for this garden
                db.collection("user_data")
                    .document(userId)
                    .collection("user_gardens")
                    .document(gardenName)
                    .collection("plants")
                    .get()
                    .addOnSuccessListener { plantsSnapshot ->
                        for (plantDoc in plantsSnapshot) {                            val plant = Plant(
                                id = plantDoc.id,
                                name = plantDoc.id,
                                plantedDate = plantDoc.getTimestamp("plantedDate")?.toDate(),
                                harvestDate = plantDoc.getTimestamp("harvestDate")?.toDate(),
                                growthPeriodDays = plantDoc.getLong("growthPeriodDays")?.toInt() ?: 0,
                                imageRef = plantDoc.getString("imageRef") ?: ""
                            )
                            garden.plants.add(plant)
                        }

                        // Add to the list and notify adapter
                        if (!userGardens.contains(garden)) {
                            userGardens.add(garden)
                            gardens.clear()
                            gardens.addAll(userGardens)
                            gardenAdapter.notifyDataSetChanged()
                            showGardensView()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyPlotsActivity", "Error loading plants for garden $gardenName", e)
                    }
            }
        }
    }

    private fun showLoadingView() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        gardensRecyclerView.visibility = View.GONE
    }

    private fun showEmptyView() {
        progressBar.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        gardensRecyclerView.visibility = View.GONE
    }

    private fun showGardensView() {
        progressBar.visibility = View.GONE
        emptyView.visibility = View.GONE
        gardensRecyclerView.visibility = View.VISIBLE
    }

    /**
     * Shows a confirmation dialog before deleting a garden
     */
    private fun showDeleteGardenConfirmation(garden: Garden) {
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete ${garden.name}")
            .setMessage("Are you sure you want to delete this garden? All plants in this garden will also be deleted.")
            .setPositiveButton("Delete") { _, _ -> 
                deleteGardenFromFirebase(garden)
            }
            .setNegativeButton("Cancel", null)
            
        // Show dialog and get its reference
        val dialog = dialogBuilder.create()
        dialog.show()
        
        // Set button colors to green to match app theme
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
    }
    
    /**
     * Deletes a garden from Firebase
     */
    private fun deleteGardenFromFirebase(garden: Garden) {
        val userId = currentUser?.uid
        if (userId == null) {
            Log.e("MyPlotsActivity", "Cannot delete garden: User not authenticated")
            android.widget.Toast.makeText(this, "You need to be logged in to delete gardens", android.widget.Toast.LENGTH_SHORT).show()
            showGardensView()
            return
        }

        showLoadingView()
        
        // Print the full path information for debugging
        Log.d("MyPlotsActivity", "Current user ID: $userId")
        Log.d("MyPlotsActivity", "Garden to delete - Name: ${garden.name}, ID: ${garden.id}")
        Log.d("MyPlotsActivity", "Expected Firebase path: /user_data/$userId/user_gardens/${garden.id}")
        
        // Find the garden in our local list for UI updates
        val indexToRemove = gardens.indexOfFirst { it.id == garden.id }
        
        // Direct path to the garden document - always use user_data/{userId}/user_gardens/{gardenId}
        val gardenRef = db.collection("user_data")
            .document(userId)
            .collection("user_gardens")
            .document(garden.id)
        
        // Log the exact Firestore reference path
        Log.d("MyPlotsActivity", "Attempting to delete garden document at path: ${gardenRef.path}")
        
        // First check if this document exists
        gardenRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    Log.d("MyPlotsActivity", "Garden document exists, proceeding with deletion")
                    
                    // Document exists, proceed with deletion
                    gardenRef.delete()
                        .addOnSuccessListener {
                            Log.d("MyPlotsActivity", "GARDEN DELETED SUCCESSFULLY: ${garden.name}")
                            
                            // Show success toast
                            android.widget.Toast.makeText(this, "${garden.name} successfully deleted", android.widget.Toast.LENGTH_SHORT).show()
                            
                            // Remove from local list and update UI
                            if (indexToRemove >= 0 && indexToRemove < gardens.size) {
                                gardens.removeAt(indexToRemove)
                                gardenAdapter.notifyItemRemoved(indexToRemove)
                                
                                if (gardens.isEmpty()) {
                                    showEmptyView()
                                } else {
                                    showGardensView()
                                }
                            } else {
                                // Something changed in our local list, reload everything
                                loadUserGardens()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MyPlotsActivity", "ERROR DELETING GARDEN: ${e.message}")
                            android.widget.Toast.makeText(this, "Error deleting garden: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            showGardensView()
                        }
                } else {
                    // Document doesn't exist - try deleting where garden.name is the document ID
                    Log.d("MyPlotsActivity", "Garden document not found with ID ${garden.id}, trying with name ${garden.name}")
                    
                    val alternateRef = db.collection("user_data")
                        .document(userId)
                        .collection("user_gardens")
                        .document(garden.name)
                    
                    Log.d("MyPlotsActivity", "Trying alternate path: ${alternateRef.path}")
                    
                    alternateRef.delete()
                        .addOnSuccessListener {
                            Log.d("MyPlotsActivity", "GARDEN DELETED SUCCESSFULLY using name as ID: ${garden.name}")
                            android.widget.Toast.makeText(this, "${garden.name} successfully deleted", android.widget.Toast.LENGTH_SHORT).show()
                            
                            // Remove from local list and update UI
                            if (indexToRemove >= 0 && indexToRemove < gardens.size) {
                                gardens.removeAt(indexToRemove)
                                gardenAdapter.notifyItemRemoved(indexToRemove)
                                
                                if (gardens.isEmpty()) {
                                    showEmptyView()
                                } else {
                                    showGardensView()
                                }
                            } else {
                                // Something changed in our local list, reload everything
                                loadUserGardens()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MyPlotsActivity", "ERROR DELETING GARDEN with name as ID: ${e.message}")
                            android.widget.Toast.makeText(this, "Error: Could not find garden to delete", android.widget.Toast.LENGTH_LONG).show()
                            showGardensView()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyPlotsActivity", "Error checking if garden exists: ${e.message}")
                android.widget.Toast.makeText(this, "Error checking if garden exists: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                showGardensView()
            }
    }
}
