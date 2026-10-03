package com.example.contestreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CHANNEL_ID = "contest_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contest Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for CodeChef, LeetCode and Codeforces contests"
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    // type: "TODAY" (morning heads-up) or "SOON" (starting in ~15 min)
    fun notify(context: Context, requestCode: Int, contestName: String, site: String, url: String, type: String) {
        ensureChannel(context)

        val title = when (site) {
            "CodeChef" -> "Yo bro, CodeChef time \uD83C\uDF6B"
            "LeetCode" -> "Yo bro, LeetCode's up \uD83E\uDDE9"
            "Codeforces" -> "Yo bro, Codeforces round \u26A1"
            else -> "Contest reminder"
        }

        val body = when (type) {
            "TODAY" -> "$contestName is today. Don't forget to attend it."
            else -> "$contestName is starting in ~15 min. Go attend it, bro."
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.ifBlank { "https://google.com/search?q=$contestName" }))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        }
    }
}
