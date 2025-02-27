package com.example.projectkp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectkp.api.RetrofitClient
import com.example.projectkp.databinding.ActivityDaftarSuratBinding
import com.example.projectkp.model.adapter.SuratAdapter
import com.example.projectkp.model.surat.Surat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DaftarSuratActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaftarSuratBinding
    private lateinit var adapter: SuratAdapter
    private var allSuratList: List<Surat> = emptyList()
    private var currentPage = 0
    private val itemsPerPage = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarSuratBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSwipeRefresh()
        loadSuratList()

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            // User belum login, arahkan ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // User sudah login, bisa akses data
            Log.d("Google Sign-In", "User masih login: ${account.email}")
        }

//        val packageManager = packageManager
//        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
//            // Tampilkan pesan bahwa aplikasi membutuhkan NFC
//            Toast.makeText(this, "Aplikasi ini membutuhkan Fingerprint", Toast.LENGTH_LONG).show()
//            finish()
//        } else{
//            Toast.makeText(this, "Aplikasi ini tidak membutuhkan Fingerprint", Toast.LENGTH_LONG).show()
//        }

        binding.fabTambahSurat.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.btnNext.setOnClickListener {
            if ((currentPage + 1) * itemsPerPage < allSuratList.size) {
                currentPage++
                updatePageDisplay()
            }
        }

        binding.btnPrevious.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                updatePageDisplay()
            }
        }
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)

        binding.btnLogout.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener(this) {
                Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 0 // Reset to first page when refreshing
            loadSuratList()
        }

        // Customize the refresh indicator colors (optional)
        binding.swipeRefresh.setColorSchemeResources(
            R.color.purple_500,
            R.color.purple_700,
            R.color.teal_200
        )
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SuratAdapter(emptyList()) { pdfUrl ->
            openPdfViewer(pdfUrl)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun loadSuratList() {
        binding.progressBar.visibility = if (!binding.swipeRefresh.isRefreshing) View.VISIBLE else View.GONE

        RetrofitClient.instance.getSuratList()
            .enqueue(object : Callback<List<Surat>> {
                override fun onResponse(call: Call<List<Surat>>, response: Response<List<Surat>>) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    if (response.isSuccessful) {
                        val suratList = response.body() ?: emptyList()

                        // Pisahkan yang expired dan belum expired
                        val suratBelumExpired = suratList.filterNot { isExpired(it.tanggal_berakhir) }
                        val suratExpired = suratList.filter { isExpired(it.tanggal_berakhir) }

                        // Gabungkan: yang belum expired di atas, expired di bawah
                        allSuratList = suratBelumExpired + suratExpired

                        updatePageDisplay()
                        updateNavigationButtons()
                    } else {
                        Toast.makeText(this@DaftarSuratActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Surat>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@DaftarSuratActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Fungsi cek expired hanya di Activity
    private fun isExpired(tanggalBerakhir: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = sdf.parse(tanggalBerakhir)
            val today = Date()
            expiryDate != null && expiryDate.before(today)
        } catch (e: Exception) {
            false
        }
    }


    private fun updatePageDisplay() {
        val startIndex = currentPage * itemsPerPage
        val endIndex = minOf((currentPage + 1) * itemsPerPage, allSuratList.size)
        val currentPageItems = allSuratList.subList(startIndex, endIndex)

        adapter = SuratAdapter(currentPageItems) { pdfUrl ->
            openPdfViewer(pdfUrl)
        }
        binding.recyclerView.adapter = adapter

        updateNavigationButtons()
        binding.tvPageInfo.text = "Halaman ${currentPage + 1} dari ${(allSuratList.size + itemsPerPage - 1) / itemsPerPage}"
    }

    private fun updateNavigationButtons() {
        binding.btnPrevious.visibility = if (currentPage > 0) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if ((currentPage + 1) * itemsPerPage < allSuratList.size) View.VISIBLE else View.INVISIBLE
    }

    private fun openPdfViewer(pdfUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(pdfUrl)
        startActivity(intent)
    }

}