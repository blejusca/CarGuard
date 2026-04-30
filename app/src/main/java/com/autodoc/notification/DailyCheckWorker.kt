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
            var tomorrowCount = 0
            var soonCount = 0
            var clientsToNotify = 0

            documents.forEach { document ->
                val car = carDao.getCarById(document.carId) ?: return@forEach

                val expiryDate = Instant.ofEpochMilli(document.expiryDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val daysLeft = ChronoUnit.DAYS.between(today, expiryDate).toInt()

                val hasContact = car.ownerPhone.isNotBlank() || car.ownerEmail.isNotBlank()
                val isInReminderWindow = daysLeft <= document.reminderDaysBefore

                when {
                    daysLeft < 0 -> expiredCount++
                    daysLeft == 0 -> todayCount++
                    daysLeft == 1 -> tomorrowCount++
                    daysLeft in 2..document.reminderDaysBefore -> soonCount++
                }

                if (hasContact && isInReminderWindow) {
                    clientsToNotify++
                }
            }

            val totalImportant = expiredCount + todayCount + tomorrowCount + soonCount

            if (totalImportant > 0) {
                val title = "Documente auto de verificat"

                val message = buildString {
                    append("$totalImportant documente necesita atentie.")

                    if (expiredCount > 0) {
                        append(" Expirate: $expiredCount.")
                    }

                    if (todayCount > 0) {
                        append(" Expira azi: $todayCount.")
                    }

                    if (tomorrowCount > 0) {
                        append(" Expira maine: $tomorrowCount.")
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