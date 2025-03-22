package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get references to UI elements
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<CardView>(R.id.loginButton)
        val signUpText = findViewById<TextView>(R.id.signUpButton)
        val togglePasswordVisibility = findViewById<ImageButton>(R.id.togglePasswordVisibility)

        // Set up login button click listener
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validate input
            if (email.isEmpty()) {
                emailEditText.error = "Email cannot be empty"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Password cannot be empty"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            // Perform login
            loginWithEmailPassword(email, password)
        }

        // Set up sign up text click listener
        signUpText.setOnClickListener {
            val intent = Intent(this, Create_account::class.java)
            startActivity(intent)
        }

        // Set up password visibility toggle
        togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility(passwordEditText, togglePasswordVisibility)
        }
    }

    private fun togglePasswordVisibility(passwordEditText: EditText, toggleButton: ImageButton) {
        val selection = passwordEditText.selectionEnd // Save cursor position

        if (passwordEditText.inputType == 129) { // 129 = textPassword
            // Show password
            passwordEditText.inputType = 1 // 1 = text
            toggleButton.setImageResource(R.drawable.ic_visibility_off) // Change icon to "visibility off"
        } else {
            // Hide password
            passwordEditText.inputType = 129 // 129 = textPassword
            toggleButton.setImageResource(R.drawable.ic_visibility) // Change icon to "visibility"
        }

        passwordEditText.setSelection(selection) // Restore cursor position
    }

    private fun loginWithEmailPassword(email: String, password: String) {
        Log.d(TAG, "Attempting to login user: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}