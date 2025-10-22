package com.example.intouch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class ViewCardActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvFullName: TextView
    private lateinit var tvProfession: TextView
    private lateinit var tvOrganization: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvLinkedIn: TextView
    private lateinit var tvGitHub: TextView
    private lateinit var layoutPhone: LinearLayout
    private lateinit var layoutLinkedIn: LinearLayout
    private lateinit var layoutGitHub: LinearLayout
    private lateinit var layoutOrganization: LinearLayout
    private lateinit var ivQRCode: ImageView
    private lateinit var layoutQRCode: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_card)

        database = FirebaseDatabase.getInstance()

        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        tvFullName = findViewById(R.id.tvFullName)
        tvProfession = findViewById(R.id.tvProfession)
        tvOrganization = findViewById(R.id.tvOrganization)
        tvBio = findViewById(R.id.tvBio)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvLinkedIn = findViewById(R.id.tvLinkedIn)
        tvGitHub = findViewById(R.id.tvGitHub)
        layoutPhone = findViewById(R.id.layoutPhone)
        layoutLinkedIn = findViewById(R.id.layoutLinkedIn)
        layoutGitHub = findViewById(R.id.layoutGitHub)
        layoutOrganization = findViewById(R.id.layoutOrganization)
        ivQRCode = findViewById(R.id.ivQRCode)
        layoutQRCode = findViewById(R.id.layoutQRCode)

        val userId = intent.getStringExtra("USER_ID")
        val showQR = intent.getBooleanExtra("SHOW_QR", false)

        if (userId != null) {
            loadUserData(userId)
            if (showQR) {
                generateQRCode(userId)
                layoutQRCode.visibility = View.VISIBLE
            } else {
                layoutQRCode.visibility = View.GONE
            }
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
        }
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
                        tvFullName.text = snapshot.child("fullName").value?.toString() ?: "N/A"
                        tvProfession.text = snapshot.child("profession").value?.toString() ?: "N/A"
                        tvBio.text = snapshot.child("bio").value?.toString() ?: "N/A"
                        tvEmail.text = snapshot.child("email").value?.toString() ?: "N/A"

                        // Load profile picture if exists
                        val profileImageData = snapshot.child("profileImage").value?.toString()
                        if (!profileImageData.isNullOrEmpty()) {
                            try {
                                val bitmap = base64ToBitmap(profileImageData)
                                ivProfilePicture.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery)
                            }
                        } else {
                            ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery)
                        }

                        val organization = snapshot.child("organization").value?.toString()
                        if (!organization.isNullOrEmpty()) {
                            tvOrganization.text = organization
                            layoutOrganization.visibility = View.VISIBLE
                        } else {
                            layoutOrganization.visibility = View.GONE
                        }

                        val phone = snapshot.child("phone").value?.toString()
                        if (!phone.isNullOrEmpty()) {
                            tvPhone.text = phone
                            layoutPhone.visibility = View.VISIBLE
                        } else {
                            layoutPhone.visibility = View.GONE
                        }

                        val linkedIn = snapshot.child("linkedIn").value?.toString()
                        if (!linkedIn.isNullOrEmpty()) {
                            tvLinkedIn.text = linkedIn
                            layoutLinkedIn.visibility = View.VISIBLE
                        } else {
                            layoutLinkedIn.visibility = View.GONE
                        }

                        val gitHub = snapshot.child("gitHub").value?.toString()
                        if (!gitHub.isNullOrEmpty()) {
                            tvGitHub.text = gitHub
                            layoutGitHub.visibility = View.VISIBLE
                        } else {
                            layoutGitHub.visibility = View.GONE
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