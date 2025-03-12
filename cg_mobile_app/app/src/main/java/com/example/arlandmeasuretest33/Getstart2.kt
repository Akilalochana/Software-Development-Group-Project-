package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Getstart2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.getstart2)

        // Handle Next button click - go to signup page
        findViewById<TextView>(R.id.nextButton).setOnClickListener {
            val intent = Intent(this, Getstart3::class.java)
            startActivity(intent)
        }

        // Handle Skip button click - go to signup page
        findViewById<TextView>(R.id.skipButton).setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
    }
}