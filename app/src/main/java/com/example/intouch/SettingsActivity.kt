package com.example.intouch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var toolbar: Toolbar
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchVibration: SwitchCompat
    private lateinit var switchSound: SwitchCompat
    private lateinit var layoutClearHistory: LinearLayout
    private lateinit var layoutAbout: LinearLayout
    private lateinit var layoutPrivacy: LinearLayout
    private lateinit var layoutHelp: LinearLayout
    private lateinit var tvAppVersion: TextView

    companion object {
        private const val PREFS_NAME = "AppPreferences"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_SOUND = "sound"

        fun isDarkModeEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_DARK_MODE, false)
        }

        fun isVibrationEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_VIBRATION, true)
        }

        fun isSoundEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_SOUND, true)
        }

        fun areNotificationsEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_NOTIFICATIONS, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before calling super.onCreate()
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applyTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()

        initializeViews()
        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchVibration = findViewById(R.id.switchVibration)
        switchSound = findViewById(R.id.switchSound)
        layoutClearHistory = findViewById(R.id.layoutClearHistory)
        layoutAbout = findViewById(R.id.layoutAbout)
        layoutPrivacy = findViewById(R.id.layoutPrivacy)
        layoutHelp = findViewById(R.id.layoutHelp)
        tvAppVersion = findViewById(R.id.tvAppVersion)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Settings"
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        switchDarkMode.isChecked = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        switchNotifications.isChecked = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true)
        switchVibration.isChecked = sharedPreferences.getBoolean(KEY_VIBRATION, true)
        switchSound.isChecked = sharedPreferences.getBoolean(KEY_SOUND, true)

        // Set app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            tvAppVersion.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            tvAppVersion.text = "Version 1.0.0"
        }
    }

    private fun setupListeners() {
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_DARK_MODE, isChecked)
            applyTheme()
            recreate()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_NOTIFICATIONS, isChecked)
        }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_VIBRATION, isChecked)
        }

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            saveSetting(KEY_SOUND, isChecked)
        }

        layoutClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }

        layoutAbout.setOnClickListener {
            showAboutDialog()
        }

        layoutPrivacy.setOnClickListener {
            showPrivacyDialog()
        }

        layoutHelp.setOnClickListener {
            showHelpDialog()
        }
    }

    private fun saveSetting(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    private fun applyTheme() {
        val isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun showClearHistoryDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Clear Scan History")
            .setMessage("Are you sure you want to clear all scan history? This action cannot be undone.")
            .setPositiveButton("Clear") { dialog, _ ->
                clearScanHistory()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearScanHistory() {
        val userId = auth.currentUser?.uid ?: return
        val database = com.google.firebase.database.FirebaseDatabase.getInstance()

        database.reference.child("scanHistory").child(userId).removeValue()
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Scan history cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, "Failed to clear history", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("About NFC Card Exchange")
            .setMessage("""
                NFC Card Exchange
                Version 1.0.0
                
                A modern digital business card exchange application using NFC technology.
                
                Features:
                • NFC card scanning
                • QR code generation
                • Digital card management
                • Scan history & contacts
                • Profile customization
                
                Developed with ❤️ for seamless networking.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPrivacyDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage("""
                Your privacy is important to us.
                
                Data Collection:
                • Profile information (name, email, profession)
                • NFC card data
                • Scan history
                
                Data Usage:
                • All data is stored securely in Firebase
                • Data is only shared when you scan cards
                • You can delete your data anytime
                
                Security:
                • End-to-end encryption
                • Secure Firebase authentication
                • No data sold to third parties
                
                For full privacy policy, visit our website.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showHelpDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("""
                Need help? Here are some quick tips:
                
                • To add a card: Scan your NFC card and fill in details
                • To scan others: Use NFC or QR scanner
                • To share: Show your QR code from navigation drawer
                
                Common Issues:
                • NFC not working? Enable NFC in phone settings
                • Card already registered? Each card is unique to one user
                
                Contact Support:
                Email: support@nfccardexchange.com
                
                FAQ and tutorials available on our website.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
}