package com.example.arlandmeasuretest33

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity(), ChatAdapter.ButtonClickListener {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var backButton: ImageButton

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatbotService: ChatbotService
    
    // Map of feature names to their corresponding Activity classes
    private val featureActivities = mapOf(
        "weather" to WeatherActivity::class.java,
        "plants" to PlantInformationActivity::class.java,
        "tips" to TipsActivity::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        // Initialize UI components
        initViews()

        // Initialize the chatbot service
        chatbotService = ChatbotService(this)

        // Setup RecyclerView and its adapter
        setupRecyclerView()

        // Add click listeners
        setupClickListeners()

        // Add welcome message
        addBotMessage("Hello! I'm your agricultural assistant. How can I help with your farming needs today?")

        // Check if there's a message passed from HomeActivity
        intent.getStringExtra("USER_MESSAGE")?.let { message ->
            // Set the message in the EditText
            messageEditText.setText(message)

            // Optional: Automatically send the message
            sendMessage(message)
            messageEditText.text.clear()
        }
    }

    private fun initViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        backButton = findViewById(R.id.backButton)
    }    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        // Set the button click listener
        chatAdapter.setButtonClickListener(this)
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        // Send button click listener
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageEditText.text.clear()
            }
        }

        // Back button click listener
        backButton.setOnClickListener {
            finish()
        }
    }    private fun sendMessage(message: String) {
        // Add user message to chat
        addUserMessage(message)

        // Show loading indicator
        loadingIndicator.visibility = View.VISIBLE

        // Get response from Gemini API
        lifecycleScope.launch {
            try {
                val response = chatbotService.sendMessage(message)
                if (response.hasButton) {
                    addBotMessageWithButton(response.text, response.buttonText, response.featureName)
                } else {
                    addBotMessage(response.text)
                }
            } catch (e: Exception) {
                addBotMessage("Sorry, I encountered an error. Please try again later.")
            } finally {
                // Hide loading indicator
                loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun addUserMessage(message: String) {
        val chatMessage = ChatMessage(message, true)
        chatAdapter.addMessage(chatMessage)
        scrollToBottom()
    }    private fun addBotMessage(message: String) {
        val chatMessage = ChatMessage(message, false)
        chatAdapter.addMessage(chatMessage)
        scrollToBottom()
    }
    
    /**
     * Add a bot message with an interactive button
     * @param message The text message to display
     * @param buttonText The text to display on the button
     * @param featureName The name of the feature to navigate to when button is clicked
     */
    private fun addBotMessageWithButton(message: String, buttonText: String, featureName: String) {
        val chatMessage = ChatMessage(
            message = message,
            isFromUser = false,
            hasButton = true,
            buttonText = buttonText,
            featureName = featureName
        )
        chatAdapter.addMessage(chatMessage)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }
    
    /**
     * Implementation of ButtonClickListener to handle button clicks in chat messages
     */
    override fun onButtonClick(featureName: String) {
        // Navigate to the appropriate feature based on the feature name
        val activityClass = featureActivities[featureName.lowercase()]
        if (activityClass != null) {
            val intent = Intent(this, activityClass)
            startActivity(intent)
        }
    }
}