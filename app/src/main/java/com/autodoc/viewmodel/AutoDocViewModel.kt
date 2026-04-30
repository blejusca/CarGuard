package com.autodoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodoc.data.dao.CarDao
import com.autodoc.data.dao.DocumentDao
import com.autodoc.data.entity.CarEntity
import com.autodoc.data.entity.DocumentEntity
import com.autodoc.domain.mapper.toUi
import com.autodoc.notification.AutoDocNotificationScheduler
import com.autodoc.ui.CarUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutoDocViewModel(
    private val carDao: CarDao,
    private val documentDao: DocumentDao,
    private val scheduler: AutoDocNotificationScheduler
) : ViewModel() {

    val cars: StateFlow<List<CarUi>> = combine(
        carDao.observeCars(),
        documentDao.observeDocuments()
    ) { cars, documents ->
        cars.map { car ->
            car.toUi(documents)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addCar(
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String = "",
        ownerPhone: String = "",
        ownerEmail: String = "",
        ownerNotes: String = ""
    ) {
        val cleanBrand = brand.trim()
        val cleanModel = model.trim()
        val cleanPlate = plate.trim().uppercase()
        val cleanEngine = engine.trim().ifBlank { "Nespecificat" }

        val cleanOwnerName = ownerName.trim()
        val cleanOwnerPhone = ownerPhone.trim()
        val cleanOwnerEmail = ownerEmail.trim()
        val cleanOwnerNotes = ownerNotes.trim()

        if (cleanBrand.isBlank() || cleanModel.isBlank() || cleanPlate.isBlank()) {
            return
        }

        viewModelScope.launch {
            carDao.insert(
                CarEntity(
                    brand = cleanBrand,
                    model = cleanModel,
                    plate = cleanPlate,
                    year = year,
                    engine = cleanEngine,
                    ownerName = cleanOwnerName,
                    ownerPhone = cleanOwnerPhone,
                    ownerEmail = cleanOwnerEmail,
                    ownerNotes = cleanOwnerNotes
                )
            )
        }
    }

    fun updateCar(
        carId: Int,
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String = "",
        ownerPhone: String = "",
        ownerEmail: String = "",
        ownerNotes: String = ""
    ) {
        if (carId <= 0) {
            return
        }

        val cleanBrand = brand.trim()
        val cleanModel = model.trim()
        val cleanPlate = plate.trim().uppercase()
        val cleanEngine = engine.trim().ifBlank { "Nespecificat" }

        val cleanOwnerName = ownerName.trim()
        val cleanOwnerPhone = ownerPhone.trim()
        val cleanOwnerEmail = ownerEmail.trim()
        val cleanOwnerNotes = ownerNotes.trim()

        if (cleanBrand.isBlank() || cleanModel.isBlank() || cleanPlate.isBlank()) {
            return
        }

        viewModelScope.launch {
            carDao.updateCar(
                carId = carId,
                brand = cleanBrand,
                model = cleanModel,
                plate = cleanPlate,
                year = year,
                engine = cleanEngine,
                ownerName = cleanOwnerName,
                ownerPhone = cleanOwnerPhone,
                ownerEmail = cleanOwnerEmail,
                ownerNotes = cleanOwnerNotes
            )
        }
    }

    fun deleteCar(carId: Int) {
        if (carId <= 0) {
            return
        }

        viewModelScope.launch {
            val car = cars.value.firstOrNull { it.id == carId }

            car?.documents?.forEach { document ->
                scheduler.cancel(document.id)
            }

            carDao.deleteCar(carId)
        }
    }

    fun addDocument(
        carId: Int,
        type: String,
        expiry: Long,
        daysBefore: Int
    ) {
        if (carId <= 0 || expiry <= 0L) {
            return
        }

        viewModelScope.launch {
            val cleanType = normalizeDocumentType(type)

            if (cleanType.isBlank()) {
                return@launch
            }

            val car = carDao.getCarById(carId)

            val carName = if (car != null) {
                "${car.brand} ${car.model} - ${car.plate}"
            } else {
                "Masina"
            }

            val safeDaysBefore = daysBefore.coerceAtLeast(0)

            val existing = documentDao.getDocumentByCarIdAndType(
                carId = carId,
                type = cleanType
            )

            if (existing != null) {
                documentDao.updateExpiryDate(
                    documentId = existing.id,
                    expiryDateMillis = expiry
                )

                scheduler.cancel(existing.id)

                scheduler.schedule(
                    documentId = existing.id,
                    type = cleanType,
                    carName = carName,
                    expiry = expiry,
                    daysBefore = safeDaysBefore
                )

                return@launch
            }

            val documentId = documentDao.insert(
                DocumentEntity(
                    id = 0,
                    carId = carId,
                    type = cleanType,
                    expiryDate = expiry,
                    reminderDaysBefore = safeDaysBefore,
                    notifiedExpired = false,
                    notifiedToday = false,
                    notifiedTomorrow = false,
                    notifiedReminder = false
                )
            )

            scheduler.schedule(
                documentId = documentId.toInt(),
                type = cleanType,
                carName = carName,
                expiry = expiry,
                daysBefore = safeDaysBefore
            )
        }
    }

    fun deleteDocument(documentId: Int) {
        if (documentId <= 0) {
            return
        }

        viewModelScope.launch {
            scheduler.cancel(documentId)
            documentDao.deleteById(documentId)
        }
    }

    fun updateDocumentExpiry(
        documentId: Int,
        expiryDateMillis: Long
    ) {
        if (documentId <= 0 || expiryDateMillis <= 0L) {
            return
        }

        viewModelScope.launch {
            val carWithDocument = cars.value.firstOrNull { car ->
                car.documents.any { document ->
                    document.id == documentId
                }
            }

            val document = carWithDocument
                ?.documents
                ?.firstOrNull { it.id == documentId }

            documentDao.updateExpiryDate(
                documentId = documentId,
                expiryDateMillis = expiryDateMillis
            )

            if (document != null && carWithDocument != null) {
                val carName =
                    "${carWithDocument.brand} ${carWithDocument.model} - ${carWithDocument.plate}"

                scheduler.cancel(documentId)

                scheduler.schedule(
                    documentId = documentId,
                    type = document.type,
                    carName = carName,
                    expiry = expiryDateMillis,
                    daysBefore = document.reminderDaysBefore
                )
            }
        }
    }

    fun markDocumentManuallyNotified(documentId: Int) {
        if (documentId <= 0) {
            return
        }

        viewModelScope.launch {
            documentDao.markReminderNotified(documentId)
        }
    }

    private fun normalizeDocumentType(type: String): String {
        return when (type.trim().lowercase()) {
            "itp" -> "ITP"
            "rca" -> "RCA"
            "casco" -> "CASCO"
            "rovinieta" -> "Rovinieta"
            "revizie" -> "Revizie"
            else -> type.trim()
        }
    }
}