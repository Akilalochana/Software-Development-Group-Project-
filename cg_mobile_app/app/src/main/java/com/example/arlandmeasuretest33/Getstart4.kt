package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity

class Getstart4 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.getstart4)

        // Find the circular next button by its position (last FrameLayout in the view)
        val rootView = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        if (rootView is RelativeLayout) {
            // Find the last FrameLayout child, which should be our next button
            for (i in rootView.childCount - 1 downTo 0) {
                val child = rootView.getChildAt(i)
                if (child is FrameLayout) {
                    child.setOnClickListener {
                        val intent = Intent(this, getstartedpage::class.java)
                        startActivity(intent)
                    }
                    break
                }
            }
        }

        // Handle Skip button click using its ID
        findViewById<LinearLayout>(R.id.skip_button).setOnClickListener {
            val intent = Intent(this, getstartedpage::class.java)
            startActivity(intent)
        }
    }
}
