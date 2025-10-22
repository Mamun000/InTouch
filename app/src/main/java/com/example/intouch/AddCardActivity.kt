package com.example.intouch

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class AddCardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var nfcAdapter: NfcAdapter? = null
    private var nfcCardId: String? = null
    private var profileImageBase64: String? = null

    private lateinit var ivProfilePicture: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnRemoveImage: Button
    private lateinit var etFullName: EditText
    private lateinit var etProfession: EditText
    private lateinit var etOrganization: EditText
    private lateinit var etBio: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLinkedIn: EditText
    private lateinit var etGitHub: EditText
    private lateinit var cbNoNFC: CheckBox
    private lateinit var btnScanNFC: Button
    private lateinit var btnSave: Button
    private lateinit var tvNfcStatus: TextView

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val MAX_IMAGE_SIZE = 800 // Max width/height in pixels
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnRemoveImage = findViewById(R.id.btnRemoveImage)
        etFullName = findViewById(R.id.etFullName)
        etProfession = findViewById(R.id.etProfession)
        etOrganization = findViewById(R.id.etOrganization)
        etBio = findViewById(R.id.etBio)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etLinkedIn = findViewById(R.id.etLinkedIn)
        etGitHub = findViewById(R.id.etGitHub)
        cbNoNFC = findViewById(R.id.cbNoNFC)
        btnScanNFC = findViewById(R.id.btnScanNFC)
        btnSave = findViewById(R.id.btnSave)
        tvNfcStatus = findViewById(R.id.tvNfcStatus)

        // Auto-fill email
        etEmail.setText(auth.currentUser?.email ?: "")
        etEmail.isEnabled = false

        // Load existing data if available
        loadExistingData()

        btnSelectImage.setOnClickListener {
            openGallery()
        }

        btnRemoveImage.setOnClickListener {
            removeProfileImage()
        }

        cbNoNFC.setOnCheckedChangeListener { _, isChecked ->
            btnScanNFC.isEnabled = !isChecked
            if (isChecked) {
                nfcCardId = "MANUAL_${auth.currentUser?.uid}"
                tvNfcStatus.text = "✓ Manual mode selected (No NFC card)"
            } else {
                if (nfcCardId?.startsWith("MANUAL_") == true) {
                    nfcCardId = null
                    tvNfcStatus.text = "Tap 'Scan NFC Card' button"
                }
            }
        }

        btnScanNFC.setOnClickListener {
            if (nfcAdapter == null) {
                Toast.makeText(this, "NFC not supported", Toast.LENGTH_SHORT).show()
            } else if (!nfcAdapter!!.isEnabled) {
                Toast.makeText(this, "Please enable NFC", Toast.LENGTH_SHORT).show()
            } else {
                tvNfcStatus.text = "📡 Ready to scan - Hold card near device"
                Toast.makeText(this, "Hold your NFC card near the device", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            saveCardData()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun removeProfileImage() {
        profileImageBase64 = null
        ivProfilePicture.setImageResource(android.R.drawable.ic_menu_gallery)
        btnRemoveImage.visibility = android.view.View.GONE
        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                try {
                    // Load and compress the image
                    val bitmap = loadAndCompressImage(imageUri)

                    // Display the image
                    ivProfilePicture.setImageBitmap(bitmap)
                    btnRemoveImage.visibility = android.view.View.VISIBLE

                    // Convert to Base64
                    profileImageBase64 = bitmapToBase64(bitmap)

                    Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadAndCompressImage(uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // Calculate scaling factor
        val scale = calculateScale(originalBitmap.width, originalBitmap.height)

        // Resize bitmap
        val scaledWidth = (originalBitmap.width * scale).toInt()
        val scaledHeight = (originalBitmap.height * scale).toInt()

        return Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
    }

    private fun calculateScale(width: Int, height: Int): Float {
        val maxDimension = maxOf(width, height)
        return if (maxDimension > MAX_IMAGE_SIZE) {
            MAX_IMAGE_SIZE.toFloat() / maxDimension
        } else {
            1f
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun loadExistingData() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        etFullName.setText(snapshot.child("fullName").value?.toString() ?: "")
                        etProfession.setText(snapshot.child("profession").value?.toString() ?: "")
                        etOrganization.setText(snapshot.child("organization").value?.toString() ?: "")
                        etBio.setText(snapshot.child("bio").value?.toString() ?: "")
                        etPhone.setText(snapshot.child("phone").value?.toString() ?: "")
                        etLinkedIn.setText(snapshot.child("linkedIn").value?.toString() ?: "")
                        etGitHub.setText(snapshot.child("gitHub").value?.toString() ?: "")

                        val savedNfcId = snapshot.child("nfcCardId").value?.toString()
                        if (savedNfcId != null) {
                            nfcCardId = savedNfcId
                            if (savedNfcId.startsWith("MANUAL_")) {
                                cbNoNFC.isChecked = true
                                tvNfcStatus.text = "✓ Manual mode (No NFC card)"
                            } else {
                                tvNfcStatus.text = "✓ NFC Card: $savedNfcId"
                            }
                        }

                        // Load profile picture if exists
                        val profileImageData = snapshot.child("profileImage").value?.toString()
                        if (!profileImageData.isNullOrEmpty()) {
                            try {
                                val bitmap = base64ToBitmap(profileImageData)
                                ivProfilePicture.setImageBitmap(bitmap)
                                profileImageBase64 = profileImageData
                                btnRemoveImage.visibility = android.view.View.VISIBLE
                            } catch (e: Exception) {
                                // If loading fails, just keep default
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddCardActivity, "Error loading data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null && !cbNoNFC.isChecked) {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_MUTABLE
            )
            nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {

            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                handleNfcTag(it)
            }
        }
    }

    private fun handleNfcTag(tag: Tag) {
        val uid = tag.id?.joinToString("") { String.format("%02X", it) }

        if (uid != null) {
            nfcCardId = uid
            tvNfcStatus.text = "✓ NFC Card scanned: $uid"

            try {
                val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            } catch (e: Exception) {
                // Vibration not available
            }

            Toast.makeText(this, "NFC Card scanned successfully: $uid", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to read NFC card UID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCardData() {
        val fullName = etFullName.text.toString().trim()
        val profession = etProfession.text.toString().trim()
        val bio = etBio.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }

        if (profession.isEmpty()) {
            etProfession.error = "Profession is required"
            return
        }

        if (bio.isEmpty()) {
            etBio.error = "Bio is required"
            return
        }

        if (nfcCardId == null && !cbNoNFC.isChecked) {
            Toast.makeText(this, "Please scan NFC card or check 'No NFC Card'", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        if (nfcCardId == null) {
            nfcCardId = "MANUAL_$userId"
        }

        // Show progress
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        val cardData = hashMapOf(
            "fullName" to fullName,
            "profession" to profession,
            "organization" to etOrganization.text.toString().trim(),
            "bio" to bio,
            "email" to etEmail.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "linkedIn" to etLinkedIn.text.toString().trim(),
            "gitHub" to etGitHub.text.toString().trim(),
            "nfcCardId" to nfcCardId,
            "userId" to userId
        )

        // Add profile image if exists
        if (!profileImageBase64.isNullOrEmpty()) {
            cardData["profileImage"] = profileImageBase64!!
        }

        database.reference.child("users").child(userId).setValue(cardData)
            .addOnSuccessListener {
                if (nfcCardId != null && !nfcCardId!!.startsWith("MANUAL_")) {
                    database.reference.child("nfcCards").child(nfcCardId!!).setValue(userId)
                        .addOnSuccessListener {
                            btnSave.isEnabled = true
                            btnSave.text = "Save Card"
                            Toast.makeText(this, "Card saved successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnSave.isEnabled = true
                            btnSave.text = "Save Card"
                            Toast.makeText(this, "Error mapping NFC card: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    btnSave.isEnabled = true
                    btnSave.text = "Save Card"
                    Toast.makeText(this, "Card saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                btnSave.text = "Save Card"
                Toast.makeText(this, "Error saving card: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}