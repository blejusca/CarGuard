package com.autodoc.ui.screens

import com.autodoc.ui.severity
import com.autodoc.ui.shouldNotifyClient
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentSeverity
import com.autodoc.ui.DocumentUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val DeepBg = Color(0xFF0F1117)
private val Navy = Color(0xFF111827)
private val CardBg = Color(0xFF1F2937)
private val Border = Color(0xFF374151)
private val Gold = Color(0xFFD4AF37)
private val Danger = Color(0xFFDC2626)
private val Warning = Color(0xFFF59E0B)
private val Ok = Color(0xFF22C55E)
private val MutedText = Color(0xFFD1D5DB)
private val SoftText = Color(0xFF9CA3AF)
private val FieldBg = Color(0xFF1F2937)

private enum class DashboardFilter {
    ALL,
    EXPIRED,
    SOON,
    OK
}

private enum class DashboardSort {
    URGENTE,
    MARCA,
    DOCUMENTE
}

@Composable
fun DashboardScreen(
    cars: List<CarUi>,
    onAddCar: (
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    ) -> Unit,
    onUpdateCar: (
        carId: Int,
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    ) -> Unit,
    onAddDocument: (
        carId: Int,
        type: String,
        expiryDateMillis: Long,
        reminderDaysBefore: Int
    ) -> Unit,
    onDeleteDocument: (documentId: Int) -> Unit,
    onUpdateDocumentExpiry: (documentId: Int, expiryDateMillis: Long) -> Unit,
    onDeleteCar: (carId: Int) -> Unit,
    onExportCarPdf: (CarUi) -> Unit
) {
    val showAddCar = remember { mutableStateOf(false) }
    val searchInput = remember { mutableStateOf("") }
    val activeSearch = remember { mutableStateOf("") }
    val activeFilter = remember { mutableStateOf(DashboardFilter.ALL) }
    val activeSort = remember { mutableStateOf(DashboardSort.URGENTE) }
    val expandedCars = remember { mutableStateMapOf<Int, Boolean>() }
    val focusManager = LocalFocusManager.current

    val allDocuments = cars.flatMap { it.documents }
    val expiredCount = allDocuments.count { it.severity() == DocumentSeverity.EXPIRED }
    val soonCount = allDocuments.count { it.severity() == DocumentSeverity.SOON }
    val okCount = allDocuments.count { it.severity() == DocumentSeverity.OK }
    val totalDocuments = allDocuments.size

    val filteredCars = cars
        .filter { car ->
            val query = activeSearch.value.lowercase()

            val matchesSearch =
                query.isBlank() ||
                        car.brand.lowercase().contains(query) ||
                        car.model.lowercase().contains(query) ||
                        car.plate.lowercase().contains(query) ||
                        car.ownerName.lowercase().contains(query) ||
                        car.ownerPhone.lowercase().contains(query) ||
                        car.ownerEmail.lowercase().contains(query)

            val matchesFilter = when (activeFilter.value) {
                DashboardFilter.ALL -> true
                DashboardFilter.EXPIRED -> car.documents.any { it.severity() == DocumentSeverity.EXPIRED }
                DashboardFilter.SOON -> car.documents.any { it.severity() == DocumentSeverity.SOON }
                DashboardFilter.OK -> car.documents.isNotEmpty() && car.documents.all { it.severity() == DocumentSeverity.OK }
            }

            matchesSearch && matchesFilter
        }
        .sortedWith(
            when (activeSort.value) {
                DashboardSort.MARCA -> compareBy<CarUi> { it.brand.lowercase() }.thenBy { it.model.lowercase() }
                DashboardSort.DOCUMENTE -> compareByDescending<CarUi> { it.documents.size }
                DashboardSort.URGENTE -> compareBy { car -> car.documents.minOfOrNull { it.daysLeft } ?: Int.MAX_VALUE }
            }
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Header(carsCount = cars.size)

        SearchBar(
            value = searchInput.value,
            onChange = { searchInput.value = it },
            onSearch = {
                activeSearch.value = searchInput.value
                focusManager.clearFocus()
            },
            onReset = {
                searchInput.value = ""
                activeSearch.value = ""
                activeFilter.value = DashboardFilter.ALL
                activeSort.value = DashboardSort.URGENTE
                focusManager.clearFocus()
            }
        )

        SummaryCards(
            expiredCount = expiredCount,
            soonCount = soonCount,
            okCount = okCount,
            totalDocuments = totalDocuments,
            activeFilter = activeFilter.value,
            onFilterChange = { selectedFilter -> activeFilter.value = selectedFilter }
        )

        SortButtons(
            activeSort = activeSort.value,
            onSortChange = { selectedSort -> activeSort.value = selectedSort }
        )

        Button(
            onClick = { showAddCar.value = !showAddCar.value },
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (showAddCar.value) "Inchide formular" else "+ Adauga masina",
                color = Navy,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        if (showAddCar.value) {
            AddCarForm(
                onAddCar = { brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes ->
                    onAddCar(brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes)
                    searchInput.value = ""
                    activeSearch.value = ""
                    activeFilter.value = DashboardFilter.ALL
                    activeSort.value = DashboardSort.URGENTE
                    showAddCar.value = false
                    focusManager.clearFocus()
                }
            )
        }

        if (filteredCars.isEmpty()) {
            EmptyCarsCard()
        } else {
            filteredCars.forEach { car ->
                PremiumCarCard(
                    car = car,
                    expanded = expandedCars[car.id] == true,
                    onToggle = { expandedCars[car.id] = !(expandedCars[car.id] ?: false) },
                    onUpdateCar = onUpdateCar,
                    onAddDocument = onAddDocument,
                    onDeleteDocument = onDeleteDocument,
                    onUpdateDocumentExpiry = onUpdateDocumentExpiry,
                    onDeleteCar = onDeleteCar,
                    onExportCarPdf = onExportCarPdf
                )
            }
        }
    }
}

@Composable
private fun Header(carsCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.height(48.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "◆", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "CarGuard Business",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                maxLines = 1
            )
            Text(
                text = "$carsCount masini active",
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onChange: (String) -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            placeholder = { Text("Cauta masina sau client", color = SoftText, fontSize = 14.sp, maxLines = 1, softWrap = false) },
            leadingIcon = { Text("⌕", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(26.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Gold,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSearch,
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Cauta", color = Navy, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                shape = RoundedCornerShape(15.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("Reseteaza", color = Gold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SummaryCards(
    expiredCount: Int,
    soonCount: Int,
    okCount: Int,
    totalDocuments: Int,
    activeFilter: DashboardFilter,
    onFilterChange: (DashboardFilter) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard("Exp", expiredCount.toString(), Danger, activeFilter == DashboardFilter.EXPIRED, Modifier.weight(1f).clickable { onFilterChange(DashboardFilter.EXPIRED) })
        SummaryCard("Cur", soonCount.toString(), Warning, activeFilter == DashboardFilter.SOON, Modifier.weight(1f).clickable { onFilterChange(DashboardFilter.SOON) })
        SummaryCard("OK", okCount.toString(), Ok, activeFilter == DashboardFilter.OK, Modifier.weight(1f).clickable { onFilterChange(DashboardFilter.OK) })
        SummaryCard("Toate", totalDocuments.toString(), Gold, activeFilter == DashboardFilter.ALL, Modifier.weight(1f).clickable { onFilterChange(DashboardFilter.ALL) })
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(58.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Navy else CardBg),
        border = BorderStroke(1.dp, if (selected) Gold else Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, color = color, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(text = title, color = SoftText, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun SortButtons(
    activeSort: DashboardSort,
    onSortChange: (DashboardSort) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Sortare masini", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortButton("Prioritate", activeSort == DashboardSort.URGENTE, Modifier.weight(1f)) { onSortChange(DashboardSort.URGENTE) }
            SortButton("Marca", activeSort == DashboardSort.MARCA, Modifier.weight(1f)) { onSortChange(DashboardSort.MARCA) }
            SortButton("Nr. doc", activeSort == DashboardSort.DOCUMENTE, Modifier.weight(1f)) { onSortChange(DashboardSort.DOCUMENTE) }
        }
    }
}

@Composable
private fun SortButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) Gold else Color.Transparent),
        border = BorderStroke(1.dp, Gold),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (selected) Navy else Gold,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun EmptyCarsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "▣", color = SoftText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Nu exista masini pentru filtrul selectat.",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AddCarForm(
    onAddCar: (
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    ) -> Unit
) {
    val brand = remember { mutableStateOf("") }
    val model = remember { mutableStateOf("") }
    val plate = remember { mutableStateOf("") }
    val year = remember { mutableStateOf("") }
    val engine = remember { mutableStateOf("") }
    val ownerName = remember { mutableStateOf("") }
    val ownerPhone = remember { mutableStateOf("") }
    val ownerEmail = remember { mutableStateOf("") }
    val ownerNotes = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }

    PremiumLightCard {
        Text(
            text = "Masina noua",
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = 23.sp
        )

        if (errorMessage.value.isNotBlank()) {
            Text(text = errorMessage.value, color = Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        FormSectionTitle("Date masina")
        LightField(brand.value, { brand.value = it }, "Marca")
        LightField(model.value, { model.value = it }, "Model")
        LightField(plate.value, { plate.value = it.uppercase() }, "Numar inmatriculare")
        LightField(year.value, { year.value = it.filter { c -> c.isDigit() } }, "An fabricatie")
        LightField(engine.value, { engine.value = it }, "Motorizare")

        Spacer(modifier = Modifier.height(2.dp))

        FormSectionTitle("Date client / proprietar")
        LightField(ownerName.value, { ownerName.value = it }, "Nume client / proprietar")
        LightField(ownerPhone.value, { ownerPhone.value = it }, "Telefon client")
        LightField(ownerEmail.value, { ownerEmail.value = it }, "Email client")
        LightField(ownerNotes.value, { ownerNotes.value = it }, "Observatii client")

        Button(
            onClick = {
                val validationError = validateCarInput(brand.value, model.value, plate.value, year.value, engine.value, ownerName.value, ownerPhone.value, ownerEmail.value)
                errorMessage.value = validationError

                if (validationError.isBlank()) {
                    onAddCar(
                        brand.value.trim(),
                        model.value.trim(),
                        plate.value.trim().uppercase(),
                        year.value.trim().toIntOrNull() ?: 0,
                        engine.value.trim(),
                        ownerName.value.trim(),
                        ownerPhone.value.trim(),
                        ownerEmail.value.trim(),
                        ownerNotes.value.trim()
                    )
                    brand.value = ""
                    model.value = ""
                    plate.value = ""
                    year.value = ""
                    engine.value = ""
                    ownerName.value = ""
                    ownerPhone.value = ""
                    ownerEmail.value = ""
                    ownerNotes.value = ""
                    errorMessage.value = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Salveaza masina", color = Navy, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FormSectionTitle(text: String) {
    Text(
        text = text,
        color = Gold,
        fontWeight = FontWeight.Black,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EditCarForm(
    car: CarUi,
    onCancel: () -> Unit,
    onSave: (
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    ) -> Unit
) {
    val brand = remember(car.id) { mutableStateOf(car.brand) }
    val model = remember(car.id) { mutableStateOf(car.model) }
    val plate = remember(car.id) { mutableStateOf(car.plate) }
    val year = remember(car.id) { mutableStateOf(car.year.toString()) }
    val engine = remember(car.id) { mutableStateOf(car.engine) }
    val ownerName = remember(car.id) { mutableStateOf(car.ownerName) }
    val ownerPhone = remember(car.id) { mutableStateOf(car.ownerPhone) }
    val ownerEmail = remember(car.id) { mutableStateOf(car.ownerEmail) }
    val ownerNotes = remember(car.id) { mutableStateOf(car.ownerNotes) }
    val errorMessage = remember(car.id) { mutableStateOf("") }

    PremiumLightCard {
        Text(text = "Editare masina / client", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)

        if (errorMessage.value.isNotBlank()) {
            Text(text = errorMessage.value, color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        LightField(brand.value, { brand.value = it }, "Marca")
        LightField(model.value, { model.value = it }, "Model")
        LightField(plate.value, { plate.value = it.uppercase() }, "Numar inmatriculare")
        LightField(year.value, { year.value = it.filter { c -> c.isDigit() } }, "An fabricatie")
        LightField(engine.value, { engine.value = it }, "Motorizare")

        Text(text = "Date client / proprietar", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        LightField(ownerName.value, { ownerName.value = it }, "Nume client / proprietar")
        LightField(ownerPhone.value, { ownerPhone.value = it }, "Telefon client")
        LightField(ownerEmail.value, { ownerEmail.value = it }, "Email client")
        LightField(ownerNotes.value, { ownerNotes.value = it }, "Observatii client")

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    val validationError = validateCarInput(brand.value, model.value, plate.value, year.value, engine.value, ownerName.value, ownerPhone.value, ownerEmail.value)
                    errorMessage.value = validationError

                    if (validationError.isBlank()) {
                        onSave(
                            brand.value.trim(),
                            model.value.trim(),
                            plate.value.trim().uppercase(),
                            year.value.trim().toIntOrNull() ?: 0,
                            engine.value.trim(),
                            ownerName.value.trim(),
                            ownerPhone.value.trim(),
                            ownerEmail.value.trim(),
                            ownerNotes.value.trim()
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ok),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Salveaza", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.weight(1f)
            ) {
                Text("Anuleaza", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PremiumCarCard(
    car: CarUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onUpdateCar: (
        carId: Int,
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    ) -> Unit,
    onAddDocument: (
        carId: Int,
        type: String,
        expiryDateMillis: Long,
        reminderDaysBefore: Int
    ) -> Unit,
    onDeleteDocument: (documentId: Int) -> Unit,
    onUpdateDocumentExpiry: (documentId: Int, expiryDateMillis: Long) -> Unit,
    onDeleteCar: (carId: Int) -> Unit,
    onExportCarPdf: (CarUi) -> Unit
) {
    val mostUrgent = car.documents.minByOrNull { it.daysLeft }
    val showDeleteCarDialog = remember { mutableStateOf(false) }
    val showEditCar = remember { mutableStateOf(false) }

    if (showDeleteCarDialog.value) {
        ConfirmDeleteDialog(
            title = "Stergere masina",
            message = "Sigur vrei sa stergi masina ${car.brand} ${car.model} si toate documentele asociate?",
            onConfirm = {
                showDeleteCarDialog.value = false
                onDeleteCar(car.id)
            },
            onDismiss = { showDeleteCarDialog.value = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "${car.brand} ${car.model}", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(text = car.plate, color = MutedText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "${car.year} • ${car.engine.ifBlank { "Motorizare nespecificata" }}", color = SoftText, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            if (car.ownerName.isNotBlank() || car.ownerPhone.isNotBlank() || car.ownerEmail.isNotBlank()) {
                Divider(color = Border)
                Text(text = "Client: ${car.ownerName.ifBlank { "Nespecificat" }}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (car.ownerPhone.isNotBlank()) Text(text = "Telefon: ${car.ownerPhone}", color = MutedText, fontSize = 14.sp)
                if (car.ownerEmail.isNotBlank()) Text(text = "Email: ${car.ownerEmail}", color = MutedText, fontSize = 14.sp)
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = "${car.documents.size} ${if (car.documents.size == 1) "document" else "documente"}", color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = mostUrgent?.let { "${it.type}: ${documentStatusText(it)}" } ?: "Fara documente",
                    color = mostUrgent?.let { statusColor(it) } ?: MutedText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showEditCar.value = !showEditCar.value },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(if (showEditCar.value) "Inchide" else "Editeaza", color = Navy, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                Button(
                    onClick = { onExportCarPdf(car) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (showEditCar.value) {
                EditCarForm(
                    car = car,
                    onCancel = { showEditCar.value = false },
                    onSave = { brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes ->
                        onUpdateCar(car.id, brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes)
                        showEditCar.value = false
                    }
                )
            }

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (expanded) "Ascunde documente" else "Arata documente", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (expanded) {
                Divider(color = Border)

                if (car.ownerNotes.isNotBlank()) {
                    Text(text = "Observatii client:", color = Gold, fontWeight = FontWeight.Bold)
                    Text(text = car.ownerNotes, color = MutedText)
                    Divider(color = Border)
                }

                if (car.documents.isEmpty()) {
                    Text(text = "Nu exista documente adaugate.", color = Color.White)
                } else {
                    car.documents.forEach { document ->
                        DocumentRow(car, document, onDeleteDocument, onUpdateDocumentExpiry)
                    }
                }

                AddDocumentForm(car.id, car.documents, onAddDocument)

                Divider(color = Border)

                Button(
                    onClick = { showDeleteCarDialog.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sterge masina", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(
    car: CarUi,
    document: DocumentUi,
    onDeleteDocument: (documentId: Int) -> Unit,
    onUpdateDocumentExpiry: (documentId: Int, expiryDateMillis: Long) -> Unit
) {
    val context = LocalContext.current
    val showEdit = remember { mutableStateOf(false) }
    val newDate = remember { mutableStateOf("") }
    val editDateError = remember { mutableStateOf("") }
    val showDeleteDocumentDialog = remember { mutableStateOf(false) }
    val shouldNotify = document.shouldNotifyClient()
    val hasPhone = car.ownerPhone.isNotBlank()
    val canNotify = shouldNotify && hasPhone

    if (showDeleteDocumentDialog.value) {
        ConfirmDeleteDialog(
            title = "Stergere document",
            message = "Sigur vrei sa stergi documentul ${document.type}?",
            onConfirm = {
                showDeleteDocumentDialog.value = false
                onDeleteDocument(document.id)
            },
            onDismiss = { showDeleteDocumentDialog.value = false }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "${document.type} - ${documentStatusText(document)}", color = statusColor(document), fontWeight = FontWeight.Bold)
        Text(text = "Data expirarii: ${formatDate(document.expiryDateMillis)}", color = MutedText)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    showEdit.value = !showEdit.value
                    editDateError.value = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Editeaza", color = Navy, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showDeleteDocumentDialog.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = Danger),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Sterge", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = { sendWhatsAppNotification(context, car, document) },
            enabled = canNotify,
            colors = ButtonDefaults.buttonColors(containerColor = if (canNotify) Ok else Color(0xFF6B7280), disabledContainerColor = Color(0xFF6B7280)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    !shouldNotify -> "Nu necesita notificare"
                    !hasPhone -> "Telefon client lipsa"
                    else -> "Notifica WhatsApp"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        if (showEdit.value) {
            if (editDateError.value.isNotBlank()) {
                Text(text = editDateError.value, color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            DatePickerDarkField(value = newDate.value, onChange = { newDate.value = it }, label = "Data noua")

            Button(
                onClick = {
                    val validationError = validateExpiryDate(newDate.value)
                    val millis = parseDateToMillis(newDate.value)
                    editDateError.value = validationError

                    if (validationError.isBlank() && millis != null) {
                        onUpdateDocumentExpiry(document.id, millis)
                        newDate.value = ""
                        editDateError.value = ""
                        showEdit.value = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ok),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salveaza data noua", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AddDocumentForm(
    carId: Int,
    existingDocuments: List<DocumentUi>,
    onAddDocument: (
        carId: Int,
        type: String,
        expiryDateMillis: Long,
        reminderDaysBefore: Int
    ) -> Unit
) {
    val availableTypes = listOf("ITP", "RCA", "CASCO", "Rovinieta", "Revizie")
    val existingTypes = existingDocuments.map { normalizeDocumentType(it.type) }.toSet()
    val firstAvailableType = availableTypes.firstOrNull { it !in existingTypes } ?: ""
    val type = remember(carId, existingTypes.size) { mutableStateOf(firstAvailableType) }
    val expiryDateText = remember { mutableStateOf("") }
    val reminderDays = remember { mutableStateOf("7") }
    val errorMessage = remember(carId, existingTypes.size) { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Document nou", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

        if (errorMessage.value.isNotBlank()) {
            Text(text = errorMessage.value, color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (firstAvailableType.isBlank()) {
            Text(
                text = "Toate tipurile standard de documente sunt deja adaugate. Editeaza documentul existent daca vrei sa schimbi data.",
                color = Warning,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallDocButton("ITP", enabled = "ITP" !in existingTypes) { type.value = "ITP" }
            SmallDocButton("RCA", enabled = "RCA" !in existingTypes) { type.value = "RCA" }
            SmallDocButton("CASCO", enabled = "CASCO" !in existingTypes) { type.value = "CASCO" }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallDocButton("Rovinieta", enabled = "Rovinieta" !in existingTypes) { type.value = "Rovinieta" }
            SmallDocButton("Revizie", enabled = "Revizie" !in existingTypes) { type.value = "Revizie" }
        }

        DarkField(type.value, { type.value = normalizeDocumentType(it) }, "Tip document")

        if (normalizeDocumentType(type.value) in existingTypes) {
            Text(text = "Acest document exista deja pentru masina selectata. Editeaza documentul existent.", color = Warning, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        DatePickerDarkField(value = expiryDateText.value, onChange = { expiryDateText.value = it }, label = "Data expirarii")

        DarkField(reminderDays.value, { reminderDays.value = it.filter { c -> c.isDigit() } }, "Notificare cu cate zile inainte")

        Button(
            onClick = {
                val cleanType = normalizeDocumentType(type.value)
                val expiryMillis = parseDateToMillis(expiryDateText.value)
                val validationError = validateDocumentInput(cleanType, expiryDateText.value, reminderDays.value, existingTypes)
                errorMessage.value = validationError

                if (validationError.isBlank() && expiryMillis != null) {
                    onAddDocument(carId, cleanType, expiryMillis, reminderDays.value.toIntOrNull()?.coerceIn(0, 365) ?: 7)
                    val nextType = availableTypes.firstOrNull { it !in existingTypes && it != cleanType } ?: ""
                    type.value = nextType
                    expiryDateText.value = ""
                    reminderDays.value = "7"
                    errorMessage.value = ""
                }
            },
            enabled = normalizeDocumentType(type.value).isNotBlank() && normalizeDocumentType(type.value) !in existingTypes,
            colors = ButtonDefaults.buttonColors(containerColor = Gold, disabledContainerColor = Color(0xFF6B7280)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salveaza document", color = Navy, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmallDocButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Gold, disabledContainerColor = Color(0xFF4B5563)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text = text, color = if (enabled) Navy else MutedText, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LightField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        label = { Text(label, maxLines = 1, softWrap = false, fontSize = 13.sp) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 14.sp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Gold,
            unfocusedLabelColor = SoftText,
            focusedBorderColor = Border,
            unfocusedBorderColor = Border,
            cursorColor = Gold,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg
        )
    )
}

@Composable
private fun DarkField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Gold,
            unfocusedLabelColor = MutedText,
            focusedBorderColor = Border,
            unfocusedBorderColor = Border,
            cursorColor = Gold,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDarkField(value: String, onChange: (String) -> Unit, label: String) {
    val showPicker = remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("Alege data") },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker.value = true },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Gold,
            unfocusedLabelColor = MutedText,
            focusedBorderColor = Border,
            unfocusedBorderColor = Border,
            cursorColor = Gold,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg
        )
    )

    Button(
        onClick = { showPicker.value = true },
        colors = ButtonDefaults.buttonColors(containerColor = Gold),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (value.isBlank()) "Alege data" else "Schimba data: $value", color = Navy, fontWeight = FontWeight.Bold)
    }

    if (showPicker.value) {
        val initialMillis = parseDateToMillis(value)
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) onChange(formatDate(selectedMillis))
                        showPicker.value = false
                    }
                ) { Text("Alege", color = Gold, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showPicker.value = false }) { Text("Anuleaza", color = Navy) } }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun PremiumLightCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun ConfirmDeleteDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Danger)) { Text("Da, sterge", color = Color.White) } },
        dismissButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Gold)) { Text("Anuleaza", color = Navy) } }
    )
}

private fun validateDocumentInput(type: String, expiryDateText: String, reminderDaysText: String, existingTypes: Set<String>): String {
    val errors = mutableListOf<String>()
    val cleanType = normalizeDocumentType(type)
    val allowedTypes = setOf("ITP", "RCA", "CASCO", "Rovinieta", "Revizie")
    val reminderDays = reminderDaysText.trim().toIntOrNull()

    if (cleanType.isBlank()) errors.add("Tipul documentului este obligatoriu.")
    else if (cleanType !in allowedTypes) errors.add("Tipul documentului nu este valid. Alege ITP, RCA, CASCO, Rovinieta sau Revizie.")
    else if (cleanType in existingTypes) errors.add("Acest document exista deja pentru masina selectata.")

    val dateError = validateExpiryDate(expiryDateText)
    if (dateError.isNotBlank()) errors.add(dateError)

    if (reminderDaysText.trim().isBlank()) errors.add("Numarul de zile pentru notificare este obligatoriu.")
    else if (reminderDays == null) errors.add("Numarul de zile pentru notificare trebuie sa fie numeric.")
    else if (reminderDays !in 0..365) errors.add("Notificarea trebuie setata intre 0 si 365 zile inainte.")

    return errors.joinToString(separator = "\n")
}

private fun validateExpiryDate(value: String): String {
    val date = parseDate(value)
    val today = LocalDate.now()
    val maxDate = today.plusYears(10)

    return when {
        value.trim().isBlank() -> "Data expirarii este obligatorie."
        date == null -> "Data expirarii nu este valida. Foloseste selectorul de data sau formatul yyyy-MM-dd."
        date.isBefore(today.minusYears(5)) -> "Data expirarii este prea veche."
        date.isAfter(maxDate) -> "Data expirarii este prea departe in viitor. Maximum 10 ani."
        else -> ""
    }
}

private fun validateCarInput(brand: String, model: String, plate: String, yearText: String, engine: String, ownerName: String, ownerPhone: String, ownerEmail: String): String {
    val errors = mutableListOf<String>()
    val cleanBrand = brand.trim()
    val cleanModel = model.trim()
    val cleanPlate = plate.trim().uppercase()
    val cleanYear = yearText.trim()
    val cleanEngine = engine.trim()
    val cleanOwnerName = ownerName.trim()
    val cleanOwnerPhone = ownerPhone.trim()
    val cleanOwnerEmail = ownerEmail.trim()
    val currentYear = LocalDate.now().year
    val maxAllowedYear = currentYear + 1
    val parsedYear = cleanYear.toIntOrNull()

    if (cleanBrand.isBlank()) errors.add("Marca este obligatorie.")
    else if (!isValidBrand(cleanBrand)) errors.add("Marca nu este valida. Foloseste doar litere, spatii sau cratima, maximum 25 caractere.")

    if (cleanModel.isBlank()) errors.add("Modelul este obligatoriu.")
    else if (!isValidModel(cleanModel)) errors.add("Modelul nu este valid. Foloseste litere, cifre, spatii sau cratima, maximum 25 caractere.")

    if (cleanPlate.isBlank()) errors.add("Numarul de inmatriculare este obligatoriu.")
    else if (!isValidPlate(cleanPlate)) errors.add("Numarul de inmatriculare nu este valid. Foloseste 3-12 caractere: litere, cifre, spatii sau cratima.")

    if (cleanYear.isBlank()) errors.add("Anul fabricatiei este obligatoriu.")
    else if (parsedYear == null) errors.add("Anul fabricatiei trebuie sa fie numeric.")
    else if (parsedYear !in 1950..maxAllowedYear) errors.add("Anul fabricatiei trebuie sa fie intre 1950 si $maxAllowedYear.")

    if (cleanEngine.isNotBlank() && !isValidEngine(cleanEngine)) errors.add("Motorizarea nu este valida. Exemple acceptate: 1.6 TDI, 2.0 benzina, electric, hybrid.")
    if (cleanOwnerName.isNotBlank() && !isReasonableName(cleanOwnerName)) errors.add("Numele clientului nu este valid. Foloseste minimum 2 caractere si fara cifre.")
    if (cleanOwnerPhone.isNotBlank() && !isValidPhone(cleanOwnerPhone)) errors.add("Telefonul clientului nu este valid. Trebuie sa contina intre 8 si 15 cifre.")
    if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) errors.add("Emailul clientului nu este valid sau pare scris gresit.")

    return errors.joinToString(separator = "\n")
}

private fun isValidBrand(value: String): Boolean = Regex("""^[A-Za-zÀ-ž -]{2,25}$""").matches(value.trim())
private fun isValidModel(value: String): Boolean = Regex("""^[A-Za-zÀ-ž0-9 -]{1,25}$""").matches(value.trim())
private fun isValidPlate(value: String): Boolean = Regex("""^[A-Z0-9 -]{3,12}$""").matches(value.trim().uppercase())

private fun isValidEngine(value: String): Boolean {
    val cleanValue = value.trim().lowercase()
    if (cleanValue in listOf("electric", "hibrid", "hybrid", "benzina", "diesel", "gpl", "gaz")) return true
    val pattern = Regex("""^[0-9](\.[0-9])? ?[A-Za-zÀ-ž0-9 -]{0,15}$""")
    return pattern.matches(cleanValue) && cleanValue.length <= 20
}

private fun isValidPhone(value: String): Boolean = value.filter { it.isDigit() }.length in 8..15

private fun isValidEmail(value: String): Boolean {
    val email = value.trim().lowercase()
    val emailPattern = Regex("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")
    if (!emailPattern.matches(email)) return false
    val suspiciousParts = listOf("..", ".,", ",.", ".coom", ".comm", "@yaoo.", "@yahooo.", "@yahho.", "@gmai.", "@gmial.", "@hotmial.", "@outlok.")
    return suspiciousParts.none { email.contains(it) }
}

private fun isReasonableName(value: String): Boolean = Regex("""^[A-Za-zÀ-ž -]{2,40}$""").matches(value.trim())

private fun sendWhatsAppNotification(context: Context, car: CarUi, document: DocumentUi) {
    val phone = normalizePhoneForWhatsApp(car.ownerPhone)
    if (phone.isBlank()) return
    val message = buildWhatsAppMessage(car, document)
    val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

private fun buildWhatsAppMessage(car: CarUi, document: DocumentUi): String {
    val greeting = if (car.ownerName.isNotBlank()) "Buna ziua, ${car.ownerName}," else "Buna ziua,"
    val expiryDate = formatDate(document.expiryDateMillis)
    val status = documentStatusText(document)

    return """
$greeting

Va contactam in legatura cu documentul auto:

Document: ${document.type}
Masina: ${car.brand} ${car.model}
Numar inmatriculare: ${car.plate}
Data expirarii: $expiryDate
Status: $status

Va recomandam sa verificati si sa reinnoiti documentul in timp util, pentru a evita amenzi sau probleme legale.

Multumim,
CarGuard Business
""".trimIndent()
}

private fun normalizePhoneForWhatsApp(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.isBlank() -> ""
        digits.startsWith("00") -> digits.drop(2)
        digits.startsWith("40") -> digits
        digits.startsWith("0") -> "40" + digits.drop(1)
        else -> digits
    }
}

private fun parseDateToMillis(value: String): Long? = parseDate(value)?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

private fun parseDate(value: String): LocalDate? {
    val cleanValue = value.trim().replace("/", "-")
    if (cleanValue.isBlank()) return null
    val supportedFormats = listOf(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("d-M-yyyy"))

    for (formatter in supportedFormats) {
        try {
            return LocalDate.parse(cleanValue, formatter)
        } catch (e: Exception) {
            // Try next format.
        }
    }
    return null
}

private fun formatDate(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

private fun documentStatusText(document: DocumentUi): String {
    return when {
        document.daysLeft < 0 -> {
            val days = abs(document.daysLeft)
            "expirat de $days ${if (days == 1) "zi" else "zile"}"
        }
        document.daysLeft == 0 -> "expira azi"
        document.daysLeft == 1 -> "expira maine"
        else -> "expira in ${document.daysLeft} zile"
    }
}

private fun statusColor(document: DocumentUi): Color {
    return when (document.severity()) {
        DocumentSeverity.EXPIRED -> Danger
        DocumentSeverity.CRITICAL -> Danger
        DocumentSeverity.SOON -> Warning
        DocumentSeverity.OK -> Ok
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

private fun isDocumentUrgent(severity: DocumentSeverity): Boolean {
    return severity == DocumentSeverity.EXPIRED || severity == DocumentSeverity.CRITICAL || severity == DocumentSeverity.SOON
}
