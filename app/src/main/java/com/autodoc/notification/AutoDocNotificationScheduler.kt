package com.autodoc.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.math.max

class AutoDocNotificationScheduler(
    private val context: Context
) {

    fun schedule(
        documentId: Int,
        type: String,
        carName: String,
        expiry: Long,
        daysBefore: Int
    ) {
        if (documentId <= 0 || expiry <= 0L) {
            return
        }

        cancel(documentId)

        val now = System.currentTimeMillis()
        val safeDaysBefore = max(daysBefore, 0)

        val reminderDays = listOf(
            safeDaysBefore,
            1,
            0
        ).distinct()

        var hasScheduledFutureReminder = false

        reminderDays.forEach { daysBeforeExpiry ->
            val triggerTime =
                expiry - TimeUnit.DAYS.toMillis(daysBeforeExpiry.toLong())

            val delayMillis = triggerTime - now

            if (delayMillis > 0L) {
                enqueueReminder(
                    documentId = documentId,
                    type = type,
                    carName = carName,
                    expiry = expiry,
                    daysBeforeExpiry = daysBeforeExpiry,
                    delayMillis = delayMillis
                )

                hasScheduledFutureReminder = true
            }
        }

        if (!hasScheduledFutureReminder) {
            enqueueReminder(
                documentId = documentId,
                type = type,
                carName = carName,
                expiry = expiry,
                daysBeforeExpiry = EXPIRED_IMMEDIATE_REMINDER,
                delayMillis = 10_000L
            )
        }
    }

    fun cancel(documentId: Int) {
        if (documentId <= 0) {
            return
        }

        val possibleReminderDays = listOf(
            EXPIRED_IMMEDIATE_REMINDER,
            0,
            1,
            3,
            5,
            7,
            10,
            14,
            15,
            30,
            60,
            90
        )

        possibleReminderDays.forEach { daysBeforeExpiry ->
            WorkManager.getInstance(context).cancelUniqueWork(
                workName(documentId, daysBeforeExpiry)
            )
        }
    }

    private fun enqueueReminder(
        documentId: Int,
        type: String,
        carName: String,
        expiry: Long,
        daysBeforeExpiry: Int,
        delayMillis: Long
    ) {
        val notificationId = documentId * 100 + daysBeforeExpiry

        val data = workDataOf(
            "documentType" to type,
            "carName" to carName,
            "notificationId" to notificationId,
            "expiryDateMillis" to expiry
        )

        val request = OneTimeWorkRequestBuilder<DocumentReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(documentId, daysBeforeExpiry),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun workName(
        documentId: Int,
        daysBeforeExpiry: Int
    ): String {
        return "carguard_document_reminder_${documentId}_${daysBeforeExpiry}"
    }

    companion object {
        private const val EXPIRED_IMMEDIATE_REMINDER = -1
    }
}