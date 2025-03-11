package com.example.projectkp.notification

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.projectkp.api.RetrofitClient
import com.example.projectkp.model.surat.Surat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SuratNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val notificationHelper = NotificationHelper(context)
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("id"))

    override fun doWork(): Result {
        Log.d("SuratNotificationWorker", "Memeriksa surat yang berakhir hari ini")

        // Dapatkan tanggal hari ini untuk perbandingan
        val todayStr = apiDateFormat.format(Date())

        // Panggil API untuk mendapatkan daftar surat
        RetrofitClient.instance.getSuratList()
            .enqueue(object : Callback<List<Surat>> {
                override fun onResponse(call: Call<List<Surat>>, response: Response<List<Surat>>) {
                    if (response.isSuccessful) {
                        val suratList = response.body() ?: emptyList()

                        // Filter surat yang berakhir hari ini
                        val expiredSurats = suratList.filter { surat ->
                            surat.tanggal_berakhir == todayStr
                        }

                        Log.d("SuratNotificationWorker", "Ditemukan ${expiredSurats.size} surat yang berakhir hari ini")

                        if (expiredSurats.isNotEmpty()) {
                            // Buat notifikasi untuk setiap surat yang berakhir
                            expiredSurats.forEachIndexed { index, surat ->
                                notificationHelper.showNotification(surat, index + 1)
                            }

                            // Jika ada lebih dari 1 surat yang berakhir, tambahkan notifikasi ringkasan
                            if (expiredSurats.size > 1) {
                                notificationHelper.showSummaryNotification(expiredSurats.size)
                            }
                        }
                    } else {
                        Log.e("SuratNotificationWorker", "Gagal memuat data: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<List<Surat>>, t: Throwable) {
                    Log.e("SuratNotificationWorker", "Error: ${t.message}", t)
                }
            })

        return Result.success()
    }
}