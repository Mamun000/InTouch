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

class ScanHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tvTotalScans: TextView
    private lateinit var tvUniqueContacts: TextView
    private lateinit var btnClearHistory: ImageView
    private lateinit var adapter: ScanHistoryAdapter
    private val contactsList = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_history)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        loadScanHistory()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        layoutEmptyState = findViewById(R.id.tvEmptyState)
        tvTotalScans = findViewById(R.id.tvTotalScans)
        tvUniqueContacts = findViewById(R.id.tvUniqueContacts)
        btnClearHistory = findViewById(R.id.btnClearHistory)
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
        adapter = ScanHistoryAdapter(contactsList) { contact ->
            openContactCard(contact)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ScanHistoryActivity)
            adapter = this@ScanHistoryActivity.adapter
        }
    }

    private fun setupButtons() {
        btnClearHistory.setOnClickListener {
            showClearConfirmation()
        }

        val btnStartScanning = layoutEmptyState.findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnStartScanning)
        btnStartScanning?.setOnClickListener {
            finish()
        }
    }

    private fun loadScanHistory() {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("scanHistory").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    contactsList.clear()

                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        showEmptyState()
                        updateStats()
                        return
                    }

                    var loadedCount = 0
                    val totalItems = snapshot.childrenCount.toInt()

                    for (contactSnapshot in snapshot.children) {
                        val scannedUserId = contactSnapshot.child("scannedUserId").value?.toString()
                        val timestamp = contactSnapshot.child("timestamp").value as? Long ?: 0L

                        if (scannedUserId != null) {
                            loadContactDetails(scannedUserId, timestamp) {
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
                        this@ScanHistoryActivity,
                        "Error loading history: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadContactDetails(userId: String, timestamp: Long, onComplete: () -> Unit) {
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

                        val contact = Contact(
                            userId = userId,
                            fullName = fullName,
                            profession = profession,
                            email = email,
                            phone = phone,
                            profileImage = profileBitmap,
                            timestamp = timestamp
                        )

                        contactsList.add(contact)
                        // Sort by timestamp (most recent first)
                        contactsList.sortByDescending { it.timestamp }
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
        tvTotalScans.text = contactsList.size.toString()
        tvUniqueContacts.text = contactsList.distinctBy { it.userId }.size.toString()
    }

    private fun openContactCard(contact: Contact) {
        val intent = Intent(this, ViewCardActivity::class.java)
        intent.putExtra("USER_ID", contact.userId)
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

    private fun showClearConfirmation() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Clear Scan History?")
            setMessage("This action cannot be undone. All scan history will be permanently deleted.")
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            setPositiveButton("Delete") { dialog, _ ->
                clearScanHistory()
                dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun clearScanHistory() {
        val userId = auth.currentUser?.uid ?: return

        database.reference.child("scanHistory").child(userId)
            .removeValue()
            .addOnSuccessListener {
                contactsList.clear()
                adapter.notifyDataSetChanged()
                updateStats()
                showEmptyState()
                Toast.makeText(this, "Scan history cleared", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to clear history", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

// Data class for Contact
data class Contact(
    val userId: String,
    val fullName: String,
    val profession: String,
    val email: String,
    val phone: String,
    val profileImage: Bitmap?,
    val timestamp: Long
)