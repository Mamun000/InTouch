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

class IRequestedFragment : Fragment() {

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

        tvEmptyTitle.text = "No Outgoing Requests"
        tvEmptySubtitle.text = "You haven't sent any connection requests yet"

        setupRecyclerView()
        loadRequests()
    }

    private fun setupRecyclerView() {
        adapter = RequestAdapter(
            requestsList,
            onAcceptClick = { },
            onRejectClick = { },
            onCardClick = { request ->
                // If accepted, view their card, otherwise just show info
                viewProfile(request.requestedId)
            },
            showActions = false // Don't show accept/reject buttons (you're the requester)
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@IRequestedFragment.adapter
        }
    }

    private fun loadRequests() {
        val userId = auth.currentUser?.uid ?: return

        // Load all requests and filter in code
        database.reference.child("connectionRequests")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requestsList.clear()

                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        showEmptyState()
                        return
                    }

                    var requestCount = 0
                    var loadedCount = 0

                    for (requestSnapshot in snapshot.children) {
                        val requesterId = requestSnapshot.child("requesterId").value?.toString()
                        
                        // Only show requests where current user is the requester
                        if (requesterId == userId) {
                            requestCount++
                            val requestedId = requestSnapshot.child("requestedId").value?.toString()
                            val status = requestSnapshot.child("status").value?.toString() ?: "pending"
                            val timestamp = requestSnapshot.child("timestamp").value as? Long ?: 0L
                            val requestId = requestSnapshot.key ?: ""

                            if (requestedId != null) {
                                loadRequestedUserDetails(requestedId, requestId, timestamp, status) {
                                    loadedCount++
                                    if (loadedCount == requestCount && requestCount > 0) {
                                        hideEmptyState()
                                    } else if (requestCount == 0) {
                                        showEmptyState()
                                    }
                                }
                            }
                        }
                    }

                    if (requestCount == 0) {
                        showEmptyState()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showEmptyState()
                }
            })
    }

    private fun loadRequestedUserDetails(
        requestedId: String,
        requestId: String,
        timestamp: Long,
        status: String,
        onComplete: () -> Unit
    ) {
        database.reference.child("users").child(requestedId)
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

                        // Note: In "I Requested", requestedId is the person YOU requested
                        // So we show their info, but they are the "requested" user
                        val request = ConnectionRequest(
                            requestId = requestId,
                            requesterId = auth.currentUser?.uid ?: "",
                            requestedId = requestedId,
                            requesterName = fullName, // The person you requested
                            requesterProfession = profession,
                            requesterEmail = email,
                            requesterProfileImage = profileBitmap,
                            timestamp = timestamp,
                            status = status
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

    private fun viewProfile(userId: String) {
        // Find the request to check status
        val currentUserId = auth.currentUser?.uid ?: return
        val requestId = "${currentUserId}_${userId}"
        
        database.reference.child("connectionRequests").child(requestId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.child("status").value?.toString() ?: "pending"
                        
                        if (status == "accepted") {
                            // Request was accepted, can view card
                            val intent = Intent(requireContext(), ViewCardActivity::class.java)
                            intent.putExtra("USER_ID", userId)
                            startActivity(intent)
                        } else {
                            // Request not accepted yet
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Card will be available after the request is accepted",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Request not found",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun clearAllOutgoingRequests() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ref = database.reference.child("connectionRequests")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = hashMapOf<String, Any?>()
                for (req in snapshot.children) {
                    val requesterId = req.child("requesterId").value?.toString()
                    if (requesterId == currentUserId) {
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

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        recyclerView.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
    }
}

