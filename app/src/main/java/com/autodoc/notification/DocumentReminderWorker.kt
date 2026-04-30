package com.autodoc.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autodoc.MainActivity
import com.autodoc.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class DocumentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type = inputData.getString("documentType") ?: return Result.failure()
        val carName = inputData.getString("carName") ?: "Masina"
        val expiryMillis = inputData.getLong("expiryDateMillis", 0L)
        val notificationId = inputData.getInt("notificationId", 0)

        if (expiryMillis <= 0L) {
            return Result.failure()
        }

        if (!hasNotificationPermission(applicationContext)) {
            return Result.success()
        }

        val expiryDate = Instant.ofEpochMilli(expiryMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val daysLeft = ChronoUnit.DAYS.between(
            LocalDate.now(),
            expiryDate
        ).toInt()

        val statusText = when {
            daysLeft < 0 -> "a expirat"
            daysLeft == 0 -> "expira azi"
            daysLeft == 1 -> "expira maine"
            else -> "expira in $daysLeft zile"
        }

        val title = "$type $statusText"
        val message = "$carName • Data expirarii: $expiryDate"

        showNotification(
            context = applicationContext,
            notificationId = notificationId,
            title = title,
            message = message
        )

        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "car_doc_channel"

        fun showNotification(
            context: Context,
            title: String,
            message: String
        ) {
            showNotification(
                context = context,
                notificationId = createSafeNotificationId(),
                title = title,
                message = message
            )
        }

        private fun showNotification(
            context: Context,
            notificationId: Int,
            title: String,
            message: String
        ) {
            if (!hasNotificationPermission(context)) {
                return
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            createNotificationChannel(manager)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            manager.notify(notificationId, notification)
        }

        private fun createNotificationChannel(manager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Documente Auto",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificari pentru documente auto expirate sau aproape de expirare"
                }

                manager.createNotificationChannel(channel)
            }
        }

        private fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        private fun createSafeNotificationId(): Int {
            return abs((System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        }
    }
}