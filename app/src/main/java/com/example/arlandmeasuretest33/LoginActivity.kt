package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        // Set up login button click listener
        loginButton.setOnClickListener {
            val username = usernameEditText.text?.toString() ?: ""
            val password = passwordEditText.text?.toString() ?: ""

            if (validateCredentials(username, password)) {
                try {
                    // Create an intent to start MainActivity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)

                    // Optional: add transition animation
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

                    // Close LoginActivity
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error launching AR: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please enter valid credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateCredentials(username: String, password: String): Boolean {
        // Add your authentication logic here
        // For testing, we'll just check if fields are not empty
        if (username.isEmpty()) {
            usernameEditText.error = "Username required"
            return false
        }
        if (password.isEmpty()) {
            passwordEditText.error = "Password required"
            return false
        }
        return true
    }

    override fun onBackPressed() {
        // Optional: Handle back button press
        super.onBackPressed()
        finishAffinity() // This will close the app instead of going back
    }
}