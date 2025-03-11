package com.example.projectkp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.projectkp.DaftarSuratActivity
import com.example.projectkp.R
import com.example.projectkp.model.surat.Surat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "SURAT_NOTIFICATION_CHANNEL"
        const val GROUP_KEY = "com.example.projectkp.SURAT_NOTIFICATIONS"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notifikasi Surat"
            val descriptionText = "Notifikasi untuk surat yang berakhir"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(surat: Surat, notificationId: Int) {
        val intent = Intent(context, DaftarSuratActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Ganti dengan icon yang sesuai
            .setLargeIcon(largeIcon)
            .setContentTitle("Surat Berakhir Hari Ini")
            .setContentText("Surat ${surat.judul} telah berakhir masa berlakunya")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Surat ${surat.judul} (No: ${surat.id}) telah berakhir masa berlakunya pada tanggal ${surat.tanggal_berakhir}"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun showSummaryNotification(expiredSuratCount: Int) {
        if (expiredSuratCount <= 1) return

        val intent = Intent(context, DaftarSuratActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Surat Berakhir")
            .setContentText("$expiredSuratCount surat telah berakhir masa berlakunya hari ini")
            .setSmallIcon(R.drawable.ic_notification) // Ganti dengan icon yang sesuai
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("$expiredSuratCount surat berakhirrr"))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, summaryNotification)
    }
}