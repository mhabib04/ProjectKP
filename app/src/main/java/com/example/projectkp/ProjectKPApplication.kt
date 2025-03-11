package com.example.projectkp

import android.app.Application
import android.util.Log
import com.example.projectkp.notification.NotificationScheduler

class ProjectKPApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Jadwalkan notifikasi saat aplikasi pertama kali dimulai
        NotificationScheduler.scheduleNotification(this)
        Log.d("ProjectKPApplication", "Aplikasi dimulai, notifikasi dijadwalkan")
    }
}