package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class Welcome_screen : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "GoogleSignIn"

    // Register the launcher for Google Sign-In activity result
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configure Google Sign-In
        configureGoogleSignIn()

        // Set up click listener for Google Sign-In button
        findViewById<CardView>(R.id.googleSignInButton).setOnClickListener {
            signIn()
        }

        // Set up click listener for Create Account button
        findViewById<CardView>(R.id.createAccountButton).setOnClickListener {
            val intent = Intent(this, Create_account::class.java)
            startActivity(intent)
            // Navigate to Create Account screen
            // Intent to CreateAccountActivity
        }

        // Set up click listener for Login text
        findViewById<android.widget.TextView>(R.id.loginButton).setOnClickListener {
            // Navigate to Login screen
            // Intent to LoginActivity
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
    }

    private fun configureGoogleSignIn() {
        // Configure sign-in to request the user's ID, email address, and basic profile
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // You need to add your web client ID from Google Cloud Console
            .requestIdToken("1067132376349-qmocdtigf1mgmtuhu0pjn84oje5dk4kq.apps.googleusercontent.com")
            .build()

        // Build a GoogleSignInClient with the options specified by gso
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)

            // Got Google account, now authenticate with Firebase
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Sign in failed
            Log.w(TAG, "Google sign in failed", e)
            Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithCredential:success")
                    val user = firebaseAuth.currentUser
                    saveUserToFirestore(user)
                    updateUI(user)
                } else {
                    // Sign in fails
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun saveUserToFirestore(user: FirebaseUser?) {
        user?.let {
            val userMap = hashMapOf(
                "uid" to it.uid,
                "email" to it.email,
                "displayName" to it.displayName,
                "photoUrl" to (it.photoUrl?.toString() ?: ""),
                "lastLogin" to System.currentTimeMillis()
            )

            // Save to Firestore - if user exists, data will be merged
            firestore.collection("users").document(it.uid)
                .set(userMap)
                .addOnSuccessListener {
                    Log.d(TAG, "User data saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error saving user data", e)
                }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // User is signed in, navigate to main activity
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("user_email", user.email)
            intent.putExtra("user_name", user.displayName)
            startActivity(intent)
            finish() // Close the welcome screen
        }
    }
}