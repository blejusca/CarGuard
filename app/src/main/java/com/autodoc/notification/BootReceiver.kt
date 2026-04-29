package com.autodoc.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autodoc.data.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = DatabaseProvider.getDatabase(context)
                val scheduler = AutoDocNotificationScheduler(context)

                val cars = database.carDao().observeCars().first()
                val documents = database.documentDao().observeDocuments().first()

                documents.forEach { document ->

                    val car = cars.firstOrNull { it.id == document.carId }

                    val carName = if (car != null) {
                        "${car.brand} ${car.model} - ${car.plate}"
                    } else {
                        "Masina necunoscuta"
                    }

                    scheduler.schedule(
                        documentId = document.id,
                        type = document.type,
                        carName = carName,
                        expiry = document.expiryDate,
                        daysBefore = document.reminderDaysBefore
                    )
                }

            } finally {
                withContext(Dispatchers.Main) {
                    pendingResult.finish()
                }
            }
        }
    }
}