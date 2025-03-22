package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.widget.Toast

class HomeActivity : AppCompatActivity() {

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
    private lateinit var greetingText: TextView
    private lateinit var weatherCard: View
    private lateinit var reportCard: View
    private lateinit var tipsCard: View
    private lateinit var checkWeatherButton: View
    private lateinit var viewReportsButton: View
    private lateinit var getTipsButton: View
    private lateinit var getPlantInfoButton: View
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
        enableEdgeToEdge()
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

    private fun enableEdgeToEdge() {
        // Implementation for edge-to-edge display
        // For newer Android versions, you might use WindowCompat.setDecorFitsSystemWindows
    }

    private fun initializeUIComponents() {
        // User info components
        greetingText = findViewById(R.id.greetingText)

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
        getPlantInfoButton = findViewById(R.id.getPlantInfoButton)
        startButton = findViewById(R.id.startButton)

        // Make sure the plant card is clickable
        val plantCard = findViewById<CardView>(R.id.plant_card)
        plantCard.setOnClickListener {
            // Navigate to plant detail activity - commented out until PlantDetailActivity is created
            // val intent = Intent(this, PlantDetailActivity::class.java)
            // startActivity(intent)

            // Show a message instead
            android.widget.Toast.makeText(
                this,
                "Plant details will be shown here",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // Find the Plant Information button if it exists
        val plantInfoButton = findViewById<View>(R.id.getPlantInfoButton)
        plantInfoButton?.setOnClickListener {
            try {
                // Launch PlantInformationActivity
                val intent = Intent(this, PlantInformationActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error launching Plant Information: ${e.message}")
                Toast.makeText(this, "Plant Information feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }
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

                        // Update drawer profile info
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

    private fun setupDrawerListeners() {
        // Open drawer when profile button is clicked
        profileButton.setOnClickListener {
            openDrawer()
        }

        // Close drawer when close button is clicked
        closeMenuButton.setOnClickListener {
            closeDrawer()
        }

        // Logout button click listener
        val logoutButton = findViewById<View>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logout()
        }
    }

    private fun openDrawer() {
        drawerLayout.openDrawer(Gravity.RIGHT)
    }

    private fun closeDrawer() {
        drawerLayout.closeDrawer(Gravity.RIGHT)
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

                        greetingText.text = "Hello, $displayName!"
                    } else {
                        // If document doesn't exist, use email from Firebase Auth
                        val displayName = if (user.email != null && user.email!!.contains("@")) {
                            user.email!!.split("@")[0]
                        } else {
                            user.email ?: user.displayName ?: "Gardener"
                        }

                        greetingText.text = "Hello, $displayName!"
                    }
                }
                .addOnFailureListener {
                    // Fallback to email from auth
                    val displayName = if (user.email != null && user.email!!.contains("@")) {
                        user.email!!.split("@")[0]
                    } else {
                        user.email ?: user.displayName ?: "Gardener"
                    }

                    greetingText.text = "Hello, $displayName!"
                }
        } ?: run {
            greetingText.text = "Hello, Gardener!"
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
            // Uncommented for when ReportsActivity is available
            // val intent = Intent(this, ReportsActivity::class.java)
            // startActivity(intent)

            // Show a message instead
            android.widget.Toast.makeText(
                this,
                "Reports feature coming soon",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // Tips button - get gardening tips
        getTipsButton.setOnClickListener {
            val intent = Intent(this, TipsActivity::class.java)
            startActivity(intent)
        }
        
        // Plant Information button - access plant information
        getPlantInfoButton.setOnClickListener {
            val intent = Intent(this, PlantInformationActivity::class.java)
            startActivity(intent)
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

        // Check if the user has any gardens
        db.collection(userGardensPath)
            .get()
            .addOnSuccessListener { gardens ->
                if (!gardens.isEmpty) {
                    // Get the first garden (or you could show a selection to the user)
                    val firstGarden = gardens.documents[0].id
                    android.util.Log.d("HomeActivity", "Found garden: $firstGarden")

                    // Now listen to plants in this garden
                    val userPlantsPath = "$userGardensPath/$firstGarden/plants"

                    // Set up the real-time listener for plants in this garden
                    firestoreListener = db.collection(userPlantsPath)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                android.util.Log.e("HomeActivity", "Error listening for plants: ${error.message}")
                                android.widget.Toast.makeText(
                                    this,
                                    "Error listening for plant updates: ${error.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@addSnapshotListener
                            }

                            if (snapshot != null && !snapshot.isEmpty) {
                                // Get the first plant document
                                val plantDoc = snapshot.documents[0]
                                android.util.Log.d("HomeActivity", "Found plant: ${plantDoc.id}, data: ${plantDoc.data}")
                                updatePlantUI(plantDoc.data)
                            } else {
                                android.util.Log.d("HomeActivity", "No plants found")
                                updateEmptyPlantUI()
                            }
                        }
                } else {
                    // No gardens found
                    android.util.Log.d("HomeActivity", "No gardens found")
                    updateEmptyPlantUI()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("HomeActivity", "Error retrieving gardens: ${exception.message}")
                android.widget.Toast.makeText(
                    this,
                    "Error retrieving gardens: ${exception.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                updateEmptyPlantUI()
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
        areaTextView.text = "0 sq.m"

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
                // If no image reference, set default image
                plantIcon.setImageResource(R.drawable.aloe_vera)
            }

            // Calculate and set planting date
            val dateAdded = data["dateAdded"] as? Long
            val plantingDateTextView = findViewById<TextView>(R.id.planting_date)

            dateAdded?.let {
                val date = Date(it)
                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                plantingDateTextView.text = sdf.format(date)
            } ?: run {
                plantingDateTextView.text = "Date unknown"
            }

            // Calculate and set expected harvest date
            val growthPeriod = data["growthPeriod"] as? Long ?: 0
            val harvestDateTextView = findViewById<TextView>(R.id.harvest_date)

            dateAdded?.let {
                val calendar = Calendar.getInstance()
                calendar.time = Date(it)
                calendar.add(Calendar.DAY_OF_YEAR, growthPeriod.toInt())

                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                harvestDateTextView.text = sdf.format(calendar.time)
            } ?: run {
                harvestDateTextView.text = "Date unknown"
            }

            // Update area size info
            val areaTextView = findViewById<TextView>(R.id.area_size)
            // Handle different possible number types from Firestore
            val area = when (val areaValue = data["area"]) {
                is Double -> areaValue
                is Float -> areaValue.toDouble()
                is Long -> areaValue.toDouble()
                is Int -> areaValue.toDouble()
                is String -> areaValue.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val roundedArea = Math.round(area * 100.0) / 100.0  // Round to 2 decimal places
            areaTextView.text = "$roundedArea sq.m"

            // Update growth period info
            val growthPeriodTextView = findViewById<TextView>(R.id.growth_period)
            growthPeriodTextView.text = "$growthPeriod days"
        } ?: run {
            // Handle null data case
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
        if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }
}