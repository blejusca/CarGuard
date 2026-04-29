package com.autodoc.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autodoc.data.DatabaseProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DailyCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = DatabaseProvider.getDatabase(applicationContext)

            val documentDao = db.documentDao()
            val carDao = db.carDao()

            val documents = documentDao.getAllDocuments()
            val today = LocalDate.now()

            var expiredCount = 0
            var todayCount = 0
            var soonCount = 0
            var clientsToNotify = 0

            documents.forEach { doc ->
                val car = carDao.getCarById(doc.carId) ?: return@forEach

                val expiryDate = Instant.ofEpochMilli(doc.expiryDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val daysLeft = ChronoUnit.DAYS.between(today, expiryDate).toInt()

                val hasContact =
                    car.ownerPhone.isNotBlank() || car.ownerEmail.isNotBlank()

                when {
                    daysLeft < 0 -> expiredCount++
                    daysLeft == 0 -> todayCount++
                    daysLeft in 1..doc.reminderDaysBefore -> soonCount++
                }

                if (
                    hasContact &&
                    daysLeft <= doc.reminderDaysBefore
                ) {
                    clientsToNotify++
                }
            }

            val totalUrgent = expiredCount + todayCount + soonCount

            if (totalUrgent > 0) {
                val title = "Documente auto de verificat"

                val message = buildString {
                    append("$totalUrgent documente necesita atentie.")

                    if (expiredCount > 0) {
                        append(" Expirate: $expiredCount.")
                    }

                    if (todayCount > 0) {
                        append(" Expira azi: $todayCount.")
                    }

                    if (soonCount > 0) {
                        append(" In curand: $soonCount.")
                    }

                    if (clientsToNotify > 0) {
                        append(" Clienti de notificat: $clientsToNotify.")
                    }
                }

                DocumentReminderWorker.showNotification(
                    context = applicationContext,
                    title = title,
                    message = message
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}