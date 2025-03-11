package com.example.projectkp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projectkp.databinding.ActivityPdfViewerBinding
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class PdfViewerActivity : AppCompatActivity(), OnLoadCompleteListener, OnErrorListener, OnPageChangeListener {

    private lateinit var binding: ActivityPdfViewerBinding
    private val executor = Executors.newSingleThreadExecutor()
    private var currentPage = 0
    private var totalPages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pdfUrl = intent.getStringExtra("PDF_URL") ?: ""
        if (pdfUrl.isEmpty()) {
            Toast.makeText(this, "URL PDF tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PdfViewer", "Loading PDF from URL: $pdfUrl")
        setupToolbar()
        loadPdf(pdfUrl)
    }

    private fun setupToolbar() {
        binding.toolbarTitle.text = "Lihat PDF"
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadPdf(pdfUrl: String) {
        binding.progressBar.visibility = View.VISIBLE

        // Gunakan executor untuk operasi jaringan
        executor.execute {
            try {
                Log.d("PdfViewer", "Downloading PDF...")

                // Gunakan HttpURLConnection standard dari Java
                val url = URL(pdfUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000 // 30 detik
                connection.readTimeout = 30000 // 30 detik
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    showError("Server mengembalikan kode: ${connection.responseCode}")
                    return@execute
                }

                // Buat file temporary untuk menyimpan PDF
                val tempFile = File(cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")

                // Tulis respons ke file
                val inputStream = BufferedInputStream(connection.inputStream)
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0L
                val contentLength = connection.contentLength.toLong()

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Update progress jika content length diketahui
                    if (contentLength > 0) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        Log.d("PdfViewer", "Download progress: $progress%")
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d("PdfViewer", "PDF downloaded to: ${tempFile.absolutePath}")

                // Tampilkan PDF di UI thread
                runOnUiThread {
                    displayPdf(tempFile)
                }
            } catch (e: IOException) {
                showError("Error jaringan: ${e.message}")
                Log.e("PdfViewer", "Network error", e)
            } catch (e: Exception) {
                showError("Error: ${e.message}")
                Log.e("PdfViewer", "Error loading PDF", e)
            }
        }
    }

    private fun displayPdf(file: File) {
        try {
            Log.d("PdfViewer", "Displaying PDF...")
            binding.pdfView.fromFile(file)
                .onLoad(this)
                .onError(this)
                .onPageChange(this)
                .enableSwipe(true)
                .enableDoubletap(true)
                .defaultPage(0)
                .enableAnnotationRendering(true)
                .password(null)
                .load()
        } catch (e: Exception) {
            showError("Error menampilkan PDF: ${e.message}")
            Log.e("PdfViewer", "Error displaying PDF", e)
        }
    }

    override fun loadComplete(nbPages: Int) {
        totalPages = nbPages
        binding.progressBar.visibility = View.GONE
//        Toast.makeText(this, "PDF berhasil dimuat", Toast.LENGTH_SHORT).show()
        Log.d("PdfViewer", "PDF loaded successfully. Total pages: $nbPages")
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        currentPage = page
        Log.d("PdfViewer", "Page changed to: $page/$pageCount")
    }

    override fun onError(t: Throwable?) {
        binding.progressBar.visibility = View.GONE
        showError("Gagal menampilkan PDF: ${t?.message}")
        Log.e("PdfViewer", "Error in PDF viewer", t)
    }

    private fun showError(message: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            Log.e("PdfViewer", message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}