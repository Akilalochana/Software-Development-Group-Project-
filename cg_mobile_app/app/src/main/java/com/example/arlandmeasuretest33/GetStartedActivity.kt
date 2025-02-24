package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class GetStartedActivity : AppCompatActivity() {
    private lateinit var loginButton: MaterialButton
    private lateinit var signUpButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_started)

        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signUpButton)

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signUpButton.setOnClickListener {
            // TODO: Implement sign up functionality
            // For now, you can show a toast message
            android.widget.Toast.makeText(this, "Sign Up coming soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
} 