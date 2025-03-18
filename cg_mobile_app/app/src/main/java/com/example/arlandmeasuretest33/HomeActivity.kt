package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var greetingText: TextView
    private lateinit var dateText: TextView
    private lateinit var progressCard: CardView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var startButton: CardView
    private lateinit var weatherButton: CardView
    private lateinit var arPlotButton: CardView
    private lateinit var reportButton: CardView
    private lateinit var profileButton: CardView
    private lateinit var tipsButton: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        greetingText = findViewById(R.id.greetingText)
        dateText = findViewById(R.id.dateText)
        progressCard = findViewById(R.id.progressCard)
        progressIndicator = findViewById(R.id.progressIndicator)
        startButton = findViewById(R.id.startButton)
        weatherButton = findViewById(R.id.weatherButton)
        arPlotButton = findViewById(R.id.arPlotButton)
        reportButton = findViewById(R.id.reportButton)
        profileButton = findViewById(R.id.profileButton)
        tipsButton = findViewById(R.id.tipsButton)

        // Set user information and date
        updateUserInfo()
        updateDateInfo()

        // Set click listeners for all buttons
        startButton.setOnClickListener {
            // Start the location selection activity (usual flow)
            val intent = Intent(this, LocationSelectionActivity::class.java)
            startActivity(intent)
        }

        weatherButton.setOnClickListener {
            // Navigate to the weather activity
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

        arPlotButton.setOnClickListener {
            // TODO: Navigate to the AR plot activity
            // val intent = Intent(this, ARPlotActivity::class.java)
            // startActivity(intent)
        }

        reportButton.setOnClickListener {
            // TODO: Navigate to the report activity
            // val intent = Intent(this, ReportActivity::class.java)
            // startActivity(intent)
        }

        profileButton.setOnClickListener {
            // TODO: Navigate to the profile activity
            // val intent = Intent(this, ProfileActivity::class.java)
            // startActivity(intent)
        }

        tipsButton.setOnClickListener {
            // Navigate to the tips activity
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
        }
    }

    private fun updateDateInfo() {
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        dateText.text = currentDate
    }

    // Sign out method (you can call this from a menu option if needed)
    private fun signOut() {
        auth.signOut()
        val intent = Intent(this, SignUp::class.java)
        startActivity(intent)
        finish()
    }
}