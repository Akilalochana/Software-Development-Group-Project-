package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Getstart3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.getstart3)

        // Handle Next button click - go to signup page
        findViewById<TextView>(R.id.nextButton).setOnClickListener {
            val intent = Intent(this, Getstart4::class.java)
            startActivity(intent)
        }

        // Handle Skip button click - go to signup page
        findViewById<TextView>(R.id.skipButton).setOnClickListener {
            val intent = Intent(this, Create_account::class.java)
            startActivity(intent)
        }
    }
}