package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class Create_account : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Make sure this points to the correct layout file
        setContentView(R.layout.activity_create_account)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get references to UI elements
        val emailEditText = findViewById<EditText>(R.id.emailEditText) // Changed from emailInput
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText) // Changed from passwordInput
        val signupButton = findViewById<CardView>(R.id.loginButton) // Changed from signupButton

        // Set up signup button click listener
        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validate input
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sign up the user
            signUpWithEmailPassword(email, password)
        }
    }

    private fun signUpWithEmailPassword(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sign Up Successful", Toast.LENGTH_SHORT).show()
                    // Navigate to the Home Activity
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}