package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class getstartedpage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_start1)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get the button reference
        val getStartedButton = findViewById<Button>(R.id.getStartedButton)

        // Set the click listener
        getStartedButton.setOnClickListener {
            // Check if user is logged in ONLY when button is clicked
            if (auth.currentUser != null) {
                // User is logged in, go directly to home page
                val homeIntent = Intent(this, HomeActivity::class.java)
                startActivity(homeIntent)
                finish()
            } else {
                // User is not logged in, continue with onboarding
                val onboardingIntent = Intent(this, Getstart2::class.java)
                startActivity(onboardingIntent)
            }
        }
    }
}