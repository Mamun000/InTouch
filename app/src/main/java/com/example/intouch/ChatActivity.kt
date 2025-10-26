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

                // Clear input
                etMessage.setText("")
                Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Failed to send message: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
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