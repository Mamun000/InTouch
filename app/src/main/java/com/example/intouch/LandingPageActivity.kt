package com.example.intouch

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.google.zxing.qrcode.QRCodeWriter

class LandingPageActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var cardAddNewCard: CardView
    private lateinit var cardScanHistory: CardView
    private lateinit var cardSettings: CardView
    private lateinit var tvWelcome: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        initializeViews()
        setupToolbar()
        setupNavigationDrawer()
        setupBottomNavigation()
        loadUserData()
        setupCardClicks()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        cardAddNewCard = findViewById(R.id.cardAddNewCard)
        cardScanHistory = findViewById(R.id.cardScanHistory)
        cardSettings = findViewById(R.id.cardSettings)
        tvWelcome = findViewById(R.id.tvWelcome)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Digital Card Exchange"
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        // Load user data into navigation header
        loadNavigationHeader()
    }

    private fun loadNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val ivHeaderProfile = headerView.findViewById<ImageView>(R.id.ivHeaderProfile)
        val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
        val tvHeaderEmail = headerView.findViewById<TextView>(R.id.tvHeaderEmail)
        val ivHeaderQR = headerView.findViewById<ImageView>(R.id.ivHeaderQR)

        val userId = auth.currentUser?.uid
        tvHeaderEmail.text = auth.currentUser?.email

        if (userId != null) {
            database.reference.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val fullName = snapshot.child("fullName").value?.toString()
                            tvHeaderName.text = fullName ?: "User"

                            // Load profile image
                            val profileImageData = snapshot.child("profileImage").value?.toString()
                            if (!profileImageData.isNullOrEmpty()) {
                                try {
                                    val bitmap = base64ToBitmap(profileImageData)
                                    ivHeaderProfile.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    ivHeaderProfile.setImageResource(R.drawable.ic_profile_placeholder)
                                }
                            }

                            // Generate QR code
                            generateQRCode(userId, ivHeaderQR)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan_nfc -> {
                    scanNFC()
                    true
                }
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_scan_qr -> {
                    scanQRCode()
                    true
                }
                else -> false
            }
        }
        // Set home as selected
        bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun setupCardClicks() {
        cardAddNewCard.setOnClickListener {
            startActivity(Intent(this, AddCardActivity::class.java))
        }

        cardScanHistory.setOnClickListener {
            Toast.makeText(this, "Scan History - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        cardSettings.setOnClickListener {
            Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.reference.child("users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val fullName = snapshot.child("fullName").value?.toString()
                            tvWelcome.text = "Welcome, ${fullName ?: "User"}!"
                        } else {
                            tvWelcome.text = "Welcome! Please add your card"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun scanNFC() {
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported on this device", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Please enable NFC in settings", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Hold your device near an NFC card", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scanQRCode() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan QR Code")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun generateQRCode(userId: String, imageView: ImageView) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(userId, BarcodeFormat.QR_CODE, 300, 300)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            // QR generation failed
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val intent = Intent(this, ViewCardActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("SHOW_QR", true)
                    startActivity(intent)
                }
            }
            R.id.nav_edit_card -> {
                startActivity(Intent(this, AddCardActivity::class.java))
            }
            R.id.nav_scan_history -> {
                Toast.makeText(this, "Scan History - Coming Soon", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Settings - Coming Soon", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadNavigationHeader()

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
        val uid = tag.id?.joinToString("") { String.format("%02X", it) }

        if (uid != null) {
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

            Toast.makeText(this, "NFC Card Scanned", Toast.LENGTH_SHORT).show()
            lookupUserByNfcUid(uid)
        } else {
            Toast.makeText(this, "Failed to read NFC card UID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun lookupUserByNfcUid(nfcUid: String) {
        database.reference.child("nfcCards").child(nfcUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userId = snapshot.value.toString()
                        val intent = Intent(this@LandingPageActivity, ViewCardActivity::class.java)
                        intent.putExtra("USER_ID", userId)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@LandingPageActivity,
                            "This NFC card is not registered",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@LandingPageActivity,
                        "Error: ${error.message}",
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}