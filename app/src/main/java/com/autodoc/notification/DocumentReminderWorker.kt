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
        return try {
            val type = inputData.getString("documentType").orEmpty().ifBlank { "Document auto" }
            val carName = inputData.getString("carName").orEmpty().ifBlank { "Masina" }
            val expiryMillis = inputData.getLong("expiryDateMillis", 0L)
            val rawNotificationId = inputData.getInt("notificationId", 0)
            val notificationId = if (rawNotificationId > 0) {
                rawNotificationId
            } else {
                createSafeNotificationId()
            }

            if (expiryMillis <= 0L) {
                return Result.success()
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
                daysLeft < 0 -> {
                    val days = abs(daysLeft)
                    "a expirat de $days ${if (days == 1) "zi" else "zile"}"
                }

                daysLeft == 0 -> "expira azi"
                daysLeft == 1 -> "expira maine"
                else -> "expira in $daysLeft zile"
            }

            val title = "$type $statusText"
            val message = "$carName • Verifica documentul in aplicatie"

            showNotification(
                context = applicationContext,
                notificationId = notificationId,
                title = title,
                message = message
            )

            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        private const val CHANNEL_ID = "car_doc_channel"

        fun showNotification(
            context: Context,
            title: String,
            message: String
        ) {
            try {
                showNotification(
                    context = context,
                    notificationId = createSafeNotificationId(),
                    title = title,
                    message = message
                )
            } catch (e: Exception) {
                // Notificarea nu trebuie sa inchida aplicatia.
            }
        }

        private fun showNotification(
            context: Context,
            notificationId: Int,
            title: String,
            message: String
        ) {
            try {
                if (!hasNotificationPermission(context)) {
                    return
                }

                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        ?: return

                createNotificationChannel(manager)

                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val safeTitle = title.ifBlank { "Document auto" }
                val safeMessage = message.ifBlank { "Ai un document auto de verificat." }

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(safeTitle)
                    .setContentText(safeMessage)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(safeMessage)
                    )
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                manager.notify(notificationId, notification)
            } catch (e: Exception) {
                // Notificarea nu trebuie sa inchida aplicatia.
            }
        }

        private fun createNotificationChannel(manager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Documente Auto",
                    NotificationManager.IMPORTANCE_DEFAULT
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
            return abs((System.currentTimeMillis() % Int.MAX_VALUE).toInt()).coerceAtLeast(1)
        }
    }
}