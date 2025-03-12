package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class getstartedpage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_start1)

        findViewById<Button>(R.id.getStartedButton).setOnClickListener {
            val intent = Intent(this, Getstart2::class.java)
            startActivity(intent)
        }
    }
}