package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import android.widget.Toast

class TipItem(val icon: Int, val title: String, val description: String)

class TipsAdapter(private val tips: List<TipItem>) : RecyclerView.Adapter<TipsAdapter.TipViewHolder>() {
    class TipViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.tipIcon)
        val title: TextView = view.findViewById(R.id.tipTitle)
        val description: TextView = view.findViewById(R.id.tipDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tip, parent, false)
        return TipViewHolder(view)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        val tip = tips[position]
        holder.icon.setImageResource(tip.icon)
        holder.title.text = tip.title
        holder.description.text = tip.description
    }

    override fun getItemCount() = tips.size
}

class LocationSelectionActivity : AppCompatActivity() {
    private lateinit var gardenNameInput: TextInputEditText
    private lateinit var districtSpinner: AutoCompleteTextView
    private lateinit var continueButton: MaterialButton
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: TabLayout

    private val tips = listOf(
        TipItem(R.drawable.ic_tips, "Colombo Climate", "Colombo is known for its diverse climate. Ensure your garden gets enough sunlight."),
        TipItem(R.drawable.ic_tips, "Anuradhapura Tips", "Anuradhapura has a hot climate. Regular watering is key to keeping plants healthy."),
        TipItem(R.drawable.ic_tips, "Kandy Gardens", "Kandy's cooler temperatures are great for certain plants. Keep them in a shaded area."),
        TipItem(R.drawable.ic_tips, "Nuwara Eliya Plants", "Nuwara Eliya has a cooler climate, ideal for a variety of flowers. Consider adding some roses!"),
        TipItem(R.drawable.ic_tips, "Coastal Gardening", "Galle's coastal climate requires salt-tolerant plants. Be mindful of the soil's salinity.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_selection)

        initializeViews()
        setupDistrictSpinner()
        setupTipsViewPager()
        setupListeners()
    }

    private fun initializeViews() {
        gardenNameInput = findViewById(R.id.gardenNameInput)
        districtSpinner = findViewById(R.id.districtSpinner)
        continueButton = findViewById(R.id.continueButton)
        viewPager = findViewById(R.id.tipsViewPager)
        dotsIndicator = findViewById(R.id.dotsIndicator)
    }

    private fun setupTipsViewPager() {
        viewPager.adapter = TipsAdapter(tips)
        
        // Connect the dots indicator with the ViewPager2
        TabLayoutMediator(dotsIndicator, viewPager) { _, _ -> }.attach()
        
        // Auto-scroll every 5 seconds
        viewPager.setCurrentItem(0, false)
        startAutoScroll()
    }

    private fun startAutoScroll() {
        val handler = android.os.Handler()
        val runnable = object : Runnable {
            override fun run() {
                val nextItem = (viewPager.currentItem + 1) % tips.size
                viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, 5000)
            }
        }
        handler.postDelayed(runnable, 5000)
    }

    private fun setupDistrictSpinner() {
        val districts = arrayOf(
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo",
            "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara",
            "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar",
            "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya",
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.item_location_dropdown,
            districts
        )

        districtSpinner.setAdapter(adapter)
    }

    private fun setupListeners() {
        continueButton.setOnClickListener {
            val gardenName = gardenNameInput.text.toString()
            val selectedDistrict = districtSpinner.text.toString()

            try {
                // Store the selected location for weather feature
                val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putString("SELECTED_LOCATION", selectedDistrict)
                    apply()
                }

                // Continue to plant recommendations
                val intent = Intent(this, PlantRecommendationActivity::class.java).apply {
                    putExtra("SELECTED_DISTRICT", selectedDistrict)
                    putExtra("GARDEN_NAME", gardenName)
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error navigating to recommendations", Toast.LENGTH_SHORT).show()
            }
        }
    }
}