package com.example.intouch

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddCardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var nfcAdapter: NfcAdapter? = null
    private var nfcCardId: String? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_card)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

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
        // Read the UID from the tag (works with non-NDEF formatted tags)
        val uid = tag.id?.joinToString("") { String.format("%02X", it) }

        if (uid != null) {
            nfcCardId = uid
            tvNfcStatus.text = "✓ NFC Card scanned: $uid"

            // Provide haptic feedback
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

        database.reference.child("users").child(userId).setValue(cardData)
            .addOnSuccessListener {
                // Also map NFC card ID to user ID for lookup
                if (nfcCardId != null && !nfcCardId!!.startsWith("MANUAL_")) {
                    database.reference.child("nfcCards").child(nfcCardId!!).setValue(userId)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Card saved successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error mapping NFC card: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Card saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving card: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}