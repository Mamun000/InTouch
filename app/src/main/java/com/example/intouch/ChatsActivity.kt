package com.example.intouch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import android.widget.LinearLayout


class ChatsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: LinearLayout
    private lateinit var adapter: ChatsAdapter
    private val chatsList = mutableListOf<ChatItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadChats()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmptyState = findViewById(R.id.tvEmptyState)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Chats"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatsAdapter(
            chatsList,
            onChatClick = { chatItem ->
                openChat(chatItem)
            },
            onChatLongClick = { chatItem ->
                showDeleteChatDialog(chatItem)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Load chats from BOTH sources:
        // 1. Chats where current user has sent/received messages
        // 2. Users in scan history (even without messages)
        
        loadFromScanHistory(currentUserId)
        loadFromChats(currentUserId)
        
        // Show empty state if both sources have no data after a short delay
        recyclerView.postDelayed({
            if (chatsList.isEmpty()) {
                showEmptyState()
            }
        }, 500)
    }
    
    private fun loadFromChats(currentUserId: String) {
        // Load chats where current user is a participant
        database.reference.child("chats")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        return
                    }

                    // Get all chat IDs where current user is a participant
                    val processedUsers = mutableSetOf<String>()
                    
                    for (chatSnapshot in snapshot.children) {
                        val chatId = chatSnapshot.key ?: continue
                        
                        // Parse chat ID to get participants (format: userId1_userId2)
                        val participantIds = chatId.split("_")
                        if (participantIds.size == 2) {
                            val user1 = participantIds[0]
                            val user2 = participantIds[1]
                            
                            // Determine which one is the friend (not current user)
                            val friendUserId = when {
                                user1 == currentUserId -> user2
                                user2 == currentUserId -> user1
                                else -> null
                            }
                            
                            // Only load once per friend
                            if (friendUserId != null && !processedUsers.contains(friendUserId)) {
                                processedUsers.add(friendUserId)
                                loadChatItem(currentUserId, friendUserId)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
    
    private fun loadFromScanHistory(currentUserId: String) {
        // Load from scan history to get all contacts
        database.reference.child("scanHistory").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        // Don't show empty state here - let loadFromChats handle it
                        return
                    }

                    for (contactSnapshot in snapshot.children) {
                        // The key IS the scannedUserId
                        val friendUserId = contactSnapshot.key

                        if (friendUserId != null) {
                            // Only show chats from scan history if connection is accepted
                            database.reference.child("savedCards").child(currentUserId).child(friendUserId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(savedSnapshot: DataSnapshot) {
                                        if (savedSnapshot.exists()) {
                                            loadChatItem(currentUserId, friendUserId)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error silently
                }
            })
    }

    private fun loadChatItem(currentUserId: String, friendUserId: String) {
        val chatId = getChatId(currentUserId, friendUserId)
        
        // Check if current user has deleted this chat
        database.reference.child("chats").child(chatId)
            .child("deletedBy").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // If chat is deleted by current user, don't load it
                    if (snapshot.exists() && snapshot.getValue(Boolean::class.java) == true) {
                        // Remove from list if it exists
                        chatsList.removeAll { it.chatId == chatId }
                        adapter.notifyDataSetChanged()
                        return
                    }

                    // Load friend details
                    database.reference.child("users").child(friendUserId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                if (userSnapshot.exists()) {
                                    val fullName = userSnapshot.child("fullName").value?.toString() ?: "Unknown"
                                    val profession = userSnapshot.child("profession").value?.toString() ?: ""
                                    val profileImageData = userSnapshot.child("profileImage").value?.toString()

                                    var profileBitmap: Bitmap? = null
                                    if (!profileImageData.isNullOrEmpty()) {
                                        try {
                                            profileBitmap = base64ToBitmap(profileImageData)
                                        } catch (e: Exception) {
                                            // Use default
                                        }
                                    }

                                    // Listen for last message changes
                                    database.reference.child("chats").child(chatId).child("lastMessage")
                                        .addValueEventListener(object : ValueEventListener {
                                            override fun onDataChange(msgSnapshot: DataSnapshot) {
                                                val lastMessage = msgSnapshot.child("text").value?.toString() ?: "Say Hi 👋"
                                                val timestamp = msgSnapshot.child("timestamp").value as? Long ?: 0L
                                                val senderId = msgSnapshot.child("senderId").value?.toString() ?: ""

                                                // Load unread count from Firebase (use ValueEventListener for real-time updates)
                                                database.reference.child("chats").child(chatId)
                                                    .child("unreadCount").child(currentUserId)
                                                    .addValueEventListener(object : ValueEventListener {
                                                        override fun onDataChange(unreadSnapshot: DataSnapshot) {
                                                            val unreadCount = unreadSnapshot.getValue(Int::class.java) ?: 0

                                                            val chatItem = ChatItem(
                                                                chatId = chatId,
                                                                friendUserId = friendUserId,
                                                                friendName = fullName,
                                                                friendProfession = profession,
                                                                profileImage = profileBitmap,
                                                                lastMessage = lastMessage,
                                                                timestamp = timestamp,
                                                                unreadCount = unreadCount,
                                                                isOwnMessage = senderId == currentUserId
                                                            )

                                                            // Update or add chat item
                                                            val existingIndex = chatsList.indexOfFirst { it.friendUserId == friendUserId }
                                                            if (existingIndex != -1) {
                                                                chatsList[existingIndex] = chatItem
                                                            } else {
                                                                chatsList.add(chatItem)
                                                            }

                                                            // Sort by timestamp
                                                            chatsList.sortByDescending { it.timestamp }
                                                            adapter.notifyDataSetChanged()
                                                            hideEmptyState()
                                                        }

                                                        override fun onCancelled(error: DatabaseError) {}
                                                    })
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                // Handle error
                                            }
                                        })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle error
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error - still try to load the chat
                }
            })
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun openChat(chatItem: ChatItem) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("FRIEND_USER_ID", chatItem.friendUserId)
        intent.putExtra("FRIEND_NAME", chatItem.friendName)
        intent.putExtra("CHAT_ID", chatItem.chatId)
        startActivity(intent)
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        tvEmptyState.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        recyclerView.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
    }

    private fun showDeleteChatDialog(chatItem: ChatItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Chat?")
            .setMessage("Are you sure you want to delete the chat with ${chatItem.friendName}? This chat will be hidden from your chats list.")
            .setPositiveButton("Delete") { _, _ ->
                deleteChat(chatItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteChat(chatItem: ChatItem) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Mark chat as deleted by current user
        database.reference.child("chats").child(chatItem.chatId)
            .child("deletedBy")
            .child(currentUserId)
            .setValue(true)
            .addOnSuccessListener {
                // Check if both users have deleted the chat
                database.reference.child("chats").child(chatItem.chatId)
                    .child("deletedBy")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val deletedUsers = snapshot.children.count()
                            val participantIds = chatItem.chatId.split("_")
                            
                            // If both users deleted the chat, delete it completely
                            if (deletedUsers >= 2 && participantIds.size == 2) {
                                val user1 = participantIds[0]
                                val user2 = participantIds[1]
                                
                                // Get all deleted user IDs
                                val deletedUserIds = mutableListOf<String>()
                                snapshot.children.forEach {
                                    if (it.value == true) {
                                        deletedUserIds.add(it.key ?: "")
                                    }
                                }
                                
                                // If both participants deleted, remove entire chat
                                if (deletedUserIds.contains(user1) && deletedUserIds.contains(user2)) {
                                    database.reference.child("chats").child(chatItem.chatId)
                                        .removeValue()
                                        .addOnSuccessListener {
                                            removeChatFromList(chatItem)
                                        }
                                } else {
                                    removeChatFromList(chatItem)
                                }
                            } else {
                                removeChatFromList(chatItem)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            removeChatFromList(chatItem)
                        }
                    })
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeChatFromList(chatItem: ChatItem) {
        chatsList.removeAll { it.chatId == chatItem.chatId }
        adapter.notifyDataSetChanged()
        
        if (chatsList.isEmpty()) {
            showEmptyState()
        }
        
        Toast.makeText(this, "Chat deleted", Toast.LENGTH_SHORT).show()
    }
}

data class ChatItem(
    val chatId: String,
    val friendUserId: String,
    val friendName: String,
    val friendProfession: String,
    val profileImage: Bitmap?,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int,
    val isOwnMessage: Boolean
)