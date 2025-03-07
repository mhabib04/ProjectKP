package com.example.projectkp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.projectkp.api.RetrofitClient
import com.example.projectkp.databinding.ActivityMainBinding
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PICK_PDF_REQUEST = 1
    private var pdfUri: Uri? = null
    private var selectedStartDate: Calendar? = null
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil data email dari intent atau SharedPreferences
        userEmail = intent.getStringExtra("user_email") ?:
                getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_email", "") ?: ""

        Log.d("MainActivity", "Email user: $userEmail")

        setupListeners()

        binding.btnSimpan.setOnClickListener {
            if (binding.etJudul.text?.isEmpty() == true) {
                Toast.makeText(this, "Judul harus diisi!", Toast.LENGTH_SHORT).show()
            } else if (binding.etTujuan.text?.isEmpty() == true) {
                Toast.makeText(this, "Deskripsi harus diisi!", Toast.LENGTH_SHORT).show()
            } else if (binding.etTanggalMulai.text?.isEmpty() == true) {
                Toast.makeText(this, "Tanggal mulai harus diisi!", Toast.LENGTH_SHORT).show()
            } else if (binding.etTanggalBerakhir.text?.isEmpty() == true) {
                Toast.makeText(this, "Tanggal berakhir harus diisi!", Toast.LENGTH_SHORT).show()
            } else {
                saveData()
            }
        }
    }

    private fun setupListeners() {
        // Tombol kembali
        binding.btnKembali.setOnClickListener {
            startActivity(Intent(this, DaftarSuratActivity::class.java))
        }

        // Set event untuk pemilihan tanggal
        binding.etTanggalMulai.setOnClickListener { showDatePicker(isStartDate = true) }
        binding.etTanggalBerakhir.setOnClickListener { showDatePicker(isStartDate = false) }

        // Set event untuk memilih file
        binding.btnPilihFile.setOnClickListener { openFileChooser() }

        // Tombol simpan
        binding.btnSimpan.setOnClickListener {
            if (validateInputs()) {
                saveData()
            }
        }
    }

    private fun validateInputs(): Boolean {
        when {
            binding.etJudul.text?.isEmpty() == true -> {
                Toast.makeText(this, "Judul harus diisi!", Toast.LENGTH_SHORT).show()
                return false
            }
            binding.etTujuan.text?.isEmpty() == true -> {
                Toast.makeText(this, "Tujuan harus diisi!", Toast.LENGTH_SHORT).show()
                return false
            }
            binding.etTanggalMulai.text?.isEmpty() == true -> {
                Toast.makeText(this, "Tanggal mulai harus diisi!", Toast.LENGTH_SHORT).show()
                return false
            }
            binding.etTanggalBerakhir.text?.isEmpty() == true -> {
                Toast.makeText(this, "Tanggal berakhir harus diisi!", Toast.LENGTH_SHORT).show()
                return false
            }
            pdfUri == null -> {
                Toast.makeText(this, "Silakan pilih file PDF terlebih dahulu!", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()

        // Jika memilih tanggal berakhir, set batas minimum dari tanggal mulai yang sudah dipilih
        if (!isStartDate && selectedStartDate != null) {
            calendar.time = selectedStartDate!!.time
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Format yang diharapkan API

                if (isStartDate) {
                    binding.etTanggalMulai.setText(format.format(selectedDate.time))
                    selectedStartDate = selectedDate
                    binding.etTanggalBerakhir.text?.clear() // Reset tanggal berakhir jika tanggal mulai berubah
                } else {
                    binding.etTanggalBerakhir.setText(format.format(selectedDate.time))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Jika memilih tanggal berakhir, set batas minimalnya dari tanggal mulai
        if (!isStartDate && selectedStartDate != null) {
            datePicker.datePicker.minDate = selectedStartDate!!.timeInMillis
        }

        datePicker.show()
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Pilih File PDF"), PICK_PDF_REQUEST)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PDF_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            pdfUri = data.data
            pdfUri?.let { uri ->
                updateFileInfo(uri)
            }
        }
    }

    private fun updateFileInfo(uri: Uri) {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                val fileName = cursor.getString(nameIndex)
                val fileSize = cursor.getLong(sizeIndex)

                // Format file size
                val formattedSize = when {
                    fileSize < 1024 -> "$fileSize B"
                    fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
                    else -> String.format("%.1f MB", fileSize / (1024 * 1024f))
                }

                binding.fileInfoContainer.visibility = View.VISIBLE
                binding.tvFileName.text = fileName
                binding.tvFileSize.text = formattedSize

                // Tambahkan click listener untuk tombol hapus
                binding.btnClearFile.setOnClickListener {
                    clearSelectedFile()
                }
            }
        }
    }

    private fun clearSelectedFile() {
        pdfUri = null
        binding.fileInfoContainer.visibility = View.GONE
        binding.btnPilihFile.text = "Pilih File PDF"
    }

    private fun getFileName(uri: Uri): String {
        var name = "file.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun saveData() {
        if (pdfUri == null) {
            Toast.makeText(this, "Silakan pilih file PDF terlebih dahulu!", Toast.LENGTH_SHORT).show()
            return
        }

        // Tampilkan loading dan disable tombol
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Mengirim..."

        // Persiapkan data untuk dikirim
        val judul = RequestBody.create(MultipartBody.FORM, binding.etJudul.text.toString())
        val tujuan = RequestBody.create(MultipartBody.FORM, binding.etTujuan.text.toString())
        val tanggalMulai = RequestBody.create(MultipartBody.FORM, binding.etTanggalMulai.text.toString())
        val tanggalBerakhir = RequestBody.create(MultipartBody.FORM, binding.etTanggalBerakhir.text.toString())

        val inputStream = contentResolver.openInputStream(pdfUri!!)
        val fileBytes = inputStream!!.readBytes()
        val file = RequestBody.create(MultipartBody.FORM, fileBytes)
        val filePart = MultipartBody.Part.createFormData("file_pdf", getFileName(pdfUri!!), file)

        // Kirim data ke server
        RetrofitClient.instance.insertSurat(judul, tujuan, tanggalMulai, tanggalBerakhir, filePart)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    // Sembunyikan loading dan enable tombol kembali
                    binding.progressBar.visibility = View.INVISIBLE
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "Simpan"

                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Data berhasil dikirim!", Toast.LENGTH_SHORT).show()

                        // Tambahkan ke Google Calendar
                        addEventToCalendar(
                            binding.etJudul.text.toString(),
                            binding.etTanggalMulai.text.toString(),
                            binding.etTanggalBerakhir.text.toString()
                        )

                        // Pindah ke halaman daftar surat
                        startActivity(Intent(this@MainActivity, DaftarSuratActivity::class.java))
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(applicationContext, "Gagal mengirim data: $errorBody", Toast.LENGTH_LONG).show()
                        Log.e("API_ERROR", "Error code: ${response.code()}, message: $errorBody")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Sembunyikan loading dan enable tombol kembali
                    binding.progressBar.visibility = View.INVISIBLE
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "Simpan"

                    Toast.makeText(applicationContext, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("API_ERROR", "Error: ${t.message}")
                }
            })
    }

    private fun addEventToCalendar(title: String, startDate: String, endDate: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Parse tanggal
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDateTime = formatter.parse(startDate)
                val endDateTime = formatter.parse(endDate)

                if (startDateTime == null || endDateTime == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "Format tanggal tidak valid", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Tambahkan 1 hari ke tanggal akhir untuk event sepanjang hari
                val endDateTimePlusDay = Calendar.getInstance().apply {
                    time = endDateTime
                    add(Calendar.DAY_OF_MONTH, 1)
                }.time

                // Siapkan kredensial
                val credential = getCalendarCredential()

                // Buat layanan calendar
                val transport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()

                val calendar = com.google.api.services.calendar.Calendar.Builder(
                    transport,
                    jsonFactory,
                    credential
                )
                    .setApplicationName("ProjectKP")
                    .build()

                // Buat event
                val event = Event()
                    .setSummary(title)
                    .setDescription("Surat terkait: $title")
                    .setLocation("PTPN IV")

                // Set tanggal mulai
                val start = EventDateTime()
                    .setDate(DateTime(true, startDateTime.time, 0))
                    .setTimeZone("Asia/Jakarta")
                event.setStart(start)

                // Set tanggal selesai
                val end = EventDateTime()
                    .setDate(DateTime(true, endDateTimePlusDay.time, 0))
                    .setTimeZone("Asia/Jakarta")
                event.setEnd(end)

                // Masukkan event ke Google Calendar
                val calendarId = "primary"
                val insertedEvent = calendar.events().insert(calendarId, event).execute()

                withContext(Dispatchers.Main) {
                    if (insertedEvent != null && insertedEvent.htmlLink != null) {
                        Toast.makeText(
                            applicationContext,
                            "Event berhasil ditambahkan ke Google Calendar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CALENDAR_ERROR", "Error menambahkan event: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        "Gagal menambahkan event: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getCalendarCredential(): GoogleAccountCredential {
        // Menggunakan GoogleAccountCredential dengan scope CalendarScopes.CALENDAR
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            listOf(CalendarScopes.CALENDAR)
        )

        // Mengambil email dari SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)

        if (userEmail.isNullOrEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            throw IllegalStateException("Email pengguna tidak ditemukan")
        }

        credential.selectedAccountName = userEmail
        return credential
    }
}
