package com.example.intouch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ChatMessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TYPE_USER)
            R.layout.item_chat_user
        else
            R.layout.item_chat_bot

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val messageCard: MaterialCardView = itemView.findViewById(R.id.messageCard)

        fun bind(message: ChatMessage) {
            messageText.text = message.text
        }
    }
}