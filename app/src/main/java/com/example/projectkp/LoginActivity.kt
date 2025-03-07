package com.example.projectkp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectkp.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001 // Request Code Sign-In

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupClickListeners()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            handleGoogleSignInResult(data)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            account?.let {
                val name = it.displayName
                val email = it.email

                Log.d("Google Sign-In", "Login Berhasil\nNama: $name\nEmail: $email")

                saveUserData(name, email)
                navigateToDaftarSurat(name, email)
            }
        } catch (e: ApiException) {
            handleSignInError(e)
        }
    }

    private fun saveUserData(name: String?, email: String?) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            apply()
        }
    }

    private fun navigateToDaftarSurat(name: String?, email: String?) {
        val intent = Intent(this, DaftarSuratActivity::class.java).apply {
            putExtra("user_name", name)
            putExtra("user_email", email)
        }
        startActivity(intent)
        finish()
    }

    private fun handleSignInError(e: ApiException) {
        val statusCode = e.statusCode
        val statusMessage = CommonStatusCodes.getStatusCodeString(statusCode)

        Log.e("Google Sign-In", "Sign-in failed with code: $statusCode ($statusMessage)")
        Toast.makeText(this, "Login gagal: $statusCode - $statusMessage", Toast.LENGTH_SHORT).show()
    }
}