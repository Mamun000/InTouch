// SuggestionDetailActivity.kt
// This activity shows suggested user profile with option to connect

package com.example.intouch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SuggestionDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var ivProfile: ImageView
    private lateinit var tvName: TextView
    //private lateinit var tvSkills: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnViewProfile: Button
    private var friendName: String = ""
    private var suggestedUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggestion_detail)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        ivProfile = findViewById(R.id.ivProfile)
        tvName = findViewById(R.id.tvName)
       // tvSkills = findViewById(R.id.tvSkills)
        tvBio = findViewById(R.id.tvBio)
        tvEmail = findViewById(R.id.tvEmail)
        btnConnect = findViewById(R.id.btnConnect)
        btnViewProfile = findViewById(R.id.btnViewProfile)

        suggestedUserId = intent.getStringExtra("USER_ID") ?: ""

        if (suggestedUserId.isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserDetails(suggestedUserId)

        btnConnect.setOnClickListener {
            // Start chat immediately
            startChatWithUser(suggestedUserId)
            // Also create connection in background (non-blocking)
            createConnection(suggestedUserId)
        }

        btnViewProfile.setOnClickListener {
            val intent = Intent(this, ViewCardActivity::class.java)
            intent.putExtra("USER_ID", suggestedUserId)
            startActivity(intent)
        }
    }

    private fun loadUserDetails(userId: String) {
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val fullName = snapshot.child("fullName").value?.toString() ?: "Unknown"
                        val email = snapshot.child("email").value?.toString() ?: ""
                        val skills = snapshot.child("skills").value?.toString() ?: "Not specified"
                        val bio = snapshot.child("bio").value?.toString() ?: ""
                        val profileImage = snapshot.child("profileImage").value?.toString()

                        friendName = fullName
                        tvName.text = fullName
                        tvEmail.text = email
                        //tvSkills.text = "Skills: $skills"
                        tvBio.text = bio

                        if (!profileImage.isNullOrEmpty()) {
                            try {
                                val bitmap = base64ToBitmap(profileImage)
                                ivProfile.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun createConnection(suggestedUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Create connection record
        val connectionData = mapOf(
            "connectedWith" to suggestedUserId,
            "timestamp" to System.currentTimeMillis(),
            "source" to "chatbot_suggestion"
        )

        database.reference.child("connections")
            .child(currentUserId)
            .child(suggestedUserId)
            .setValue(connectionData)
            .addOnSuccessListener {
                // Also add reverse connection
                val reverseConnection = mapOf(
                    "connectedWith" to currentUserId,
                    "timestamp" to System.currentTimeMillis(),
                    "source" to "chatbot_suggestion"
                )

                database.reference.child("connections")
                    .child(suggestedUserId)
                    .child(currentUserId)
                    .setValue(reverseConnection)
                    .addOnSuccessListener {
                        // Start chat with the person
                        startChatWithUser(suggestedUserId)
                    }
                    .addOnFailureListener { e ->
                        // Even if reverse connection fails, still start chat
                        startChatWithUser(suggestedUserId)
                    }
            }
            .addOnFailureListener { e ->
                // Even if connection creation fails, still start chat
                startChatWithUser(suggestedUserId)
            }
    }
    
    private fun startChatWithUser(suggestedUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Get friend's name (use stored name or fallback to displayed name)
        val nameToUse = if (friendName.isNotEmpty()) friendName else tvName.text.toString()
        
        // Create chat ID (sort user IDs alphabetically and join with underscore)
        val chatId = if (currentUserId < suggestedUserId) {
            "${currentUserId}_${suggestedUserId}"
        } else {
            "${suggestedUserId}_${currentUserId}"
        }
        
        // Start chat activity
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("FRIEND_USER_ID", suggestedUserId)
        intent.putExtra("FRIEND_NAME", nameToUse)
        intent.putExtra("CHAT_ID", chatId)
        
        try {
            startActivity(intent)
            Toast.makeText(
                this,
                "Connected! Starting chat...",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error starting chat: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
        
        // Finish after starting the activity
        finish()
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
    
    fun onBackPressed(view: android.view.View) {
        finish()
    }
}


