package com.example.arlandmeasuretest33

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class Home : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Find all CardViews and set click listeners
        findViewById<View>(R.id.timeTableCard).setOnClickListener(this)
        findViewById<View>(R.id.homeworkCard).setOnClickListener(this)
        findViewById<View>(R.id.weatherCard).setOnClickListener(this)
        findViewById<View>(R.id.arPlotCard).setOnClickListener(this)
        findViewById<View>(R.id.reportCard).setOnClickListener(this)
        findViewById<View>(R.id.waterCycleCard).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.timeTableCard -> {
                // Handle Time Table click
                println("Time Table clicked")
            }
            R.id.homeworkCard -> {
                // Handle Homework click
                println("Homework clicked")
            }
            R.id.weatherCard -> {
                // Handle Weather click
                println("Weather clicked")
            }
            R.id.arPlotCard -> {
                // Handle AR Plot click
                println("AR Plot clicked")
            }
            R.id.reportCard -> {
                // Handle Report click
                println("Report clicked")
            }
            R.id.waterCycleCard -> {
                // Handle Water Cycle click
                println("Water Cycle clicked")
            }
        }
    }
}