package com.example.arlandmeasuretest33

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity

class TipsActivity : AppCompatActivity() {

    private lateinit var arPlotVideo: VideoView
    private lateinit var weatherVideo: VideoView
    private lateinit var reportVideo: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tips)

        // Initialize VideoViews
        arPlotVideo = findViewById(R.id.arPlotVideo)
        weatherVideo = findViewById(R.id.weatherVideo)
        reportVideo = findViewById(R.id.reportVideo)

        // Set up video sources
        setupVideos()
    }

    private fun setupVideos() {
        // Set up AR Plot video
        val arPlotVideoPath = "android.resource://" + packageName + "/" + R.raw.ar_plot_video
        val arPlotUri = Uri.parse(arPlotVideoPath)
        arPlotVideo.setVideoURI(arPlotUri)

        // Hide media controls
        arPlotVideo.setMediaController(null)

        // Set up Weather video
        val weatherVideoPath = "android.resource://" + packageName + "/" + R.raw.ar_plot_video
        val weatherUri = Uri.parse(weatherVideoPath)
        weatherVideo.setVideoURI(weatherUri)
        weatherVideo.setMediaController(null)

        // Set up Report video
        val reportVideoPath = "android.resource://" + packageName + "/" + R.raw.ar_plot_video
        val reportUri = Uri.parse(reportVideoPath)
        reportVideo.setVideoURI(reportUri)
        reportVideo.setMediaController(null)

        // Set up completion listeners to loop videos
        setupVideoLooping()

        // Start playing videos
        arPlotVideo.start()
        weatherVideo.start()
        reportVideo.start()
    }

    private fun setupVideoLooping() {
        // Set up looping for AR Plot video
        arPlotVideo.setOnCompletionListener { mp ->
            arPlotVideo.start()
        }

        // Set up looping for Weather video
        weatherVideo.setOnCompletionListener { mp ->
            weatherVideo.start()
        }

        // Set up looping for Report video
        reportVideo.setOnCompletionListener { mp ->
            reportVideo.start()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume videos when activity comes to foreground
        arPlotVideo.start()
        weatherVideo.start()
        reportVideo.start()
    }

    override fun onPause() {
        super.onPause()
        // Pause videos when activity goes to background
        arPlotVideo.pause()
        weatherVideo.pause()
        reportVideo.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        arPlotVideo.stopPlayback()
        weatherVideo.stopPlayback()
        reportVideo.stopPlayback()
    }
}

