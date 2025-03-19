package com.example.arlandmeasuretest33

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import java.util.Timer
import java.util.TimerTask


class HomeActivity : AppCompatActivity() {
    // Firebase Auth components
    private lateinit var auth: FirebaseAuth
    private lateinit var greetingText: TextView

    // Banner slideshow components
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private lateinit var dots: Array<View?>
    private var currentPage = 0
    private val sliderHandler = Handler(Looper.getMainLooper())

    // Feature cards and buttons
    private lateinit var weatherCard: CardView
    private lateinit var reportCard: CardView
    private lateinit var tipsCard: CardView
    private lateinit var checkWeatherButton: Button
    private lateinit var viewReportsButton: Button
    private lateinit var getTipsButton: Button
    private lateinit var startButton: Button

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
        setContentView(R.layout.activity_home) // Changed to match the home layout filename

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI components for user info and navigation
        initializeUIComponents()

        // Set user information
        updateUserInfo()

        // Set up the banner slideshow
        setupBannerSlideshow()

        // Set click listeners for all buttons
        setupButtonListeners()
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
    }

    private fun setupButtonListeners() {
        startButton.setOnClickListener {
            // Start the location selection activity
            val intent = Intent(this, LocationSelectionActivity::class.java)
            startActivity(intent)
        }

        checkWeatherButton.setOnClickListener {
            // Navigate to the weather activity
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

        viewReportsButton.setOnClickListener {
            // For now, just show a toast message since ReportsActivity doesn't exist yet
            android.widget.Toast.makeText(this, "Reports feature coming soon!", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Uncomment when ReportsActivity is implemented
            // val intent = Intent(this, ReportsActivity::class.java)
            // startActivity(intent)
        }

        getTipsButton.setOnClickListener {
            // Navigate to the tips activity
            val intent = Intent(this, TipsActivity::class.java)
            startActivity(intent)
        }

        // You can also add click listeners to the cards themselves
        weatherCard.setOnClickListener {
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

        reportCard.setOnClickListener {
            // For now, just show a toast message
            android.widget.Toast.makeText(this, "Reports feature coming soon!", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Uncomment when ReportsActivity is implemented
            // val intent = Intent(this, ReportsActivity::class.java)
            // startActivity(intent)
        }

        tipsCard.setOnClickListener {
            val intent = Intent(this, TipsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateUserInfo() {
        val currentUser = auth.currentUser
        currentUser?.let {
            val displayName = it.displayName
            val email = it.email

            if (!displayName.isNullOrEmpty()) {
                greetingText.text = "Hi, ${displayName.split(" ")[0]}!"
            } else if (!email.isNullOrEmpty()) {
                val username = email.substringBefore("@")
                greetingText.text = "Hi, $username!"
            } else {
                greetingText.text = "Hi, User!"
            }
        } ?: run {
            // Handle case where user is not signed in
            greetingText.text = "Welcome, Guest!"

            // Optionally redirect to sign in
            // val intent = Intent(this, SignInActivity::class.java)
            // startActivity(intent)
            // finish()
        }
    }

    private fun setupBannerSlideshow() {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            // Show error message if banner setup fails
            android.widget.Toast.makeText(this, "Error setting up banners: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDots(position: Int) {
        try {
            // First clear any existing views
            dotsLayout.removeAllViews()

            // Create dots based on number of slides
            dots = arrayOfNulls(bannerSlides.size)

            for (i in bannerSlides.indices) {
                dots[i] = View(this)
                // Specify dot size and margin directly if resource doesn't exist
                val dotSize = resources.getDimensionPixelSize(R.dimen.dot_size)
                    ?: 8 // Fallback to 8dp if resource not found
                val dotMargin = resources.getDimensionPixelSize(R.dimen.dot_margin)
                    ?: 4 // Fallback to 4dp if resource not found

                val params = LinearLayout.LayoutParams(dotSize, dotSize)
                params.setMargins(dotMargin, 0, dotMargin, 0)

                // Apply different background based on whether dot is active
                dots[i]?.layoutParams = params

                // Use direct color background if drawable resources don't exist
                if (i == position) {
                    dots[i]?.setBackgroundResource(R.drawable.active_dot)
                        ?: dots[i]?.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, theme))
                } else {
                    dots[i]?.setBackgroundResource(R.drawable.inactive_dot)
                        ?: dots[i]?.setBackgroundColor(resources.getColor(android.R.color.darker_gray, theme))
                }

                // Add dot to layout
                dotsLayout.addView(dots[i])
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Simply log the error but continue execution
            android.util.Log.e("HomeActivity", "Error setting up dots: ${e.message}")
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

    // Sign out method (you can call this from a menu option if needed)
    private fun signOut() {
        auth.signOut()
        val intent = Intent(this, SignUp::class.java)
        startActivity(intent)
        finish()
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