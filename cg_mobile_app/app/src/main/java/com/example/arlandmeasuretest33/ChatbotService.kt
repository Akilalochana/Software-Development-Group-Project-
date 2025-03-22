package com.example.arlandmeasuretest33

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class ChatbotService(private val context: Context) {

    // Replace with your actual API key when you get it from Google AI Studio
    private val apiKey = "AIzaSyBOwNDarz_8lGhJX66mz1k-RycVWKm1YTw"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-pro",
            apiKey = apiKey
        )
    }

    // Enhanced Sinhala detection pattern with more common words
    private val sinhalaPattern = Pattern.compile(
        "\\b(mata|mage|oya|oyage|api|apita|mokada|kohomada|ayubowan|karanna|kiyanna|yanna|enna|parissamin|" +
                "bohoma|istuti|apé|kərənnə|næ|kawda|monawa|wathura|pola|govi|govithena|gas|mal|palathuru|" +
                "කොහොමද|බත්|එළවළු|ගොවිතැන්|පලතුරු|ගෙවත්ත|වගාව|බීජ|පොහොර)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Check if text appears to be Sinhala or Singlish
    private fun isSinhalaOrSinglish(text: String): Boolean {
        return sinhalaPattern.matcher(text).find()
    }

    suspend fun sendMessage(message: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Detect if input appears to be Sinhala or Singlish
                val isUserSinhala = isSinhalaOrSinglish(message)

                // Create language instruction
                val languageInstruction = if (isUserSinhala) {
                    "The user is typing in Singlish (English mixed with Sinhala) or Sinhala. Please respond in proper Sinhala. "
                } else {
                    "The user is typing in English. Please respond in English. "
                }

                // Specialized agricultural context for Sri Lankan farming
                val agriculturalContext = """
                    You are a specialized Sri Lankan agricultural assistant focused exclusively on helping farmers with 
                    16 specific crops: carrot, bitter melon, winged bean, red spinach, long purple eggplant, beetroot, 
                    brinjal, cabbage, leeks, onion, potato, manioc, taro, pumpkin, knolkhol, and drumstick.
                    
                    For these 16 crops only, provide expert advice on:
                    1. Optimal growing conditions in Sri Lanka's climate zones
                    2. Seasonal planting calendars specific to Sri Lankan regions
                    3. Common pests and diseases affecting these crops in Sri Lanka
                    4. Traditional and modern farming techniques suitable for local conditions
                    5. Irrigation methods appropriate for different regions of Sri Lanka
                    6. Organic farming practices and natural pest control for these crops
                    7. Post-harvest handling and storage specific to Sri Lankan conditions
                    8. Market information and potential value addition for these crops
                    
                    When users ask about crops not in this list or non-agricultural topics, politely inform them that 
                    you can only assist with the 16 specified crops. Always tailor your answers to Sri Lankan farming 
                    contexts, considering local climate, traditions, and challenges. Keep answers practical, clear, and 
                    actionable for farmers with varying levels of technical knowledge.
                """.trimIndent()

                // Combine the context, language instruction, and user message
                val fullPrompt = "$agriculturalContext $languageInstruction User message: $message"

                // Generate response
                val response = generativeModel.generateContent(fullPrompt)

                // Return the text or a default message if null
                val responseText = response.text
                if (responseText != null) {
                    responseText
                } else {
                    if (isUserSinhala) {
                        "සමාවෙන්න, දැනට මට පිළිතුරු දීමට නොහැකිය. පසුව නැවත උත්සාහ කරන්න."
                    } else {
                        "Sorry, I couldn't generate a response. Please try again later."
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatbotService", "Error generating response", e)
                if (isSinhalaOrSinglish(message)) {
                    "සමාවෙන්න, දැනට මට පිළිතුරු දීමට නොහැකිය. පසුව නැවත උත්සාහ කරන්න."
                } else {
                    "Sorry, I'm unable to respond right now. Please try again later."
                }
            }
        }
    }
}