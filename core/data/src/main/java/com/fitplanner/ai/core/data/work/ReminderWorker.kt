package com.fitplanner.ai.core.data.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fitplanner.ai.core.data.R
import com.fitplanner.ai.core.data.repository.PlanRepository
import com.fitplanner.ai.core.data.settings.SettingsRepository
import com.fitplanner.ai.core.model.SessionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val planRepository: PlanRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val preferences = settingsRepository.preferences.first()
        if (!preferences.notificationsEnabled) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val plan = planRepository.getActivePlan() ?: return Result.success()
        val today = LocalDate.now()
        val session = plan.weeks
            .flatMap { it.sessions }
            .firstOrNull { it.date == today && it.status != SessionStatus.COMPLETED && it.exercises.isNotEmpty() }
            ?: return Result.success()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de entrenamiento",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Avisos de las sesiones programadas en FitPlanner AI"
                }
            )
        }
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                applicationContext,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hoy toca ${session.name}")
            .setContentText("Tu sesión de ${session.estimatedMinutes} min está lista. Ve a tu ritmo.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "fitplanner_reminders"
        const val NOTIFICATION_ID = 1001
    }
}
