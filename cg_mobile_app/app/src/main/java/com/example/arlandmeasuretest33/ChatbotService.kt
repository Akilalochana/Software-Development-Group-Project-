package com.example.arlandmeasuretest33

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import org.json.JSONObject

class ChatbotService(private val context: Context) {

    // Replace with your actual API key when you get it from Google AI Studio
    private val apiKey = "AIzaSyBOwNDarz_8lGhJX66mz1k-RycVWKm1YTw"

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )
    }
    
    // Data class to hold the response with potential button info
    data class BotResponse(
        val text: String,
        val hasButton: Boolean = false,
        val buttonText: String = "",
        val featureName: String = ""
    )
    
    // Store recent user messages to detect repetition
    private val recentUserMessages = mutableListOf<String>()
    private val MAX_MESSAGE_HISTORY = 10 // Keep track of last 10 messages

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
    
    /**
     * Add a message to the history and maintain maximum size
     */
    private fun addMessageToHistory(message: String) {
        val normalizedMessage = message.trim().lowercase()
        recentUserMessages.add(normalizedMessage)
        
        // Maintain max size
        if (recentUserMessages.size > MAX_MESSAGE_HISTORY) {
            recentUserMessages.removeAt(0)
        }
    }
    
    /**
     * Check if the user is sending the same message repeatedly
     * @return true if the same message appears 3 or more times in recent history
     */
    private fun checkForRepetitiveMessages(message: String): Boolean {
        val normalizedMessage = message.trim().lowercase()
        
        // Count occurrences of this message in history
        val occurrences = recentUserMessages.count { it == normalizedMessage }
        
        // If it appears 2 times already and this would be the 3rd time, consider it repetitive
        return occurrences >= 2
    }

    /**
     * Detects if the user's message is requesting a specific feature
     * @param message The user's input message
     * @return BotResponse with button if feature detected, null otherwise
     */
    private fun detectFeatureRequest(message: String): BotResponse? {
        // Convert message to lowercase for easier matching
        val lowercaseMessage = message.lowercase()
        
        // Check for weather-related queries
        if (lowercaseMessage.contains("weather") || 
            lowercaseMessage.contains("rain") || 
            lowercaseMessage.contains("temperature") || 
            lowercaseMessage.contains("forecast") ||
            lowercaseMessage.contains("climate")) {
            
            return BotResponse(
                text = "I can provide you with weather information for your location, which can help you plan your farming activities. Would you like to check the weather forecast?",
                hasButton = true,
                buttonText = "Check Weather",
                featureName = "weather"
            )
        }
        
        // Check for plant information queries
        else if (lowercaseMessage.contains("plant") || 
                lowercaseMessage.contains("crop") || 
                lowercaseMessage.contains("grow") || 
                lowercaseMessage.contains("cultivation") ||
                lowercaseMessage.contains("farming techniques")) {
            
            return BotResponse(
                text = "I can show you detailed information about various plants including growing conditions, care instructions, and expected yields. Would you like to explore our plant database?",
                hasButton = true,
                buttonText = "Explore Plants",
                featureName = "plants"
            )
        }
        
        // Check for tips/advice queries
        else if (lowercaseMessage.contains("tip") || 
                lowercaseMessage.contains("advice") || 
                lowercaseMessage.contains("help") || 
                lowercaseMessage.contains("guide") ||
                lowercaseMessage.contains("how to")) {
            
            return BotResponse(
                text = "I have some helpful farming tips and video guides that might assist you with your agricultural needs. Would you like to see them?",
                hasButton = true,
                buttonText = "View Tips",
                featureName = "tips"
            )
        }
        
        // No specific feature detected
        return null
    }

    suspend fun sendMessage(message: String): BotResponse {
        return withContext(Dispatchers.IO) {
            try {
                // Check for repetitive messages
                val isRepetitive = checkForRepetitiveMessages(message)
                if (isRepetitive) {
                    val isUserSinhala = isSinhalaOrSinglish(message)
                    return@withContext if (isUserSinhala) {
                        BotResponse(text = "ඔබ එකම ප්‍රශ්නය නැවත නැවත අසමින් සිටින බව පෙනේ. කරුණාකර වෙනත් ප්‍රශ්නයක් අසන්න හෝ ඔබේ ප්‍රශ්නය වඩාත් පැහැදිලිව සඳහන් කරන්න.")
                    } else {
                        BotResponse(text = "I notice you're asking the same question repeatedly. Please try a different question or provide more details about what you're looking for.")
                    }
                }
                
                // Store this message in the history
                addMessageToHistory(message)
                
                // Check if the message is about a specific feature first
                val featureResponse = detectFeatureRequest(message)
                if (featureResponse != null) {
                    return@withContext featureResponse
                }
                
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
                    
                    IMPORTANT FACTUAL ACCURACY GUIDELINES:
                    - If you're uncertain about any specific detail, acknowledge your uncertainty instead of providing 
                      potentially incorrect information. For example, say "I don't have specific information about X" 
                      rather than guessing.
                    - Base your advice on established farming knowledge applicable to tropical climates and Sri Lankan 
                      conditions specifically.
                    - For factual claims about growing seasons, pest treatments, or yields, only provide information that 
                      is commonly accepted for these crops in Sri Lanka.
                    - Clearly distinguish between traditional practices and modern/scientific approaches when relevant.
                    - When discussing quantities (fertilizer amounts, water needs, etc.), provide ranges rather than 
                      precise figures if exact measurements vary by specific conditions.
                    - Do not recommend experimental techniques or practices without explicitly labeling them as such.
                      
                    RESPONSE FORMAT GUIDELINES:
                    - Keep all responses concise and brief, typically 2-3 short paragraphs maximum.
                    - Use bullet points instead of long paragraphs when providing steps or lists.
                    - Focus on the most important information that directly answers the user's question.
                    - Avoid lengthy explanations, background information, or tangential details.
                    - For complex topics, provide only the key points and most practical advice.
                """.trimIndent()

                // Combine the context, language instruction, and user message
                val fullPrompt = "$agriculturalContext $languageInstruction User message: $message"

                // Generate response
                val response = generativeModel.generateContent(fullPrompt)                // Return the text or a default message if null
                val responseText = response.text
                if (responseText != null) {
                    BotResponse(text = responseText)
                } else {
                    if (isUserSinhala) {
                        BotResponse(text = "සමාවෙන්න, දැනට මට පිළිතුරු දීමට නොහැකිය. පසුව නැවත උත්සාහ කරන්න.")
                    } else {
                        BotResponse(text = "Sorry, I couldn't generate a response. Please try again later.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatbotService", "Error generating response", e)
                if (isSinhalaOrSinglish(message)) {
                    BotResponse(text = "සමාවෙන්න, දැනට මට පිළිතුරු දීමට නොහැකිය. පසුව නැවත උත්සාහ කරන්න.")
                } else {
                    BotResponse(text = "Sorry, I'm unable to respond right now. Please try again later.")
                }
            }
        }
    }
}