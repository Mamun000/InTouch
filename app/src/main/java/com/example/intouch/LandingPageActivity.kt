package com.example.intouch


import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult

class LandingPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var btnAddCard: Button
    private lateinit var btnScanNFC: Button
    private lateinit var btnScanQR: Button
    private lateinit var btnShowMyQR: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        btnAddCard = findViewById(R.id.btnAddCard)
        btnScanNFC = findViewById(R.id.btnScanNFC)
        btnScanQR = findViewById(R.id.btnScanQR)
        btnShowMyQR = findViewById(R.id.btnShowMyQR)
        btnLogout = findViewById(R.id.btnLogout)

        btnAddCard.setOnClickListener {
            startActivity(Intent(this, AddCardActivity::class.java))
        }

        btnScanNFC.setOnClickListener {
            if (nfcAdapter == null) {
                Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_SHORT).show()
            } else if (!nfcAdapter!!.isEnabled) {
                Toast.makeText(this, "Please enable NFC in settings", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ready to scan NFC card - Hold device near card", Toast.LENGTH_SHORT).show()
            }
        }

        btnScanQR.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan QR Code")
            integrator.setCameraId(0)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(false)
            integrator.initiateScan()
        }

        btnShowMyQR.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val intent = Intent(this, ViewCardActivity::class.java)
                intent.putExtra("USER_ID", userId)
                intent.putExtra("SHOW_QR", true)
                startActivity(intent)
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter != null) {
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
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {

            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                readNfcTag(it)
            }
        }
    }

    private fun readNfcTag(tag: Tag) {
        // Read the UID from the tag (works with non-NDEF formatted tags)
        val uid = tag.id?.joinToString("") { String.format("%02X", it) }

        if (uid != null) {
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

            Toast.makeText(this, "NFC Card UID: $uid", Toast.LENGTH_SHORT).show()

            // Look up user by NFC card UID
            lookupUserByNfcUid(uid)
        } else {
            Toast.makeText(this, "Failed to read NFC card UID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun lookupUserByNfcUid(nfcUid: String) {
        // First, try to find the user ID mapped to this NFC card UID
        database.reference.child("nfcCards").child(nfcUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userId = snapshot.value.toString()
                        // User found, navigate to ViewCard
                        val intent = Intent(this@LandingPageActivity, ViewCardActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        startActivity(intent)
                    } else {
                        // NFC card not registered
                        Toast.makeText(
                            this@LandingPageActivity,
                            "This NFC card is not registered. Please add card first.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@LandingPageActivity,
                        "Error looking up card: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            } else {
                val userId = result.contents
                val intent = Intent(this, ViewCardActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}