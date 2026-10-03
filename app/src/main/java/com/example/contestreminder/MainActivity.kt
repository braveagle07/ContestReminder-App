package com.example.contestreminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var listText: TextView

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op, user's choice either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        listText = findViewById(R.id.listText)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val exactAlarmButton = findViewById<Button>(R.id.exactAlarmButton)

        requestNotificationPermissionIfNeeded()
        schedulePeriodicWork()
        refreshNow()

        refreshButton.setOnClickListener { refreshNow() }
        exactAlarmButton.setOnClickListener { requestExactAlarmPermission() }

        updateExactAlarmStatus()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Exact alarms already allowed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Not needed on this Android version", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateExactAlarmStatus() {
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true
        statusText.text = if (allowed) {
            "Exact alarms: allowed \u2705"
        } else {
            "Exact alarms: NOT allowed \u274C \u2014 tap 'Allow exact alarms' below or reminders may fire late"
        }
    }

    private fun schedulePeriodicWork() {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RefreshWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun refreshNow() {
        listText.text = "Loading contests..."
        lifecycleScope.launch {
            val contests = withContext(Dispatchers.IO) { ContestFetcher.fetchUpcoming() }
            AlarmScheduler.scheduleAllNew(this@MainActivity, contests)
            renderList(contests)
        }
    }

    private fun renderList(contests: List<Contest>) {
        if (contests.isEmpty()) {
            listText.text = "No upcoming CodeChef / LeetCode / Codeforces contests found right now."
            return
        }
        val fmt = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
        val sb = StringBuilder()
        for (c in contests) {
            sb.append("\u2022 [${c.site}] ${c.name}\n")
            sb.append("   ${fmt.format(java.util.Date(c.startTimeMillis))}\n\n")
        }
        listText.text = sb.toString()
    }
}
