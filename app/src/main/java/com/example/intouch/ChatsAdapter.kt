package com.example.intouch


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ChatsAdapter(
    private val chats: List<ChatItem>,
    private val onChatClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvUnreadCount: TextView = itemView.findViewById(R.id.tvUnreadCount)

        fun bind(chat: ChatItem) {
            tvName.text = chat.friendName

            // Show "You: " prefix if it's own message
            tvLastMessage.text = if (chat.isOwnMessage) {
                "You: ${chat.lastMessage}"
            } else {
                chat.lastMessage
            }

            tvTimestamp.text = formatTimestamp(chat.timestamp)

            if (chat.profileImage != null) {
                ivProfile.setImageBitmap(chat.profileImage)
            } else {
                ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // Show/hide unread count
            if (chat.unreadCount > 0) {
                tvUnreadCount.visibility = View.VISIBLE
                tvUnreadCount.text = chat.unreadCount.toString()
            } else {
                tvUnreadCount.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp == 0L) return ""

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m"
                diff < 86400000 -> "${diff / 3600000}h"
                diff < 604800000 -> "${diff / 86400000}d"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size
}