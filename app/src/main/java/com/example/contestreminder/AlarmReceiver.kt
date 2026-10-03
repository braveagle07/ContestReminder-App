package com.example.contestreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Contest"
        val site = intent.getStringExtra("site") ?: ""
        val url = intent.getStringExtra("url") ?: ""
        val type = intent.getStringExtra("type") ?: "SOON"
        val requestCode = intent.getIntExtra("requestCode", (name + site).hashCode())

        NotificationHelper.notify(context, requestCode, name, site, url, type)
    }
}
