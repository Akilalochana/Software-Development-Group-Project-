package com.example.arlandmeasuretest33

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.arlandmeasuretest33.Getstart2
import com.example.arlandmeasuretest33.HomeActivity
import com.example.arlandmeasuretest33.R
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val splashDuration: Long = 2500 // Slightly longer duration to accommodate the pulse animation
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_ARlandMeasuretest33)
        setContentView(R.layout.activity_splash)
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Find views
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val outerCircle1 = findViewById<View>(R.id.outerCircle1)
        val outerCircle2 = findViewById<View>(R.id.outerCircle2)

        // Start animations
        animateLogo(logoImageView)
        animateCirclePulse(outerCircle1, outerCircle2)

        // Navigate to appropriate activity after the splash duration
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, splashDuration)
    }

    private fun animateLogo(logoImageView: ImageView) {
        // Initial state - start with logo slightly smaller and invisible
        logoImageView.scaleX = 0.7f
        logoImageView.scaleY = 0.7f
        logoImageView.alpha = 0f

        // Create animations
        val fadeIn = ObjectAnimator.ofFloat(logoImageView, View.ALPHA, 0f, 1f)
        fadeIn.duration = 800

        val scaleXAnimator = ObjectAnimator.ofFloat(logoImageView, View.SCALE_X, 0.7f, 1f)
        scaleXAnimator.duration = 1000

        val scaleYAnimator = ObjectAnimator.ofFloat(logoImageView, View.SCALE_Y, 0.7f, 1f)
        scaleYAnimator.duration = 1000

        // Combine animations and play them together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, scaleXAnimator, scaleYAnimator)
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun animateCirclePulse(outerCircle1: View, outerCircle2: View) {
        // Set initial states
        outerCircle1.scaleX = 1f
        outerCircle1.scaleY = 1f
        outerCircle1.alpha = 1f

        outerCircle2.scaleX = 1f
        outerCircle2.scaleY = 1f
        outerCircle2.alpha = 1f

        // First circle animation - REDUCED from 2.5f to 1.8f
        val scale1X = ObjectAnimator.ofFloat(outerCircle1, View.SCALE_X, 1f, 1.8f)
        val scale1Y = ObjectAnimator.ofFloat(outerCircle1, View.SCALE_Y, 1f, 1.8f)
        val fade1 = ObjectAnimator.ofFloat(outerCircle1, View.ALPHA, 1f, 0f)

        scale1X.duration = 1500
        scale1Y.duration = 1500
        fade1.duration = 1500

        // Second circle animation with delay - REDUCED from 2.5f to 1.8f
        val scale2X = ObjectAnimator.ofFloat(outerCircle2, View.SCALE_X, 1f, 1.8f)
        val scale2Y = ObjectAnimator.ofFloat(outerCircle2, View.SCALE_Y, 1f, 1.8f)
        val fade2 = ObjectAnimator.ofFloat(outerCircle2, View.ALPHA, 1f, 0f)

        scale2X.duration = 1500
        scale2X.startDelay = 400
        scale2Y.duration = 1500
        scale2Y.startDelay = 400
        fade2.duration = 1500
        fade2.startDelay = 400

        // Play animations
        val pulse1 = AnimatorSet()
        pulse1.playTogether(scale1X, scale1Y, fade1)
        pulse1.interpolator = DecelerateInterpolator()

        val pulse2 = AnimatorSet()
        pulse2.playTogether(scale2X, scale2Y, fade2)
        pulse2.interpolator = DecelerateInterpolator()

        val pulseSet = AnimatorSet()
        pulseSet.playTogether(pulse1, pulse2)
        pulseSet.start()
    }

    private fun checkAuthAndNavigate() {
        if (auth.currentUser != null) {
            // User is logged in, go directly to home page
            val homeIntent = Intent(this, HomeActivity::class.java)
            startActivity(homeIntent)
        } else {
            // User is not logged in, continue with onboarding
            val onboardingIntent = Intent(this, Getstart2::class.java)
            startActivity(onboardingIntent)
        }

        finish()

        // Add custom transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}