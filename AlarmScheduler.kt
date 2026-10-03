package com.example.contestreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    private const val PREFS = "contest_reminder_prefs"
    private const val KEY_SCHEDULED = "scheduled_urls"

    fun scheduleAllNew(context: Context, contests: List<Contest>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // entries stored as "url::startTimeMillis" so we can prune by real elapsed time later
        val stored = prefs.getStringSet(KEY_SCHEDULED, emptySet())!!.toMutableSet()
        val scheduledUrls = stored.mapNotNull { it.substringBeforeLast("::", "").ifBlank { null } }.toSet()

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

        for (contest in contests) {
            if (contest.url in scheduledUrls) continue

            // 1) "today" heads-up at 9 AM on the day of the contest (skip if that's already past)
            val dayOfMillis = nineAmOnDayOf(contest.startTimeMillis)
            if (dayOfMillis > System.currentTimeMillis()) {
                schedule(context, am, canExact, contest, dayOfMillis, "TODAY", baseRequestCode(contest, 1))
            }

            // 2) "starting soon" 15 minutes before start
            val soonMillis = contest.startTimeMillis - 15 * 60 * 1000
            if (soonMillis > System.currentTimeMillis()) {
                schedule(context, am, canExact, contest, soonMillis, "SOON", baseRequestCode(contest, 2))
            }

            stored.add("${contest.url}::${contest.startTimeMillis}")
        }

        // Prune bookkeeping for contests whose start time is more than a day in the past,
        // so the stored set doesn't grow forever.
        val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
        val pruned = stored.filter { entry ->
            val ts = entry.substringAfterLast("::", "0").toLongOrNull() ?: 0L
            ts > cutoff
        }.toSet()

        prefs.edit().putStringSet(KEY_SCHEDULED, pruned).apply()
    }

    private fun schedule(
        context: Context,
        am: AlarmManager,
        canExact: Boolean,
        contest: Contest,
        triggerAtMillis: Long,
        type: String,
        requestCode: Int
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("name", contest.name)
            putExtra("site", contest.site)
            putExtra("url", contest.url)
            putExtra("type", type)
            putExtra("requestCode", requestCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // fallback: inexact but still fires roughly on time
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun nineAmOnDayOf(startTimeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startTimeMillis
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun baseRequestCode(contest: Contest, salt: Int): Int {
        return (contest.url.hashCode() * 31 + salt)
    }
}
