package com.autodoc.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodoc.data.AppPlanManager
import com.autodoc.data.dao.CarDao
import com.autodoc.data.dao.DocumentDao
import com.autodoc.data.entity.CarEntity
import com.autodoc.data.entity.DocumentEntity
import com.autodoc.domain.mapper.toUi
import com.autodoc.notification.AutoDocNotificationScheduler
import com.autodoc.ui.CarUi
import com.autodoc.ui.normalizeDocumentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutoDocViewModel(
    private val carDao: CarDao,
    private val documentDao: DocumentDao,
    private val scheduler: AutoDocNotificationScheduler,
    private val appPlanManager: AppPlanManager
) : ViewModel() {

    private val _isProPlan = MutableStateFlow(appPlanManager.isProPlan())
    val isProPlan: StateFlow<Boolean> = _isProPlan.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

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

    fun setProPlan(enabled: Boolean) {
        appPlanManager.setProPlan(enabled)
        _isProPlan.value = appPlanManager.isProPlan()

        _userMessage.value = if (_isProPlan.value) {
            "Plan Pro activ. Limita de masini este dezactivata."
        } else {
            "Plan Free activ. Poti adauga maximum ${appPlanManager.getFreePlanMaxCars()} masini."
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun isValidPhone(phone: String): Boolean {
        val cleaned = phone
            .trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        val digits = cleaned.filter { it.isDigit() }

        if (digits.length < 8 || digits.length > 13) {
            return false
        }

        return when {
            // Romania local mobil: 07xxxxxxxx
            digits.startsWith("07") && digits.length == 10 -> true

            // Romania international: 407xxxxxxxx
            digits.startsWith("407") && digits.length == 11 -> true

            // Romania international cu 00: 00407xxxxxxxx
            digits.startsWith("00407") && digits.length == 13 -> true

            // Danemarca local: 8 cifre
            digits.length == 8 && digits.first() in '2'..'9' -> true

            // Danemarca international: 45xxxxxxxx
            digits.startsWith("45") && digits.length == 10 -> true

            // Danemarca international cu 00: 0045xxxxxxxx
            digits.startsWith("0045") && digits.length == 12 -> true

            else -> false
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()

        if (cleanEmail.isBlank()) {
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return false
        }

        val domain = cleanEmail.substringAfter("@", missingDelimiterValue = "")
        val tld = domain.substringAfterLast(".", missingDelimiterValue = "")

        if (domain.isBlank() || tld.isBlank()) {
            return false
        }

        val blockedTlds = setOf(
            "xom",
            "con",
            "comm",
            "cim",
            "vom",
            "gmai",
            "gmial"
        )

        val allowedTlds = setOf(
            "ro",
            "dk",
            "com",
            "net",
            "org",
            "eu",
            "de",
            "co",
            "info",
            "biz"
        )

        if (tld in blockedTlds) {
            return false
        }

        if (tld !in allowedTlds) {
            return false
        }

        if (domain.contains("..")) {
            return false
        }

        return true
    }

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
            _userMessage.value = "Completeaza campurile obligatorii."
            return
        }

        if (cleanOwnerPhone.isNotBlank() && !isValidPhone(cleanOwnerPhone)) {
            _userMessage.value = "Numar de telefon invalid. Acceptat: 07..., +40..., 0040..., numar DK sau +45..."
            return
        }

        if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) {
            _userMessage.value = "Email invalid. Verifica adresa si extensia domeniului."
            return
        }

        viewModelScope.launch {
            try {
                val currentCarsCount = cars.value.size
                val maxFreeCars = appPlanManager.getFreePlanMaxCars()

                val duplicatePlateExists = cars.value.any { existingCar ->
                    existingCar.plate.trim().uppercase() == cleanPlate
                }

                if (duplicatePlateExists) {
                    _userMessage.value =
                        "Exista deja o masina cu numarul $cleanPlate. Verifica lista sau editeaza masina existenta."
                    return@launch
                }

                if (!appPlanManager.isProPlan() && currentCarsCount >= maxFreeCars) {
                    _isProPlan.value = false
                    _userMessage.value =
                        "Ai atins limita planului Free: maximum $maxFreeCars masini. Activeaza Pro pentru masini nelimitate."
                    return@launch
                }

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
            } catch (e: Exception) {
                _userMessage.value = if (isUniqueConstraintError(e)) {
                    "Exista deja o masina cu numarul $cleanPlate. Numarul de inmatriculare trebuie sa fie unic."
                } else {
                    "Eroare la salvarea masinii. Incearca din nou."
                }
            }
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
            _userMessage.value = "Completeaza campurile obligatorii."
            return
        }

        if (cleanOwnerPhone.isNotBlank() && !isValidPhone(cleanOwnerPhone)) {
            _userMessage.value = "Numar de telefon invalid. Acceptat: 07..., +40..., 0040..., numar DK sau +45..."
            return
        }

        if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) {
            _userMessage.value = "Email invalid. Verifica adresa si extensia domeniului."
            return
        }

        viewModelScope.launch {
            try {
                val duplicatePlateExists = cars.value.any { existingCar ->
                    existingCar.id != carId &&
                            existingCar.plate.trim().uppercase() == cleanPlate
                }

                if (duplicatePlateExists) {
                    _userMessage.value =
                        "Exista deja o alta masina cu numarul $cleanPlate. Numarul de inmatriculare trebuie sa fie unic."
                    return@launch
                }

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
            } catch (e: Exception) {
                _userMessage.value = if (isUniqueConstraintError(e)) {
                    "Exista deja o alta masina cu numarul $cleanPlate. Numarul de inmatriculare trebuie sa fie unic."
                } else {
                    "Eroare la actualizarea masinii. Incearca din nou."
                }
            }
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
                    notifiedReminder = false,
                    manuallyNotified = false
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
            documentDao.markManuallyNotified(documentId)
        }
    }

    private fun isUniqueConstraintError(error: Throwable): Boolean {
        val message = error.message.orEmpty()

        return message.contains("UNIQUE", ignoreCase = true) ||
                message.contains("constraint", ignoreCase = true) ||
                message.contains("index_cars_plate", ignoreCase = true) ||
                message.contains("cars.plate", ignoreCase = true)
    }
}