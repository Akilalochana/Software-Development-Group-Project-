package com.example.arlandmeasuretest33

import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val hasButton: Boolean = false,
    val buttonText: String = "",
    val featureName: String = ""
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}