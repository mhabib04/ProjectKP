package com.example.projectkp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectkp.api.RetrofitClient
import com.example.projectkp.databinding.ActivityDaftarSuratBinding
import com.example.projectkp.model.adapter.SuratAdapter
import com.example.projectkp.model.surat.Surat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DaftarSuratActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaftarSuratBinding
    private lateinit var adapter: SuratAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarSuratBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadSuratList()

        binding.fabTambahSurat.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SuratAdapter(emptyList()) { pdfUrl ->
            openPdfViewer(pdfUrl)
        }
        binding.recyclerView.adapter = adapter
    }

    private fun loadSuratList() {
        binding.progressBar.visibility = View.VISIBLE
        RetrofitClient.instance.getSuratList()
            .enqueue(object : Callback<List<Surat>> {
                override fun onResponse(call: Call<List<Surat>>, response: Response<List<Surat>>) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        adapter = SuratAdapter(response.body() ?: emptyList()) { pdfUrl ->
                            openPdfViewer(pdfUrl)
                        }
                        binding.recyclerView.adapter = adapter
                    } else {
                        Toast.makeText(this@DaftarSuratActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Surat>>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@DaftarSuratActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun openPdfViewer(pdfUrl: String) {
        // You can either open in browser or use a PDF viewer library
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(pdfUrl)
        startActivity(intent)
    }
}