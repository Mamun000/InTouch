package com.example.intouch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class ViewCardActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var toolbar: Toolbar
    private lateinit var cardProfile: CardView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvFullName: TextView
    private lateinit var tvProfession: TextView
    private lateinit var cardBio: CardView
    private lateinit var tvBio: TextView
    private lateinit var cardContact: CardView
    private lateinit var layoutOrganization: LinearLayout
    private lateinit var tvOrganization: TextView
    private lateinit var layoutEmail: LinearLayout
    private lateinit var tvEmail: TextView
    private lateinit var layoutPhone: LinearLayout
    private lateinit var tvPhone: TextView
    private lateinit var cardSocial: CardView
    private lateinit var containerSocialLinks: LinearLayout
    private lateinit var cardQRCode: CardView
    private lateinit var ivQRCode: ImageView
    private lateinit var btnShare: Button
    private lateinit var btnSaveContact: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_card)

        database = FirebaseDatabase.getInstance()

        initializeViews()
        setupToolbar()

        val userId = intent.getStringExtra("USER_ID")
        val showQR = intent.getBooleanExtra("SHOW_QR", false)

        if (userId != null) {
            loadUserData(userId)
            if (showQR) {
                generateQRCode(userId)
                cardQRCode.visibility = View.VISIBLE
            } else {
                cardQRCode.visibility = View.GONE
            }
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupButtons()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        cardProfile = findViewById(R.id.cardProfile)
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        tvFullName = findViewById(R.id.tvFullName)
        tvProfession = findViewById(R.id.tvProfession)
        cardBio = findViewById(R.id.cardBio)
        tvBio = findViewById(R.id.tvBio)
        cardContact = findViewById(R.id.cardContact)
        layoutOrganization = findViewById(R.id.layoutOrganization)
        tvOrganization = findViewById(R.id.tvOrganization)
        layoutEmail = findViewById(R.id.layoutEmail)
        tvEmail = findViewById(R.id.tvEmail)
        layoutPhone = findViewById(R.id.layoutPhone)
        tvPhone = findViewById(R.id.tvPhone)
        cardSocial = findViewById(R.id.cardSocial)
        containerSocialLinks = findViewById(R.id.containerSocialLinks)
        cardQRCode = findViewById(R.id.cardQRCode)
        ivQRCode = findViewById(R.id.ivQRCode)
        btnShare = findViewById(R.id.btnShare)
        btnSaveContact = findViewById(R.id.btnSaveContact)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Digital Card"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        btnShare.setOnClickListener {
            shareCard()
        }

        btnSaveContact.setOnClickListener {
            Toast.makeText(this, "Save Contact - Feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareCard() {
        val userId = intent.getStringExtra("USER_ID")
        val shareText = "Check out my digital card: https://yourapp.com/card/$userId"

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share Card via"))
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun loadUserData(userId: String) {
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Basic Info
                        tvFullName.text = snapshot.child("fullName").value?.toString() ?: "N/A"
                        tvProfession.text = snapshot.child("profession").value?.toString() ?: "N/A"
                        tvBio.text = snapshot.child("bio").value?.toString() ?: "No bio available"

                        // Load profile picture
                        val profileImageData = snapshot.child("profileImage").value?.toString()
                        if (!profileImageData.isNullOrEmpty()) {
                            try {
                                val bitmap = base64ToBitmap(profileImageData)
                                ivProfilePicture.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                            }
                        } else {
                            ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                        }

                        // Organization
                        val organization = snapshot.child("organization").value?.toString()
                        if (!organization.isNullOrEmpty()) {
                            tvOrganization.text = organization
                            layoutOrganization.visibility = View.VISIBLE
                        } else {
                            layoutOrganization.visibility = View.GONE
                        }

                        // Email
                        val email = snapshot.child("email").value?.toString()
                        if (!email.isNullOrEmpty()) {
                            tvEmail.text = email
                            layoutEmail.visibility = View.VISIBLE
                        } else {
                            cardContact.visibility = View.GONE
                        }

                        // Phone
                        val phone = snapshot.child("phone").value?.toString()
                        if (!phone.isNullOrEmpty()) {
                            tvPhone.text = phone
                            layoutPhone.visibility = View.VISIBLE
                        } else {
                            layoutPhone.visibility = View.GONE
                        }

                        // Check if contact card should be visible
                        if (organization.isNullOrEmpty() && email.isNullOrEmpty() && phone.isNullOrEmpty()) {
                            cardContact.visibility = View.GONE
                        }

                        // Social Links
                        containerSocialLinks.removeAllViews()
                        val socialLinks = snapshot.child("socialLinks")
                        var hasSocialLinks = false
                        
                        if (socialLinks.exists()) {
                            socialLinks.children.forEach { linkSnapshot ->
                                val name = linkSnapshot.child("name").value?.toString() ?: ""
                                val url = linkSnapshot.child("url").value?.toString() ?: ""
                                
                                if (name.isNotEmpty() && url.isNotEmpty()) {
                                    addSocialLinkView(name, url)
                                    hasSocialLinks = true
                                }
                            }
                        } else {
                            // Backward compatibility: display old linkedIn and gitHub fields
                            val linkedIn = snapshot.child("linkedIn").value?.toString() ?: ""
                            val gitHub = snapshot.child("gitHub").value?.toString() ?: ""
                            
                            if (linkedIn.isNotEmpty()) {
                                addSocialLinkView("LinkedIn", linkedIn)
                                hasSocialLinks = true
                            }
                            if (gitHub.isNotEmpty()) {
                                addSocialLinkView("GitHub", gitHub)
                                hasSocialLinks = true
                            }
                        }
                        
                        // Check if social card should be visible
                        if (!hasSocialLinks) {
                            cardSocial.visibility = View.GONE
                        }

                    } else {
                        Toast.makeText(this@ViewCardActivity, "User data not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ViewCardActivity, "Error loading data", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun openUrl(url: String) {
        var finalUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            finalUrl = "https://$url"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addSocialLinkView(name: String, url: String) {
        val inflater = LayoutInflater.from(this)
        val linkView = inflater.inflate(R.layout.item_social_link_view, containerSocialLinks, false)
        
        val tvLinkLabel = linkView.findViewById<TextView>(R.id.tvLinkLabel)
        val tvLinkUrl = linkView.findViewById<TextView>(R.id.tvLinkUrl)
        
        tvLinkLabel.text = name
        tvLinkUrl.text = url
        
        linkView.setOnClickListener {
            openUrl(url)
        }
        
        containerSocialLinks.addView(linkView)
    }
    
    private fun generateQRCode(userId: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(userId, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            ivQRCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }
}