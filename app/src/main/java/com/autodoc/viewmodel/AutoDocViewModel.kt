package com.autodoc.viewmodel

import android.app.Activity
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autodoc.billing.BillingState
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutoDocViewModel(
    private val carDao: CarDao,
    private val documentDao: DocumentDao,
    private val scheduler: AutoDocNotificationScheduler,
    private val appPlanManager: AppPlanManager,
    private val getActivity: () -> Activity?
) : ViewModel() {

    // isProPlan vine direct din BillingManager - sursa de adevar unica
    val isProPlan: StateFlow<Boolean> = appPlanManager.isProFlow

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Expune billingState pentru UI (succes, erori, pending)
    val billingState: StateFlow<BillingState> = appPlanManager.billingManager.billingState

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

    init {
        // Observam billingState pentru a afisa mesaje utilizatorului
        appPlanManager.billingManager.billingState.onEach { state ->
            when (state) {
                is BillingState.PurchaseSuccess -> {
                    _userMessage.value = "Plan Pro activat cu succes! Masini nelimitate disponibile."
                }
                is BillingState.Cancelled -> {
                    // Utilizatorul a anulat - nu afisam mesaj de eroare
                }
                is BillingState.Pending -> {
                    _userMessage.value = "Plata este in curs de procesare. Vei fi notificat cand este finalizata."
                }
                is BillingState.Error -> {
                    _userMessage.value = state.message
                }
                else -> {}
            }
        }.launchIn(viewModelScope)
    }

    fun launchPurchaseFlow() {
        val activity = getActivity()
        if (activity == null) {
            _userMessage.value = "Nu s-a putut deschide fereastra de cumparare. Incearca din nou."
            return
        }
        appPlanManager.billingManager.launchPurchaseFlow(activity)
    }

    fun consumeBillingState() {
        appPlanManager.billingManager.consumeBillingState()
    }

    fun clearUserMessage() {
        _userMessage.value = null
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
            _userMessage.value = "Numar de telefon invalid. Acceptat: 07..., +40..., 0040... sau orice numar international valid."
            return
        }

        if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) {
            _userMessage.value = "Email invalid. Verifica adresa introdusa."
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
                    _userMessage.value =
                        "Ai atins limita planului Free: maximum $maxFreeCars masini. Cumpara Pro pentru masini nelimitate."
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
        if (carId <= 0) return

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
            _userMessage.value = "Numar de telefon invalid. Acceptat: 07..., +40..., 0040... sau orice numar international valid."
            return
        }

        if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) {
            _userMessage.value = "Email invalid. Verifica adresa introdusa."
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
        if (carId <= 0) return

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
        if (carId <= 0 || expiry <= 0L) return

        viewModelScope.launch {
            val cleanType = normalizeDocumentType(type)

            if (cleanType.isBlank()) return@launch

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
        if (documentId <= 0) return

        viewModelScope.launch {
            scheduler.cancel(documentId)
            documentDao.deleteById(documentId)
        }
    }

    fun updateDocumentExpiry(
        documentId: Int,
        expiryDateMillis: Long
    ) {
        if (documentId <= 0 || expiryDateMillis <= 0L) return

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
        if (documentId <= 0) return

        viewModelScope.launch {
            documentDao.markManuallyNotified(documentId)
        }
    }

    // Validare telefon internationala - accepta orice numar cu 8-15 cifre
    private fun isValidPhone(phone: String): Boolean {
        val digits = phone.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("+", "")
            .filter { it.isDigit() }

        return digits.length in 8..15
    }

    // Validare email permisiva - orice TLD de minimum 2 caractere
    private fun isValidEmail(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()

        if (cleanEmail.isBlank()) return false

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) return false

        val domain = cleanEmail.substringAfter("@", missingDelimiterValue = "")
        val tld = domain.substringAfterLast(".", missingDelimiterValue = "")

        if (domain.isBlank() || tld.length < 2) return false

        if (domain.contains("..")) return false

        // Blocheaza doar typo-uri evidente
        val blockedTlds = setOf("xom", "con", "comm", "cim", "vom", "gmai", "gmial")
        if (tld in blockedTlds) return false

        return true
    }

    private fun isUniqueConstraintError(error: Throwable): Boolean {
        val message = error.message.orEmpty()

        return message.contains("UNIQUE", ignoreCase = true) ||
                message.contains("constraint", ignoreCase = true) ||
                message.contains("index_cars_plate", ignoreCase = true) ||
                message.contains("cars.plate", ignoreCase = true)
    }

    override fun onCleared() {
        super.onCleared()
        appPlanManager.billingManager.destroy()
    }
}
