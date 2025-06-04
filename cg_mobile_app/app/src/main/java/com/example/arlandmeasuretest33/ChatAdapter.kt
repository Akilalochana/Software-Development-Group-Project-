package com.example.arlandmeasuretest33

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.*

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_BOT = 2
        const val VIEW_TYPE_BOT_WITH_BUTTON = 3
    }

    private val messages = mutableListOf<ChatMessage>()
    
    // Interface for handling button clicks
    interface ButtonClickListener {
        fun onButtonClick(featureName: String)
    }
    
    // Button click listener
    private var buttonClickListener: ButtonClickListener? = null
    
    fun setButtonClickListener(listener: ButtonClickListener) {
        this.buttonClickListener = listener
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isFromUser -> VIEW_TYPE_USER
            message.hasButton -> VIEW_TYPE_BOT_WITH_BUTTON
            else -> VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_BOT_WITH_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_bot_with_button, parent, false)
                BotMessageWithButtonViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_bot, parent, false)
                BotMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is BotMessageViewHolder -> holder.bind(message)
            is BotMessageWithButtonViewHolder -> holder.bind(message)
        }
    }

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.message
            timeText.text = chatMessage.getFormattedTime()
        }
    }

    inner class BotMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.message
            timeText.text = chatMessage.getFormattedTime()
        }
    }
    
    inner class BotMessageWithButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val actionButton: MaterialButton = itemView.findViewById(R.id.actionButton)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.message
            timeText.text = chatMessage.getFormattedTime()
            
            // Configure the button
            actionButton.text = chatMessage.buttonText
            actionButton.visibility = if (chatMessage.hasButton) View.VISIBLE else View.GONE
            
            // Set button click listener
            actionButton.setOnClickListener {
                buttonClickListener?.onButtonClick(chatMessage.featureName)
            }
        }
    }
}