package com.example.intouch

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: ChatMessagesAdapter
    private val messagesList = mutableListOf<Message>()

    private var friendUserId: String = ""
    private var friendName: String = ""
    private var chatId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        friendUserId = intent.getStringExtra("FRIEND_USER_ID") ?: ""
        friendName = intent.getStringExtra("FRIEND_NAME") ?: "Chat"
        chatId = intent.getStringExtra("CHAT_ID") ?: ""

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupSendButton()
        loadMessages()
        markMessagesAsRead()
    }

    private fun initializeViews() {
        try {
            toolbar = findViewById(R.id.toolbar)
            recyclerView = findViewById(R.id.recyclerView)
            etMessage = findViewById(R.id.etMessage)
            btnSend = findViewById(R.id.btnSend)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = friendName
        }
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""
        adapter = ChatMessagesAdapter(messagesList, currentUserId)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    private fun setupSendButton() {
        btnSend.setOnClickListener {
            sendMessage()
        }
    }

    private fun loadMessages() {
        database.reference.child("chats").child(chatId).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()

                    for (msgSnapshot in snapshot.children) {
                        val message = msgSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            messagesList.add(message)
                        }
                    }

                    adapter.notifyDataSetChanged()
                    if (messagesList.isNotEmpty()) {
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ChatActivity,
                        "Error loading messages: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please type a message", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if both users have deleted the chat (fresh start)
        database.reference.child("chats").child(chatId)
            .child("deletedBy")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user1 = chatId.split("_")[0]
                    val user2 = chatId.split("_")[1]
                    
                    val user1Deleted = snapshot.child(user1).getValue(Boolean::class.java) ?: false
                    val user2Deleted = snapshot.child(user2).getValue(Boolean::class.java) ?: false
                    
                    // If both users deleted, clean up old messages and deletedBy
                    if (user1Deleted && user2Deleted) {
                        // Remove old messages
                        database.reference.child("chats").child(chatId).child("messages")
                            .removeValue()
                        
                        // Remove deletedBy flag
                        database.reference.child("chats").child(chatId).child("deletedBy")
                            .removeValue()
                    }
                    
                    // Proceed to send message
                    sendMessageToFirebase(messageText, currentUserId)
                }

                override fun onCancelled(error: DatabaseError) {
                    // If error, still try to send message
                    sendMessageToFirebase(messageText, currentUserId)
                }
            })
    }
    
    private fun sendMessageToFirebase(messageText: String, currentUserId: String) {
        val timestamp = System.currentTimeMillis()
        val messageId = database.reference.child("chats").child(chatId).child("messages").push().key ?: run {
            Toast.makeText(this, "Failed to generate message ID", Toast.LENGTH_SHORT).show()
            return
        }

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = friendUserId,
            text = messageText,
            timestamp = timestamp
        )

        // Save message to Firebase
        database.reference.child("chats").child(chatId).child("messages").child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                // Update last message info
                val lastMessageData = mapOf(
                    "text" to messageText,
                    "timestamp" to timestamp,
                    "senderId" to currentUserId
                )

                database.reference.child("chats").child(chatId).child("lastMessage")
                    .setValue(lastMessageData)

                // Increment unread count for the receiver (only if receiver is not the sender)
                if (friendUserId != currentUserId) {
                    database.reference.child("chats").child(chatId)
                        .child("unreadCount").child(friendUserId)
                        .get().addOnSuccessListener { snapshot ->
                            val currentCount = snapshot.getValue(Int::class.java) ?: 0
                            database.reference.child("chats").child(chatId)
                                .child("unreadCount").child(friendUserId)
                                .setValue(currentCount + 1)
                        }
                }

                // Clear input
                etMessage.setText("")
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to send message: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun markMessagesAsRead() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Reset unread count to 0 when chat is opened
        database.reference.child("chats").child(chatId)
            .child("unreadCount").child(currentUserId)
            .setValue(0)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)