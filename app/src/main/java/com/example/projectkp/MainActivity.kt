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

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Ambil data dari intent atau SharedPreferences
        userEmail = intent.getStringExtra("user_email") ?:
                getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("user_email", "") ?: ""

        // Log untuk validasi
        Log.d("MainActivity", "Email user: $userEmail")

        binding.btnKembali.setOnClickListener {
            val intent = Intent(this, DaftarSuratActivity::class.java)
            startActivity(intent)
        }

        // Set event untuk pemilihan tanggal
        binding.etTanggalMulai.setOnClickListener { showDatePicker(isStartDate = true) }
        binding.etTanggalBerakhir.setOnClickListener { showDatePicker(isStartDate = false) }

        // Set event untuk memilih file
        binding.btnPilihFile.setOnClickListener { openFileChooser() }

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
        val contentResolver = contentResolver

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                cursor.moveToFirst()

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
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file information", Toast.LENGTH_SHORT).show()
            binding.fileInfoContainer.visibility = View.GONE
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
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
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

        val judul = RequestBody.create(MultipartBody.FORM, binding.etJudul.text.toString())
        val tujuan = RequestBody.create(MultipartBody.FORM, binding.etTujuan.text.toString())
        val tanggalMulai = RequestBody.create(MultipartBody.FORM, binding.etTanggalMulai.text.toString())
        val tanggalBerakhir = RequestBody.create(MultipartBody.FORM, binding.etTanggalBerakhir.text.toString())

        val inputStream = contentResolver.openInputStream(pdfUri!!)
        val file = RequestBody.create(MultipartBody.FORM, inputStream!!.readBytes())
        val filePart = MultipartBody.Part.createFormData("file_pdf", getFileName(pdfUri!!), file!!)

        RetrofitClient.instance.insertSurat(judul, tujuan, tanggalMulai, tanggalBerakhir, filePart)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    // Sembunyikan loading dan enable tombol kembali
                    binding.progressBar.visibility = View.INVISIBLE
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "Simpan"

                    if (response.isSuccessful) {
                        Toast.makeText(applicationContext, "Data berhasil dikirim!", Toast.LENGTH_SHORT).show()
                        addEventToCalendar(binding.etJudul.text.toString(),
                            binding.etTanggalMulai.text.toString(),
                            binding.etTanggalBerakhir.text.toString())

                        // Reset form setelah berhasil
                        resetForm()
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

    // Fungsi tambahan untuk reset form
    private fun resetForm() {
        binding.etJudul.text?.clear()
        binding.etTujuan.text?.clear()
        binding.etTanggalMulai.text?.clear()
        binding.etTanggalBerakhir.text?.clear()
        clearSelectedFile()
    }

//    private fun addEventToCalendar(title: String, startDate: String, endDate: String) {
//        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val startMillis = formatter.parse(startDate)?.time ?: return
//        val endMillis = formatter.parse(endDate)?.time ?: return
//
//        val intent = Intent(Intent.ACTION_INSERT).apply {
//            data = android.provider.CalendarContract.Events.CONTENT_URI
//            putExtra(android.provider.CalendarContract.Events.TITLE, title)
//            putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Surat terkait: $title")
//            putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, "PTPN IV")
//            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
//            putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
//            putExtra(android.provider.CalendarContract.Events.ALL_DAY, true)
//        }
//
//        if (intent.resolveActivity(packageManager) != null) {
//            startActivity(intent)
//        } else {
//            Toast.makeText(this, "Tidak ada aplikasi kalender yang tersedia!", Toast.LENGTH_SHORT).show()
//        }
//    }


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

                // Tambahkan 1 hari ke tanggal akhir karena model "all-day" event
                val endDateTimePlusDay = Calendar.getInstance().apply {
                    time = endDateTime
                    add(Calendar.DAY_OF_MONTH, 1) // Penting untuk event sepanjang hari!
                }.time

                // Log untuk debugging
                Log.d("CALENDAR_DEBUG", "Start Date: $startDateTime, End Date: $endDateTimePlusDay")

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

                // Perbaikan: Gunakan RFC3339 timestamp untuk DateTime
                val start = EventDateTime()
                    .setDate(DateTime(true, startDateTime.time, 0)) // Set isDateOnly=true untuk all-day event
                    .setTimeZone("Asia/Jakarta")
                event.setStart(start)

                val end = EventDateTime()
                    .setDate(DateTime(true, endDateTimePlusDay.time, 0)) // Gunakan tanggal+1 untuk end date
                    .setTimeZone("Asia/Jakarta")
                event.setEnd(end)

                // Debug output
                Log.d("CALENDAR_API", "Event yang akan ditambahkan: ${event.toPrettyString()}")

                // Masukkan event
                val calendarId = "primary"
                val insertedEvent = calendar.events().insert(calendarId, event).execute()

                withContext(Dispatchers.Main) {
                    if (insertedEvent != null && insertedEvent.htmlLink != null) {
                        Toast.makeText(
                            applicationContext,
                            "Event berhasil ditambahkan ke Google Calendar",
                            Toast.LENGTH_SHORT
                        ).show()
                        val eventId = insertedEvent.id
                        Log.d("CALENDAR_API", "Event ID: $eventId")
                    }
                }
            } catch (e: Exception) {
                Log.e("CALENDAR_ERROR", "Error menambahkan event: ${e.message}")
                Log.e("CALENDAR_ERROR", "Stack trace: ${e.stackTraceToString()}")
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
        // Menggunakan GoogleAccountCredential dengan client ID dari resources
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext,
            listOf(CalendarScopes.CALENDAR)
        )

        // Mengambil email dari SharedPreferences
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", null)

        if (userEmail.isNullOrEmpty()) {
            // MASALAH: Toast ini dijalankan langsung, tidak di Main thread
            // jika dipanggil dari thread background
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
