package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.arlandmeasuretest33.BannerSlideAdapter
import com.example.arlandmeasuretest33.BannerSlide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var dots: Array<View?>
    private var currentPage = 0
    private val sliderHandler = Handler(Looper.getMainLooper())

    // UI component properties
    private lateinit var greetingText: TextView
    private lateinit var weatherCard: View
    private lateinit var reportCard: View
    private lateinit var tipsCard: View
    private lateinit var checkWeatherButton: View
    private lateinit var viewReportsButton: View
    private lateinit var getTipsButton: View
    private lateinit var startButton: View

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

        // Set user information
        updateUserInfo()

        // Set up real-time updates for plant data
        setupRealTimeUpdates()

        // Set up the banner slideshow
        setupBannerSlideshow()

        // Set click listeners for all buttons
        setupButtonListeners()
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
    }

    // Setup real-time updates for plant data
    private fun setupRealTimeUpdates() {
        val currentUser = auth.currentUser ?: return

        // First, get all gardens for the current user
        val userGardensPath = "user_data/${currentUser.uid}/user_gardens"

        db.collection(userGardensPath)
            .get()
            .addOnSuccessListener { gardens ->
                if (!gardens.isEmpty) {
                    // Get the first garden (or you could show a selection to the user)
                    val firstGarden = gardens.documents[0].id

                    // Now listen to plants in this garden
                    val userPlantsPath = "$userGardensPath/$firstGarden/plants"

                    // Set up the real-time listener for plants in this garden
                    db.collection(userPlantsPath)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
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
                                updatePlantUI(plantDoc.data)
                            } else {
                                updateEmptyPlantUI()
                            }
                        }
                } else {
                    // No gardens found
                    updateEmptyPlantUI()
                }
            }
            .addOnFailureListener { exception ->
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

    // Add this function to update the UI with plant data
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
            // Update area size info
            val areaTextView = findViewById<TextView>(R.id.area_size)
            val area = data["area"] as? Double ?: 0.0
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
}