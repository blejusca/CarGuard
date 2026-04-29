package com.autodoc.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autodoc.data.DatabaseProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

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

            documents.forEach { doc ->
                val car = carDao.getCarById(doc.carId) ?: return@forEach

                val expiryDate = Instant.ofEpochMilli(doc.expiryDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val daysLeft = ChronoUnit.DAYS.between(today, expiryDate).toInt()
                val carInfo = "${car.brand} ${car.model} (${car.plate})"

                when {
                    daysLeft < 0 && !doc.notifiedExpired -> {
                        DocumentReminderWorker.showNotification(
                            context = applicationContext,
                            title = "Document expirat",
                            message = "${doc.type} pentru $carInfo este expirat de ${abs(daysLeft)} zile"
                        )

                        documentDao.markExpiredNotified(doc.id)
                    }

                    daysLeft == 0 && !doc.notifiedToday -> {
                        DocumentReminderWorker.showNotification(
                            context = applicationContext,
                            title = "Documentul expira azi",
                            message = "${doc.type} pentru $carInfo expira astazi"
                        )

                        documentDao.markTodayNotified(doc.id)
                    }

                    daysLeft == 1 && !doc.notifiedTomorrow -> {
                        DocumentReminderWorker.showNotification(
                            context = applicationContext,
                            title = "Documentul expira maine",
                            message = "${doc.type} pentru $carInfo expira maine"
                        )

                        documentDao.markTomorrowNotified(doc.id)
                    }

                    daysLeft in 2..7 &&
                            daysLeft == doc.reminderDaysBefore &&
                            !doc.notifiedReminder -> {
                        DocumentReminderWorker.showNotification(
                            context = applicationContext,
                            title = "Document urgent",
                            message = "${doc.type} pentru $carInfo expira in $daysLeft zile"
                        )

                        documentDao.markReminderNotified(doc.id)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}