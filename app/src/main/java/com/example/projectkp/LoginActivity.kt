package com.example.projectkp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val name = account.displayName
                    val email = account.email

                    Log.d("Google Sign-In", "Login Berhasil\nNama: $name\nEmail: $email")

                    // TAMBAHAN: Simpan data pengguna ke SharedPreferences
                    val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("user_name", name)
                    editor.putString("user_email", email)
                    editor.apply() // Menyimpan perubahan

                    // Intent ke DaftarSuratActivity saat login berhasil
                    val intent = Intent(this, DaftarSuratActivity::class.java)
                    // Opsional: kirim data pengguna ke activity berikutnya
                    intent.putExtra("user_name", name)
                    intent.putExtra("user_email", email)
                    startActivity(intent)
                    finish() // Tutup activity login
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val statusMessage = CommonStatusCodes.getStatusCodeString(statusCode)
                Log.e("Google Sign-In", "Sign-in failed with code: $statusCode ($statusMessage)")
                Log.e("Google Sign-In", "Status: ${e.status}")

                // Tampilkan pesan error ke pengguna
                Toast.makeText(this, "Login gagal: $statusCode - $statusMessage", Toast.LENGTH_SHORT).show()

                // Log tambahan untuk debugging
                if (e.status.hasResolution()) {
                    Log.e("Google Sign-In", "Error memiliki resolusi yang bisa dijalankan")
                }
            }
        }
    }
}