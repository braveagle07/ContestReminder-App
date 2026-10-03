package com.example.contestreminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_PERIODIC_NAME = "contest_refresh_periodic"
        const val TAG = "contest_refresh"
    }

    override suspend fun doWork(): Result {
        return try {
            val contests = withContext(Dispatchers.IO) { ContestFetcher.fetchUpcoming() }
            AlarmScheduler.scheduleAllNew(applicationContext, contests)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
