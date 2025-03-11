package com.example.projectkp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm received, menjalankan worker untuk notifikasi")

        // Buat one-time work request untuk memeriksa surat yang berakhir
        val workRequest = OneTimeWorkRequestBuilder<SuratNotificationWorker>()
            .build()

        // Jalankan worker
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
