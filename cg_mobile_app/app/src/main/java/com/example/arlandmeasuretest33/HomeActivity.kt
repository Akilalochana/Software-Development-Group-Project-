package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.arlandmeasuretest33.BannerSlideAdapter
import com.example.arlandmeasuretest33.BannerSlide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import android.widget.EditText
import java.util.*
import android.widget.Button
import android.util.Log


class HomeActivity : BaseActivity() {

    private val TAG = "HomeActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var dots: Array<View?>
    private var currentPage = 0
    private val sliderHandler = Handler(Looper.getMainLooper())

    // Track Firestore listeners to properly remove them
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var firestoreListener: ListenerRegistration? = null

    // UI component properties
    private lateinit var weatherCard: View
    private lateinit var reportCard: View
    private lateinit var tipsCard: View
    private lateinit var checkWeatherButton: View
    private lateinit var viewReportsButton: View
    private lateinit var getTipsButton: View
    private lateinit var startButton: View

    // Drawer components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var profileButton: ImageView
    private lateinit var closeMenuButton: ImageView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var userStatusText: TextView

    // List of banner images and their corresponding titles and descriptions
    private val bannerSlides = listOf(
        BannerSlide(
            R.drawable.garden_banner,
            "Grow Your Garden",
            "Start your gardening journey today"
        ),
        BannerSlide(
            R.drawable.garden_banner2,
            "Plant with Confidence",
            "Expert tips for successful gardening"
        ),
        BannerSlide(
            R.drawable.garden_banner3,
            "Harvest Season",
            "Enjoy the fruits of your labor"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeUIComponents()

        // Initialize drawer components
        initializeDrawer()

        // Set up the banner slideshow
        setupBannerSlideshow()

        // Set click listeners for all buttons
        setupButtonListeners()

        // Add auth state listener to track login/logout
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                updateUserInfo()
                setupRealTimeUpdates()
            } else {
                // User is signed out - remove listeners
                firestoreListener?.remove()
                // Update UI to show logged out state
                updateEmptyPlantUI()
            }
        }
        auth.addAuthStateListener(authStateListener!!)

