package com.example.projectkp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class NotificationScheduler {
    companion object {
        private const val ALARM_REQUEST_CODE = 100

        fun scheduleNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Buat Intent untuk AlarmReceiver
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set waktu alarm untuk jam 8 pagi setiap hari
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 13)
                set(Calendar.MINUTE, 55)
                set(Calendar.SECOND, 0)

                // Jika waktu saat ini sudah lewat jam 8 pagi, jadwalkan untuk besok
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val triggerTime = calendar.timeInMillis

            // Jadwalkan alarm berulang
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d("NotificationScheduler", "Notifikasi dijadwalkan untuk: ${calendar.time}")
        }

        fun cancelScheduledNotification(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Batalkan alarm yang sudah dijadwalkan
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            Log.d("NotificationScheduler", "Penjadwalan notifikasi dibatalkan")
        }
    }
}
