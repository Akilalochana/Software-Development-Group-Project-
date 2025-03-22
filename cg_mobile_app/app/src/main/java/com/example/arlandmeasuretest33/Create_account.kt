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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class Create_account : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "CreateAccount"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_account)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Debug log to verify Firebase initialization
        Log.d(TAG, "Firebase Auth initialized: ${auth != null}")
        Log.d(TAG, "Firebase Firestore initialized: ${db != null}")

        // Get references to UI elements
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val signupButton = findViewById<CardView>(R.id.loginButton)
        val togglePasswordVisibility = findViewById<ImageButton>(R.id.togglePasswordVisibility)

        // Set up password visibility toggle
        togglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility(passwordEditText, togglePasswordVisibility)
        }

        // Set up signup button click listener
        signupButton.setOnClickListener {
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

            if (password.length < 6) {
                passwordEditText.error = "Password must be at least 6 characters"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            // Direct sign up without pre-checking email
            signUpWithEmailPassword(email, password)
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

    private fun signUpWithEmailPassword(email: String, password: String) {
        Log.d(TAG, "Attempting to create user: $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")

                    // Get the current user
                    val currentUser = auth.currentUser

                    if (currentUser != null) {
                        Log.d(TAG, "User created with ID: ${currentUser.uid}")
                        createUserInFirestore(currentUser.uid, email)
                    } else {
                        Log.w(TAG, "User is null despite successful auth")
                        Toast.makeText(this, "Authentication successful but failed to get user details",
                            Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)

                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> {
                            Log.d(TAG, "Email already exists, attempting sign in")
                            signInExistingUser(email, password)
                        }
                        else -> {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun signInExistingUser(email: String, password: String) {
        Log.d(TAG, "Attempting to sign in existing user: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    if (user != null) {
                        Log.d(TAG, "User signed in with ID: ${user.uid}")
                        createUserInFirestore(user.uid, email)
                    } else {
                        Toast.makeText(this, "Signed in but failed to get user details",
                            Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createUserInFirestore(userId: String, email: String) {
        Log.d(TAG, "Attempting to create Firestore document for user: $userId")

        // Create a user document with basic info
        val userData = hashMapOf(
            "email" to email,
            "timestamp" to System.currentTimeMillis()
        )

        // Use this approach which is more reliable
        db.collection("user_data").document(userId)
            .set(userData)  // Use set instead of merge for a new user
            .addOnSuccessListener {
                Log.d(TAG, "User document successfully created!")
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user document", e)
                // Still navigate to home but inform the user
                Toast.makeText(this, "Account created but profile setup failed: ${e.message}",
                    Toast.LENGTH_LONG).show()

                // Log the specific error for debugging
                Log.e(TAG, "Firestore error details: ", e)
                navigateToHome()
            }
    }

    private fun navigateToHome() {
        Log.d(TAG, "Navigating to Home Activity")
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}