        // Initial check of user authentication state
        if (auth.currentUser != null) {
            updateUserInfo()
            setupRealTimeUpdates()
        } else {
            updateEmptyPlantUI()
        }
    }

    private fun initializeUIComponents() {
        // Initialize chat input and send button
        val chatInput = findViewById<EditText>(R.id.chat_input)
        val sendButton = findViewById<CardView>(R.id.send_button)

        // Set click listener for send button
        sendButton.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isNotEmpty()) {
                // Process the message
                processMessage(message)
                // Clear the input field
                chatInput.text.clear()
            }
        }

        // Banner components
        viewPager = findViewById(R.id.banner_viewpager)
        dotsLayout = findViewById(R.id.dots_indicator)

        // Feature cards
        weatherCard = findViewById(R.id.weather_card)
        reportCard = findViewById(R.id.report_card)
        tipsCard = findViewById(R.id.tips_card)

        // Buttons - find them directly in the layout
        checkWeatherButton = findViewById(R.id.checkWeatherButton)
        viewReportsButton = findViewById(R.id.viewReportsButton)
        getTipsButton = findViewById(R.id.getTipsButton)
        startButton = findViewById(R.id.startButton)

        // Make sure the plant card is clickable
        val plantCard = findViewById<CardView>(R.id.plant_card)
        plantCard.setOnClickListener {
            // Navigate to My Plots screen when the plant card is clicked
            val intent = Intent(this, MyPlotsActivity::class.java)
            startActivity(intent)
        }
    }

    // New method to process messages
    private fun processMessage(message: String) {
        // You can either handle the chat directly here or navigate to ChatbotActivity
        val intent = Intent(this, ChatbotActivity::class.java)
        intent.putExtra("USER_MESSAGE", message)
        startActivity(intent)

        // Alternatively, display a toast for testing
        android.widget.Toast.makeText(
            this,
            "Message sent: $message",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun initializeDrawer() {
        // Initialize drawer components
        drawerLayout = findViewById(R.id.drawer_layout)
        profileButton = findViewById(R.id.profileButton)
        closeMenuButton = findViewById(R.id.closeMenuButton)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        userStatusText = findViewById(R.id.userStatusText)

        // Set up drawer profile information
        setupUserProfile()

        // Set up drawer click listeners
        setupDrawerListeners()
    }

    private fun setupUserProfile() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Get user data from Firestore
            db.collection("user_data").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Try to get name, if not available use email, then fallback to default
                        val userName = document.getString("name")
                            ?: document.getString("email")
                            ?: user.email
                            ?: user.displayName
                            ?: "Gardener"

                        // Extract the part before @ if it's an email
                        val displayName = if (userName.contains("@")) {
                            userName.split("@")[0]
                        } else {
                            userName
                        }

                        userNameText.text = displayName
                        userEmailText.text = user.email ?: "No email available"
                        userStatusText.text = "Active Gardener"
                        
                        // Load saved profile picture if available
                        val profilePicture = document.getLong("profilePicture")
                        if (profilePicture != null) {
                            val profileImage = findViewById<ImageView>(R.id.profileImage)
                            profileImage.setImageResource(profilePicture.toInt())
                            profileButton.setImageResource(profilePicture.toInt())
                            android.util.Log.d("HomeActivity", "Loaded profile picture from database: $profilePicture")
                        }
                    } else {
                        // If document doesn't exist, use email from Firebase Auth
                        val displayName = if (user.email != null && user.email!!.contains("@")) {
                            user.email!!.split("@")[0]
                        } else {
                            user.email ?: user.displayName ?: "Gardener"
                        }

                        userNameText.text = displayName
                        userEmailText.text = user.email ?: "No email available"
                        userStatusText.text = "Active Gardener"
                    }
                }
                .addOnFailureListener {
                    // Fallback to email from auth
                    val displayName = if (user.email != null && user.email!!.contains("@")) {
                        user.email!!.split("@")[0]
                    } else {
                        user.email ?: user.displayName ?: "Gardener"
                    }

                    userNameText.text = displayName
                    userEmailText.text = user.email ?: "No email available"
                    userStatusText.text = "Active Gardener"
                }
        } ?: run {
            userNameText.text = "Guest User"
            userEmailText.text = "No email available"
            userStatusText.text = "Guest"
        }
    }

    private fun setupDrawerListeners() {
        // Open drawer when profile button is clicked
        profileButton.setOnClickListener {
            openDrawer()
        }

        // Close drawer when close button is clicked
        closeMenuButton.setOnClickListener {
            closeDrawer()
        }

        // Add profile image click listener to change profile picture
        val profileImage = findViewById<ImageView>(R.id.profileImage)
        profileImage.setOnClickListener {
            showProfilePictureOptions()
        }

        // My Plots button click listener
        val myPlotsButton = findViewById<View>(R.id.myPlotsButton)
        myPlotsButton.setOnClickListener {
            // Navigate to My Plots screen
            val intent = Intent(this, MyPlotsActivity::class.java)
            startActivity(intent)
            closeDrawer()
        }

        // Logout button click listener
        val logoutButton = findViewById<View>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun openDrawer() {
        drawerLayout.openDrawer(Gravity.START)
    }

    private fun closeDrawer() {
        drawerLayout.closeDrawer(Gravity.START)
    }

    private fun logout() {
        // Show confirmation dialog
        android.app.AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Clear user session
                auth.signOut()

                // Clear any saved credentials
                val sharedPrefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                sharedPrefs.edit().clear().apply()

                // Redirect to welcome screen
                val intent = Intent(this, Welcome_screen::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateUserInfo() {
        // This method previously updated the greeting text
        // Now just update the user profile in the drawer
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Get user data from Firestore
            db.collection("user_data").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Try to get name, if not available use email, then fallback to default
                        val userName = document.getString("name")
                            ?: document.getString("email")
                            ?: user.email
                            ?: user.displayName
                            ?: "Gardener"

                        // Extract the part before @ if it's an email
                        val displayName = if (userName.contains("@")) {
                            userName.split("@")[0]
                        } else {
                            userName
                        }

                        userNameText.text = displayName
                        userEmailText.text = user.email ?: "No email available"
                        userStatusText.text = "Active Gardener"
                    } else {
                        // If document doesn't exist, use email from Firebase Auth
                        val displayName = if (user.email != null && user.email!!.contains("@")) {
                            user.email!!.split("@")[0]
                        } else {
                            user.email ?: user.displayName ?: "Gardener"
                        }

                        userNameText.text = displayName
                        userEmailText.text = user.email ?: "No email available"
                        userStatusText.text = "Active Gardener"
                    }
                }
                .addOnFailureListener {
                    // Fallback to email from auth
                    val displayName = if (user.email != null && user.email!!.contains("@")) {
                        user.email!!.split("@")[0]
                    } else {
                        user.email ?: user.displayName ?: "Gardener"
                    }

                    userNameText.text = displayName
                    userEmailText.text = user.email ?: "No email available"
                    userStatusText.text = "Active Gardener"
                }
        } ?: run {
            userNameText.text = "Guest User"
            userEmailText.text = "No email available"
            userStatusText.text = "Guest"
        }
    }

    private fun setupButtonListeners() {
        // Start button - main action button
        startButton.setOnClickListener {
            // Navigate to the location selection activity
            val intent = Intent(this, LocationSelectionActivity::class.java)
            startActivity(intent)
        }

        // Weather button - check weather information
        checkWeatherButton.setOnClickListener {
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

        // Reports button - view garden reports
        viewReportsButton.setOnClickListener {
            // Launch the Report activity
            val intent = Intent(this, Report::class.java)
            
            // If the user has a current garden selected, pass its information
            val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val currentGarden = sharedPreferences.getString("GARDEN_NAME", "")
            val currentPlant = sharedPreferences.getString("CURRENT_PLANT_NAME", "")
            
            if (!currentGarden.isNullOrEmpty()) {
                intent.putExtra("GARDEN_NAME", currentGarden)
            }
            
            if (!currentPlant.isNullOrEmpty()) {
                intent.putExtra("PLANT_NAME", currentPlant)
            }
            
            // Start the Report activity
            startActivity(intent)
        }

        // Tips button - get gardening tips
        getTipsButton.setOnClickListener {
            val intent = Intent(this, TipsActivity::class.java)
            startActivity(intent)
        }
        
        // Plant Information button - access plant information
        val getPlantInfoButton = findViewById<View>(R.id.getPlantInfoButton)
        getPlantInfoButton.setOnClickListener {
            try {
                val intent = Intent(this, PlantInformationActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("HomeActivity", "Error launching Plant Information: ${e.message}")
                android.widget.Toast.makeText(
                    this,
                    "Error launching Plant Information", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Setup real-time updates for plant data
    private fun setupRealTimeUpdates() {
        // Remove any existing listener first
        firestoreListener?.remove()

        val currentUser = auth.currentUser ?: return

        // Log authentication method to debug
        val authMethod = currentUser.providerData.firstOrNull()?.providerId ?: "unknown"
        android.util.Log.d("HomeActivity", "Auth method: $authMethod, UID: ${currentUser.uid}")

        // First, get all gardens for the current user
        val userGardensPath = "user_data/${currentUser.uid}/user_gardens"

        // Log the path we're checking
        android.util.Log.d("HomeActivity", "Checking path: $userGardensPath")
        
        // Set up a real-time listener for garden CHANGES, not just a one-time fetch
        firestoreListener = db.collection(userGardensPath)
            .addSnapshotListener { gardensSnapshot, gardensError ->
                if (gardensError != null) {
                    android.util.Log.e("HomeActivity", "Error listening for garden changes: ${gardensError.message}")
                    android.widget.Toast.makeText(
                        this,
                        "Error listening for garden updates",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (gardensSnapshot != null && !gardensSnapshot.isEmpty) {
                    // Get the most recently updated garden
                    val sortedGardens = gardensSnapshot.documents.sortedByDescending { doc ->
                        val lastUpdatedTimestamp = doc.data?.get("lastUpdated") as? com.google.firebase.Timestamp
                        val lastUpdatedLong = doc.data?.get("lastUpdated") as? Long
                        val dateAddedTimestamp = doc.data?.get("dateAdded") as? com.google.firebase.Timestamp
                        val dateAddedLong = doc.data?.get("dateAdded") as? Long
                        
                        when {
                            lastUpdatedTimestamp != null -> lastUpdatedTimestamp.toDate().time
                            lastUpdatedLong != null -> lastUpdatedLong
                            dateAddedTimestamp != null -> dateAddedTimestamp.toDate().time
                            dateAddedLong != null -> dateAddedLong
                            else -> 0L // Default value if no timestamp is available
                        }
                    }
                    
                    // Get the first garden - either most recently updated or created
                    val firstGardenDoc = sortedGardens.firstOrNull() ?: gardensSnapshot.documents[0]
                    val firstGardenId = firstGardenDoc.id
                    val gardenData = firstGardenDoc.data
                    
                    android.util.Log.d("HomeActivity", "Found garden: $firstGardenId with data: $gardenData")
                    
                    // Get garden area from the garden document
                    val gardenAreaValue = gardenData?.get("area") ?: gardenData?.get("areaSize")
                    
                    // Handle different possible number types from Firestore
                    val gardenArea = when (gardenAreaValue) {
                        is Double -> gardenAreaValue
                        is Float -> gardenAreaValue.toDouble()
                        is Long -> gardenAreaValue.toDouble()
                        is Int -> gardenAreaValue.toDouble()
                        is String -> gardenAreaValue.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    
                    // Update the area in the UI immediately
                    val areaTextView = findViewById<TextView>(R.id.area_size)
                    areaTextView.text = "${String.format("%.3f", gardenArea)} sq.m"

                    // Now listen to plants in this garden - this listener will be recreated if the garden changes
                    val userPlantsPath = "$userGardensPath/$firstGardenId/plants"
                    
                    // Set up the real-time listener for plants in this garden
                    db.collection(userPlantsPath)
                        .addSnapshotListener { plantsSnapshot, plantsError ->
                            if (plantsError != null) {
                                android.util.Log.e("HomeActivity", "Error listening for plants: ${plantsError.message}")
                                return@addSnapshotListener
                            }

                            if (plantsSnapshot != null && !plantsSnapshot.isEmpty) {
                                // Get the most recently added plant by sorting based on dateAdded
                                val sortedPlants = plantsSnapshot.documents.sortedByDescending { doc -> 
                                    val dateAddedTimestamp = doc.data?.get("dateAdded") as? com.google.firebase.Timestamp
                                    val dateAddedLong = doc.data?.get("dateAdded") as? Long
                                    
                                    when {
                                        dateAddedTimestamp != null -> dateAddedTimestamp.toDate().time
                                        dateAddedLong != null -> dateAddedLong
                                        else -> 0L // Default value if dateAdded is not available
                                    }
                                }
                                
                                // Get the latest plant (first in the sorted list)
                                val latestPlant = sortedPlants.firstOrNull()
                                
                                if (latestPlant != null) {
                                    android.util.Log.d("HomeActivity", "Found latest plant: ${latestPlant.id}, data: ${latestPlant.data}")
                                    runOnUiThread {
                                        updatePlantUI(latestPlant.data)
                                    }
                                } else {
                                    android.util.Log.d("HomeActivity", "No plants found after sorting")
                                    runOnUiThread {
                                        updateEmptyPlantUI()
                                    }
                                }
                            } else {
                                android.util.Log.d("HomeActivity", "No plants found")
                                runOnUiThread {
                                    updateEmptyPlantUI()
                                }
                            }
                        }
                } else {
                    // No gardens found
                    android.util.Log.d("HomeActivity", "No gardens found")
                    updateEmptyPlantUI()
                }
            }
    }

    // Function to update UI when no plants are found
    private fun updateEmptyPlantUI() {
        val plantNameTextView = findViewById<TextView>(R.id.plant_name)
        plantNameTextView.text = "No plants yet"

        val plantingDateTextView = findViewById<TextView>(R.id.planting_date)
        plantingDateTextView.text = "N/A"

        val harvestDateTextView = findViewById<TextView>(R.id.harvest_date)
        harvestDateTextView.text = "N/A"

        val areaTextView = findViewById<TextView>(R.id.area_size)
        areaTextView.text = "0.000 sq.m"

        val growthPeriodTextView = findViewById<TextView>(R.id.growth_period)
        growthPeriodTextView.text = "0 days"

        val plantIcon = findViewById<ImageView>(R.id.plant_icon)
        plantIcon.setImageResource(R.drawable.aloe_vera)
    }

    // Function to update the UI with plant data
    private fun updatePlantUI(plantData: Map<String, Any>?) {
        plantData?.let { data ->
            // Get plant name
            val plantName = data["name"] as? String ?: "Unknown Plant"

            // Find the plant name TextView in your plant card
            val plantNameTextView = findViewById<TextView>(R.id.plant_name)
            plantNameTextView.text = plantName

            val imageRef = data["imageRef"] as? String
            val plantIcon = findViewById<ImageView>(R.id.plant_icon)

            imageRef?.let { url ->
                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.aloe_vera)
                    .error(R.drawable.aloe_vera)
                    .into(plantIcon)
            } ?: run {
                plantIcon.setImageResource(R.drawable.aloe_vera)
            }

            // Get dateAdded, handling different possible formats
            val dateAddedTimestamp = data["dateAdded"] as? com.google.firebase.Timestamp
            val dateAddedLong = data["dateAdded"] as? Long
            
            val dateAdded: Long? = when {
                dateAddedTimestamp != null -> dateAddedTimestamp.toDate().time
                dateAddedLong != null -> dateAddedLong
                else -> null
            }

            val plantingDateTextView = findViewById<TextView>(R.id.planting_date)
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

            dateAdded?.let { timestamp ->
                val date = Date(timestamp)
                plantingDateTextView.text = sdf.format(date)
            } ?: run {
                plantingDateTextView.text = "Date unknown"
            }

            // Calculate and set expected harvest date
            val growthPeriod = data["growthPeriod"] as? Long 
                ?: data["growthPeriodDays"] as? Long ?: 0
            
            // Log the growth period value for debugging
            Log.d(TAG, "HomeActivity - growthPeriod: $growthPeriod (raw values: growthPeriod=${data["growthPeriod"]}, growthPeriodDays=${data["growthPeriodDays"]})")
            val harvestDateTextView = findViewById<TextView>(R.id.harvest_date)

            dateAdded?.let { timestamp ->
                val calendar = Calendar.getInstance()
                calendar.time = Date(timestamp)
                calendar.add(Calendar.DAY_OF_YEAR, growthPeriod.toInt())

                harvestDateTextView.text = sdf.format(calendar.time)
            } ?: run {
                harvestDateTextView.text = "Date unknown"
            }

            // Update growth period info
            val growthPeriodTextView = findViewById<TextView>(R.id.growth_period)
            growthPeriodTextView.text = "$growthPeriod days"
        } ?: run {
            updateEmptyPlantUI()
        }
    }

    private fun setupBannerSlideshow() {
        // Initialize ViewPager2 from the banner card
        viewPager = findViewById(R.id.banner_viewpager)
        dotsLayout = findViewById(R.id.dots_indicator)

        // Set up the adapter
        val bannerAdapter = BannerSlideAdapter(this, bannerSlides)
        viewPager.adapter = bannerAdapter

        // Set up dots indicator
        setupDots(0)

        // Add page change listener
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPage = position
                setupDots(position)
            }
        })

        // Start auto slideshow
        startAutoSlideshow()
    }

    private fun setupDots(position: Int) {
        // First clear any existing views
        dotsLayout.removeAllViews()

        // Create dots based on number of slides
        dots = arrayOfNulls(bannerSlides.size)

        for (i in bannerSlides.indices) {
            dots[i] = View(this)
            val dotSize = resources.getDimensionPixelSize(R.dimen.dot_size)
            val dotMargin = resources.getDimensionPixelSize(R.dimen.dot_margin)
            val params = LinearLayout.LayoutParams(dotSize, dotSize)
            params.setMargins(dotMargin, 0, dotMargin, 0)

            // Apply different background based on whether dot is active
            dots[i]?.layoutParams = params
            dots[i]?.setBackgroundResource(
                if (i == position) R.drawable.active_dot
                else R.drawable.inactive_dot
            )

            // Add dot to layout
            dotsLayout.addView(dots[i])
        }
    }

    private val sliderRunnable = Runnable {
        if (currentPage == bannerSlides.size - 1) {
            currentPage = 0
        } else {
            currentPage++
        }
        viewPager.currentItem = currentPage
    }

    private fun startAutoSlideshow() {
        // Schedule automatic sliding with 3 second intervals
        Timer().schedule(object : TimerTask() {
            override fun run() {
                sliderHandler.post(sliderRunnable)
            }
        }, 3000, 3000)
    }

    override fun onPause() {
        super.onPause()
        // Remove callbacks to prevent memory leaks
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onResume() {
        super.onResume()
        // Resume slideshow when activity resumes
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listeners to prevent memory leaks
        authStateListener?.let { auth.removeAuthStateListener(it) }
        firestoreListener?.remove()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }

    private fun showProfilePictureOptions() {
        // Create the dialog
        val dialog = android.app.Dialog(this)
        
        // Request feature before setting content view
        dialog.window?.requestFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.profile_image_selector_dialog)
        
        // Set transparent background and layout parameters
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        
        // Set dialog width to match parent with margins
        val displayMetrics = resources.displayMetrics
        val dialogWidth = (displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
        dialog.window?.setLayout(dialogWidth, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        
        // Get references to the card views
        val farmer1Card = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.farmer1_card)
        val farmer2Card = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.farmer2_card)
        val farmer3Card = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.farmer3_card)
        val farmer4Card = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.farmer4_card)
        val farmer5Card = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.farmer5_card)
        val farmer6Card = dialog.findViewById<androidx.cardview.widget.CardView>(R.id.farmer6_card)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)
        
        // Set click listeners for each farmer card
        farmer1Card.setOnClickListener {
            updateProfileImage(R.drawable.farmer1)
            dialog.dismiss()
        }
        
        farmer2Card.setOnClickListener {
            updateProfileImage(R.drawable.farmer2)
            dialog.dismiss()
        }
        
        farmer3Card.setOnClickListener {
            updateProfileImage(R.drawable.farmer3)
            dialog.dismiss()
        }
        
        farmer4Card.setOnClickListener {
            updateProfileImage(R.drawable.farmer4)
            dialog.dismiss()
        }
        
        farmer5Card.setOnClickListener {
            updateProfileImage(R.drawable.farmer5)
            dialog.dismiss()
        }
        
        farmer6Card.setOnClickListener {
            updateProfileImage(R.drawable.farmer6)
            dialog.dismiss()
        }
        
        // Set click listener for cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun updateProfileImage(resId: Int) {
        // Update both profile pictures
        val profileImage = findViewById<ImageView>(R.id.profileImage)
        profileImage.setImageResource(resId)
        profileButton.setImageResource(resId)
        
        // Save user preference if user is logged in
        auth.currentUser?.let { user ->
            val userRef = db.collection("user_data").document(user.uid)
            
            // Create a map with profile picture data
            val profileData = hashMapOf(
                "profilePicture" to resId,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            // Use set with merge option to create doc if it doesn't exist or update existing doc
            userRef.set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    android.util.Log.d("HomeActivity", "Profile picture saved to database")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("HomeActivity", "Error saving profile picture: ${e.message}")
                }
        }
        
        // Show confirmation toast
        android.widget.Toast.makeText(
            this,
            "Profile picture updated!",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

