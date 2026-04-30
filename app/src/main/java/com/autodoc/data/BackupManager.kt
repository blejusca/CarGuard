package com.autodoc.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.autodoc.data.entity.CarEntity
import com.autodoc.data.entity.DocumentEntity
import com.autodoc.notification.AutoDocNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupManager {

    suspend fun saveBackupToFile(context: Context): File {
        return withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)

            val cars = db.carDao().getAllCarsSync()
            val documents = db.documentDao().getAllDocumentsSync()

            val json = buildJson(cars, documents)

            val file = File(
                context.getExternalFilesDir(null),
                "autodoc_backup.json"
            )

            file.writeText(json)
            file
        }
    }

    suspend fun saveBackupToDownloads(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = DatabaseProvider.getDatabase(context)

                val cars = db.carDao().getAllCarsSync()
                val documents = db.documentDao().getAllDocumentsSync()

                val json = buildJson(cars, documents)
                val fileName = "autodoc_backup.json"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver

                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }

                    val uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    ) ?: return@withContext false

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    } ?: return@withContext false

                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)

                    true
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )

                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    val file = File(downloadsDir, fileName)
                    file.writeText(json)

                    true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importBackupFromUri(
        context: Context,
        uri: Uri,
        scheduler: AutoDocNotificationScheduler
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val db = DatabaseProvider.getDatabase(context)

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext false

                val jsonText = inputStream.bufferedReader().use { it.readText() }

                val root = JSONObject(jsonText)

                if (!root.has("cars") || !root.has("documents")) {
                    return@withContext false
                }

                val carsArray = root.getJSONArray("cars")
                val documentsArray = root.getJSONArray("documents")

                db.documentDao().deleteAll()
                db.carDao().deleteAll()

                val carIdMap = mutableMapOf<Int, Long>()
                val importedCarsByNewId = mutableMapOf<Int, CarEntity>()

                for (i in 0 until carsArray.length()) {
                    val obj = carsArray.getJSONObject(i)

                    val oldId = obj.optInt("id", 0)

                    val carToInsert = CarEntity(
                        id = 0,
                        brand = obj.optString("brand", ""),
                        model = obj.optString("model", ""),
                        plate = obj.optString("plate", ""),
                        year = obj.optInt("year", 0),
                        engine = obj.optString("engine", ""),
                        ownerName = obj.optString("ownerName", ""),
                        ownerPhone = obj.optString("ownerPhone", ""),
                        ownerEmail = obj.optString("ownerEmail", ""),
                        ownerNotes = obj.optString("ownerNotes", "")
                    )

                    val newId = db.carDao().insert(carToInsert)

                    carIdMap[oldId] = newId
                    importedCarsByNewId[newId.toInt()] = carToInsert.copy(id = newId.toInt())
                }

                for (i in 0 until documentsArray.length()) {
                    val obj = documentsArray.getJSONObject(i)

                    val oldCarId = obj.optInt("carId", 0)
                    val newCarId = carIdMap[oldCarId]?.toInt() ?: continue

                    val documentToInsert = DocumentEntity(
                        id = 0,
                        carId = newCarId,
                        type = obj.optString("type", ""),
                        expiryDate = obj.optLong("expiryDate", 0L),
                        reminderDaysBefore = obj.optInt("reminderDaysBefore", 7),
                        notifiedExpired = obj.optBoolean("notifiedExpired", false),
                        notifiedToday = obj.optBoolean("notifiedToday", false),
                        notifiedTomorrow = obj.optBoolean("notifiedTomorrow", false),
                        notifiedReminder = obj.optBoolean("notifiedReminder", false)
                    )

                    val newDocumentId = db.documentDao().insert(documentToInsert).toInt()

                    val importedCar = importedCarsByNewId[newCarId]
                    val carName = if (importedCar != null) {
                        "${importedCar.brand} ${importedCar.model} - ${importedCar.plate}"
                    } else {
                        "Masina necunoscuta"
                    }

                    scheduler.schedule(
                        documentId = newDocumentId,
                        type = documentToInsert.type,
                        carName = carName,
                        expiry = documentToInsert.expiryDate,
                        daysBefore = documentToInsert.reminderDaysBefore
                    )
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private fun buildJson(
        cars: List<CarEntity>,
        documents: List<DocumentEntity>
    ): String {
        val root = JSONObject()

        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val carsArray = JSONArray()
        cars.forEach { car ->
            val obj = JSONObject()
            obj.put("id", car.id)
            obj.put("brand", car.brand)
            obj.put("model", car.model)
            obj.put("plate", car.plate)
            obj.put("year", car.year)
            obj.put("engine", car.engine)
            obj.put("ownerName", car.ownerName)
            obj.put("ownerPhone", car.ownerPhone)
            obj.put("ownerEmail", car.ownerEmail)
            obj.put("ownerNotes", car.ownerNotes)
            carsArray.put(obj)
        }

        val documentsArray = JSONArray()
        documents.forEach { document ->
            val obj = JSONObject()
            obj.put("id", document.id)
            obj.put("carId", document.carId)
            obj.put("type", document.type)
            obj.put("expiryDate", document.expiryDate)
            obj.put("reminderDaysBefore", document.reminderDaysBefore)
            obj.put("notifiedExpired", document.notifiedExpired)
            obj.put("notifiedToday", document.notifiedToday)
            obj.put("notifiedTomorrow", document.notifiedTomorrow)
            obj.put("notifiedReminder", document.notifiedReminder)
            documentsArray.put(obj)
        }

        root.put("cars", carsArray)
        root.put("documents", documentsArray)

        return root.toString(2)
    }
}