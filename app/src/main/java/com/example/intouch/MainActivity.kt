package com.example.intouch

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.SignInButton
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: TextView
    private lateinit var forgetPassword: TextView
    private lateinit var googleSignInButton: SignInButton
    private lateinit var credentialManager: CredentialManager

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, LandingPageActivity::class.java))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        // Credential Manager
        credentialManager = CredentialManager.create(this)

        // UI Elements
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.btn_login)
        registerButton = findViewById(R.id.register)
        forgetPassword = findViewById(R.id.forgetPassword)
        googleSignInButton = findViewById(R.id.btn_google_signin)

        // Email/Password Login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        forgetPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPassword::class.java))
        }

        // Google Sign-In Click
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

    }

    // Email/Password Login
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LandingPageActivity::class.java))
                } else {
                    val errorMessage = task.exception?.message ?: "An unknown error occurred."
                    Toast.makeText(this, "Login failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Google Sign-In (New Credential Manager Flow)
    private fun signInWithGoogle() {
        val googleOption = GetSignInWithGoogleOption.Builder(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential(this@MainActivity, request)

                // result.credential is a Bundle
                val credentialData = result.credential
                val googleCredential = GoogleIdTokenCredential.createFrom(credentialData.data)
                val idToken = googleCredential.idToken

                if (idToken.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Google ID token not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                firebaseAuthWithGoogle(idToken)

            } catch (e: GetCredentialException) {
                val errorMessage = e.message ?: "An unknown error occurred."
                Toast.makeText(this@MainActivity, "Google sign in failed: $errorMessage", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unknown error occurred."
                Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LandingPageActivity::class.java))
            } else {
                val errorMessage = task.exception?.message ?: "An unknown error occurred."
                Toast.makeText(this, "Authentication Failed: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        }
    }
}