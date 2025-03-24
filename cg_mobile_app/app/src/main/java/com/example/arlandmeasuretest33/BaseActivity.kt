package com.example.arlandmeasuretest33

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun enableEdgeToEdge() {
        // Hide the action bar
        supportActionBar?.hide()
        
        // For Android R (API 30) and above, use the WindowInsetsController
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Set window to edge-to-edge display
            window.setDecorFitsSystemWindows(false)
            
            // Get the controller and hide both status and navigation bars
            window.insetsController?.let {
                // Hide both status bars and navigation bars
                it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                
                // When the user swipes from edge, show bars temporarily
                it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions (pre-Android 11)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                
                // Hide the nav bar and status bar
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                
                // Immersive sticky flag - bars temporarily reappear with a swipe
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        // Set status and navigation bars to transparent
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }
} 