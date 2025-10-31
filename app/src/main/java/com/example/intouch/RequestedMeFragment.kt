package com.example.intouch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Intent

class RequestedMeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var adapter: RequestAdapter
    private val requestsList = mutableListOf<ConnectionRequest>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Apply theme
        val prefs = requireContext().getSharedPreferences("AppPreferences", AppCompatActivity.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        return inflater.inflate(R.layout.fragment_requests, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        recyclerView = view.findViewById(R.id.recyclerView)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle)

        tvEmptyTitle.text = "No Incoming Requests"
        tvEmptySubtitle.text = "You haven't received any connection requests yet"

        setupRecyclerView()
        loadRequests()
    }

    private fun setupRecyclerView() {
        adapter = RequestAdapter(
            requestsList,
            onAcceptClick = { request ->
                acceptRequest(request)
            },
            onRejectClick = { request ->
                rejectRequest(request)
            },
            onCardClick = { request ->
                viewProfile(request.requesterId)
            },
            showActions = true // Show accept/reject buttons
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RequestedMeFragment.adapter
        }
    }

    private fun loadRequests() {
        val userId = auth.currentUser?.uid ?: return

        // Load all requests and filter in code (since Firebase queries have limitations)
        database.reference.child("connectionRequests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestsList.clear()

                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        showEmptyState()
                        return
                    }

                    var pendingCount = 0
                    var loadedCount = 0

                    for (requestSnapshot in snapshot.children) {
                        val requestedId = requestSnapshot.child("requestedId").value?.toString()
                        val status = requestSnapshot.child("status").value?.toString() ?: "pending"
                        
                        // Only show pending requests where current user is the requested user
                        if (requestedId == userId && status == "pending") {
                            pendingCount++
                            val requesterId = requestSnapshot.child("requesterId").value?.toString()
                            val timestamp = requestSnapshot.child("timestamp").value as? Long ?: 0L
                            val requestId = requestSnapshot.key ?: ""

                            if (requesterId != null) {
                                loadRequesterDetails(requesterId, requestId, timestamp) {
                                    loadedCount++
                                    if (loadedCount == pendingCount && pendingCount > 0) {
                                        hideEmptyState()
                                    } else if (pendingCount == 0) {
                                        showEmptyState()
                                    }
                                }
                            }
                        }
                    }

                    if (pendingCount == 0) {
                        showEmptyState()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showEmptyState()
                }
            })
    }

    private fun loadRequesterDetails(
        requesterId: String,
        requestId: String,
        timestamp: Long,
        onComplete: () -> Unit
    ) {
        database.reference.child("users").child(requesterId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val fullName = snapshot.child("fullName").value?.toString() ?: "Unknown"
                        val profession = snapshot.child("profession").value?.toString()
                        val email = snapshot.child("email").value?.toString()
                        val profileImageData = snapshot.child("profileImage").value?.toString()

                        var profileBitmap: Bitmap? = null
                        if (!profileImageData.isNullOrEmpty()) {
                            try {
                                profileBitmap = base64ToBitmap(profileImageData)
                            } catch (e: Exception) {
                                // Use default image
                            }
                        }

                        val request = ConnectionRequest(
                            requestId = requestId,
                            requesterId = requesterId,
                            requestedId = auth.currentUser?.uid ?: "",
                            requesterName = fullName,
                            requesterProfession = profession,
                            requesterEmail = email,
                            requesterProfileImage = profileBitmap,
                            timestamp = timestamp,
                            status = "pending"
                        )

                        requestsList.add(request)
                        requestsList.sortByDescending { it.timestamp }
                        adapter.notifyDataSetChanged()
                    }
                    onComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete()
                }
            })
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun acceptRequest(request: ConnectionRequest) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Update request status
        database.reference.child("connectionRequests").child(request.requestId)
            .child("status").setValue("accepted")
            .addOnSuccessListener {
                // Save to saved cards for requester (they can now see your card)
                saveCardToRequester(request.requesterId, currentUserId)

                // Save requester's card to your saved cards
                saveRequesterCard(currentUserId, request.requesterId)

                // Ensure chat exists for both users
                createChatIfNotExists(currentUserId, request.requesterId)

                // Remove from list
                requestsList.remove(request)
                adapter.notifyDataSetChanged()

                if (requestsList.isEmpty()) {
                    showEmptyState()
                }
            }
    }

    private fun createChatIfNotExists(userA: String, userB: String) {
        val chatId = if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
        val chatRef = database.reference.child("chats").child(chatId)

        chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val initData = mapOf(
                        "createdAt" to System.currentTimeMillis(),
                        "participants" to mapOf(
                            userA to true,
                            userB to true
                        ),
                        "unreadCount" to mapOf(
                            userA to 0,
                            userB to 0
                        )
                    )
                    chatRef.setValue(initData)
                }
            }

            override fun onCancelled(error: DatabaseError) { }
        })
    }

    private fun rejectRequest(request: ConnectionRequest) {
        // On rejection, clear the request entirely (both directions) so a new scan can recreate it
        val ref = database.reference.child("connectionRequests")
        val reverseId = "${request.requestedId}_${request.requesterId}"

        // Remove both entries (idempotent)
        ref.child(request.requestId).removeValue()
        ref.child(reverseId).removeValue()

        // Update UI
        requestsList.remove(request)
        adapter.notifyDataSetChanged()
        if (requestsList.isEmpty()) {
            showEmptyState()
        }
    }

    fun clearAllIncomingRequests() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ref = database.reference.child("connectionRequests")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = hashMapOf<String, Any?>()
                for (req in snapshot.children) {
                    val requestedId = req.child("requestedId").value?.toString()
                    if (requestedId == currentUserId) {
                        updates[req.key ?: continue] = null
                    }
                }
                if (updates.isNotEmpty()) {
                    ref.updateChildren(updates)
                }
                requestsList.clear()
                adapter.notifyDataSetChanged()
                showEmptyState()
            }
            override fun onCancelled(error: DatabaseError) { }
        })
    }

    private fun saveCardToRequester(requesterId: String, cardUserId: String) {
        val timestamp = System.currentTimeMillis()
        val savedCardData = mapOf(
            "cardUserId" to cardUserId,
            "timestamp" to timestamp
        )

        database.reference.child("savedCards").child(requesterId).child(cardUserId)
            .setValue(savedCardData)
    }

    private fun saveRequesterCard(currentUserId: String, cardUserId: String) {
        val timestamp = System.currentTimeMillis()
        val savedCardData = mapOf(
            "cardUserId" to cardUserId,
            "timestamp" to timestamp
        )

        database.reference.child("savedCards").child(currentUserId).child(cardUserId)
            .setValue(savedCardData)
    }

    private fun viewProfile(userId: String) {
        val intent = Intent(requireContext(), ViewCardActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        recyclerView.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
    }
}

