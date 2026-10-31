package com.fitplanner.ai.core.data.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fitplanner.ai.core.model.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun sync(preferences: AppPreferences) {
        val workManager = WorkManager.getInstance(context)
        if (!preferences.notificationsEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val delayMillis = nextDelayMillis(preferences.reminderTime)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun nextDelayMillis(time: String): Long {
        val parsed = runCatching { LocalTime.parse(time) }.getOrDefault(LocalTime.of(18, 0))
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(parsed)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis().coerceAtLeast(1_000L)
    }

    companion object {
        private const val WORK_NAME = "fitplanner_daily_reminder"
    }
}
