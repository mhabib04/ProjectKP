package com.example.projectkp

import android.content.Intent
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        checkLoginStatus()
        setupViews()
    }

    private fun checkLoginStatus() {
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
    }

    private fun setupViews() {
        setupRecyclerView()
        setupSwipeRefresh()
        setupButtons()
        loadSuratList()
    }

    private fun setupButtons() {
        // Tombol tambah surat
        binding.fabTambahSurat.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Tombol navigasi
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
            MaterialAlertDialogBuilder(this)
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin logout?")
                .setPositiveButton("Logout") { _, _ ->
                    googleSignInClient.signOut().addOnCompleteListener(this) {
                        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            currentPage = 0 // Reset ke halaman pertama saat refresh
            loadSuratList()
        }

        // Sesuaikan warna indikator refresh
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
        // Tampilkan loading jika tidak sedang refresh
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

    // Fungsi untuk cek apakah tanggal sudah expired
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
        // Hitung indeks awal dan akhir untuk halaman saat ini
        val startIndex = currentPage * itemsPerPage
        val endIndex = minOf((currentPage + 1) * itemsPerPage, allSuratList.size)
        val currentPageItems = allSuratList.subList(startIndex, endIndex)

        // Update adapter dengan data halaman saat ini
        adapter = SuratAdapter(currentPageItems) { suratIdStr ->
            openPdfViewer(suratIdStr)
        }
        binding.recyclerView.adapter = adapter

        // Update tombol navigasi dan informasi halaman
        updateNavigationButtons()
        val totalPages = (allSuratList.size + itemsPerPage - 1) / itemsPerPage
        binding.tvPageInfo.text = "Halaman ${currentPage + 1} dari $totalPages"
    }

    private fun updateNavigationButtons() {
        binding.btnPrevious.visibility = if (currentPage > 0) View.VISIBLE else View.INVISIBLE
        binding.btnNext.visibility = if ((currentPage + 1) * itemsPerPage < allSuratList.size) View.VISIBLE else View.INVISIBLE
    }

    private fun openPdfViewer(suratIdStr: String) {
        try {
            val suratId = suratIdStr.toInt()
            val baseUrl = "https://2be5-36-69-1-149.ngrok-free.app"
            val pdfUrl = "$baseUrl/surats/$suratId/pdf"

            Log.d("PDF_VIEWER", "PDF URL: $pdfUrl")
            // Buat intent baru ke activity PDF Viewer
            val intent = Intent(this, PdfViewerActivity::class.java)
            intent.putExtra("PDF_URL", pdfUrl)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("PDF_VIEWER", "Error opening PDF: ${e.message}", e)
        }
    }
}