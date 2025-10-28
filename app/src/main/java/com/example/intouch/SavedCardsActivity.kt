package com.example.intouch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SavedCardsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvTotalCards: TextView
    private lateinit var adapter: SavedCardsAdapter
    private val savedCardsList = mutableListOf<SavedCard>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_cards)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        loadSavedCards()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        tvTotalCards = findViewById(R.id.tvTotalCards)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setTitle("")
        }
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = SavedCardsAdapter(
            savedCardsList,
            onCardClick = { card ->
                openCardDetail(card)
            },
            onDeleteClick = { card ->
                showDeleteConfirmation(card)
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SavedCardsActivity)
            adapter = this@SavedCardsActivity.adapter
        }
    }

    private fun loadSavedCards() {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("savedCards").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    savedCardsList.clear()

                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        showEmptyState()
                        updateStats()
                        return
                    }

                    var loadedCount = 0
                    val totalItems = snapshot.childrenCount.toInt()

                    for (cardSnapshot in snapshot.children) {
                        val cardUserId = cardSnapshot.child("cardUserId").value?.toString()
                        val timestamp = cardSnapshot.child("timestamp").value as? Long ?: 0L

                        if (cardUserId != null) {
                            loadCardDetails(cardUserId, timestamp) {
                                loadedCount++
                                if (loadedCount == totalItems) {
                                    updateStats()
                                    hideEmptyState()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@SavedCardsActivity,
                        "Error loading saved cards: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadCardDetails(userId: String, timestamp: Long, onComplete: () -> Unit) {
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val fullName = snapshot.child("fullName").value?.toString() ?: "Unknown"
                        val profession = snapshot.child("profession").value?.toString() ?: ""
                        val email = snapshot.child("email").value?.toString() ?: ""
                        val phone = snapshot.child("phone").value?.toString() ?: ""
                        val profileImageData = snapshot.child("profileImage").value?.toString()

                        var profileBitmap: Bitmap? = null
                        if (!profileImageData.isNullOrEmpty()) {
                            try {
                                profileBitmap = base64ToBitmap(profileImageData)
                            } catch (e: Exception) {
                                // Use default image
                            }
                        }

                        val card = SavedCard(
                            userId = userId,
                            fullName = fullName,
                            profession = profession,
                            email = email,
                            phone = phone,
                            profileImage = profileBitmap,
                            timestamp = timestamp
                        )

                        savedCardsList.add(card)
                        // Sort by timestamp (most recent first)
                        savedCardsList.sortByDescending { it.timestamp }
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

    private fun updateStats() {
        tvTotalCards.text = savedCardsList.size.toString()
    }

    private fun openCardDetail(card: SavedCard) {
        val intent = Intent(this, ViewCardActivity::class.java)
        intent.putExtra("USER_ID", card.userId)
        startActivity(intent)
    }

    private fun showDeleteConfirmation(card: SavedCard) {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Delete Saved Card?")
            setMessage("Are you sure you want to remove ${card.fullName} from your saved cards?")
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton("Delete") { dialog, _ ->
                deleteSavedCard(card)
                dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun deleteSavedCard(card: SavedCard) {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("savedCards").child(userId).child(card.userId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Card removed from saved cards", Toast.LENGTH_SHORT).show()
                savedCardsList.remove(card)
                adapter.notifyDataSetChanged()
                updateStats()
                if (savedCardsList.isEmpty()) {
                    showEmptyState()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove card", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {
        recyclerView.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

