package com.autodoc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentSeverity
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.documentStatusText
import com.autodoc.ui.severity
import java.time.LocalDate

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

private val DeepBg = AppColors.DeepBg
private val Navy = AppColors.Navy
private val CardBg = AppColors.CardBg
private val Border = AppColors.Border
private val Gold = AppColors.Gold
private val Danger = AppColors.Danger
private val Warning = AppColors.Warning
private val Ok = AppColors.Ok
private val MutedText = AppColors.MutedText
private val SoftText = AppColors.SoftText
private val FieldBg = AppColors.FieldBg

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
                DashboardSort.MARCA ->
                    compareBy<CarUi> { it.brand.lowercase() }.thenBy { it.model.lowercase() }

                DashboardSort.DOCUMENTE ->
                    compareByDescending { it.documents.size }

                DashboardSort.URGENTE ->
                    compareBy { car -> car.documents.minOfOrNull { it.daysLeft } ?: Int.MAX_VALUE }
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
                    onAddCar(
                        brand,
                        model,
                        plate,
                        year,
                        engine,
                        ownerName,
                        ownerPhone,
                        ownerEmail,
                        ownerNotes
                    )
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
                Text(
                    text = "◆",
                    color = Gold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
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
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(FieldBg)
                .border(BorderStroke(1.dp, Color.Transparent), RoundedCornerShape(28.dp)),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(22.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = "Cauta masina sau client",
                                color = SoftText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        innerTextField()
                    }
                }
            }
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
                Text(
                    text = "Cauta",
                    color = Navy,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
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
                Text(
                    text = "Reseteaza",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            title = "Exp",
            value = expiredCount.toString(),
            color = Danger,
            selected = activeFilter == DashboardFilter.EXPIRED,
            modifier = Modifier
                .weight(1f)
                .clickable { onFilterChange(DashboardFilter.EXPIRED) }
        )
        SummaryCard(
            title = "Cur",
            value = soonCount.toString(),
            color = Warning,
            selected = activeFilter == DashboardFilter.SOON,
            modifier = Modifier
                .weight(1f)
                .clickable { onFilterChange(DashboardFilter.SOON) }
        )
        SummaryCard(
            title = "OK",
            value = okCount.toString(),
            color = Ok,
            selected = activeFilter == DashboardFilter.OK,
            modifier = Modifier
                .weight(1f)
                .clickable { onFilterChange(DashboardFilter.OK) }
        )
        SummaryCard(
            title = "Toate",
            value = totalDocuments.toString(),
            color = Gold,
            selected = activeFilter == DashboardFilter.ALL,
            modifier = Modifier
                .weight(1f)
                .clickable { onFilterChange(DashboardFilter.ALL) }
        )
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
        modifier = modifier.height(64.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Navy else CardBg),
        border = BorderStroke(1.dp, if (selected) Gold else Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = title,
                color = SoftText,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SortButtons(
    activeSort: DashboardSort,
    onSortChange: (DashboardSort) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Sortare masini",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortButton(
                text = "Prioritate",
                selected = activeSort == DashboardSort.URGENTE,
                modifier = Modifier.weight(1f)
            ) {
                onSortChange(DashboardSort.URGENTE)
            }

            SortButton(
                text = "Marca",
                selected = activeSort == DashboardSort.MARCA,
                modifier = Modifier.weight(1f)
            ) {
                onSortChange(DashboardSort.MARCA)
            }

            SortButton(
                text = "Nr. doc",
                selected = activeSort == DashboardSort.DOCUMENTE,
                modifier = Modifier.weight(1f)
            ) {
                onSortChange(DashboardSort.DOCUMENTE)
            }
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
            Text(
                text = "▣",
                color = SoftText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
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
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

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
            Text(
                text = errorMessage.value,
                color = Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
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
                val validationError = validateCarInput(
                    brand = brand.value,
                    model = model.value,
                    plate = plate.value,
                    yearText = year.value,
                    engine = engine.value,
                    ownerName = ownerName.value,
                    ownerPhone = ownerPhone.value,
                    ownerEmail = ownerEmail.value
                )

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
            Text(
                text = "Salveaza masina",
                color = Navy,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
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
    val focusManager = LocalFocusManager.current

    LaunchedEffect(car.id) {
        focusManager.clearFocus()
    }

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
        Text(
            text = "Editare masina / client",
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = 20.sp
        )

        if (errorMessage.value.isNotBlank()) {
            Text(
                text = errorMessage.value,
                color = Danger,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        LightField(brand.value, { brand.value = it }, "Marca")
        LightField(model.value, { model.value = it }, "Model")
        LightField(plate.value, { plate.value = it.uppercase() }, "Numar inmatriculare")
        LightField(year.value, { year.value = it.filter { c -> c.isDigit() } }, "An fabricatie")
        LightField(engine.value, { engine.value = it }, "Motorizare")

        Text(
            text = "Date client / proprietar",
            color = Gold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        LightField(ownerName.value, { ownerName.value = it }, "Nume client / proprietar")
        LightField(ownerPhone.value, { ownerPhone.value = it }, "Telefon client")
        LightField(ownerEmail.value, { ownerEmail.value = it }, "Email client")
        LightField(ownerNotes.value, { ownerNotes.value = it }, "Observatii client")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val validationError = validateCarInput(
                        brand = brand.value,
                        model = model.value,
                        plate = plate.value,
                        yearText = year.value,
                        engine = engine.value,
                        ownerName = ownerName.value,
                        ownerPhone = ownerPhone.value,
                        ownerEmail = ownerEmail.value
                    )

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
                Text(
                    text = "Salveaza",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Anuleaza",
                    color = Gold,
                    fontWeight = FontWeight.Bold
                )
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
            onDismiss = {
                showDeleteCarDialog.value = false
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${car.brand} ${car.model}",
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = car.plate,
                color = MutedText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${car.year} • ${car.engine.ifBlank { "Motorizare nespecificata" }}",
                color = SoftText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (car.ownerName.isNotBlank() || car.ownerPhone.isNotBlank() || car.ownerEmail.isNotBlank()) {
                Divider(color = Border)

                Text(
                    text = "Client: ${car.ownerName.ifBlank { "Nespecificat" }}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (car.ownerPhone.isNotBlank()) {
                    Text(
                        text = "Telefon: ${car.ownerPhone}",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                }

                if (car.ownerEmail.isNotBlank()) {
                    Text(
                        text = "Email: ${car.ownerEmail}",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${car.documents.size} ${if (car.documents.size == 1) "document" else "documente"}",
                    color = Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mostUrgent?.let {
                        "${it.type}: ${documentStatusText(it.daysLeft)}"
                    } ?: "Fara documente",
                    color = mostUrgent?.let { statusColor(it) } ?: MutedText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showEditCar.value = !showEditCar.value },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(
                        text = if (showEditCar.value) "Inchide" else "Editeaza",
                        color = Navy,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = { onExportCarPdf(car) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text(
                        text = "Export PDF",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (showEditCar.value) {
                EditCarForm(
                    car = car,
                    onCancel = {
                        showEditCar.value = false
                    },
                    onSave = { brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes ->
                        onUpdateCar(
                            car.id,
                            brand,
                            model,
                            plate,
                            year,
                            engine,
                            ownerName,
                            ownerPhone,
                            ownerEmail,
                            ownerNotes
                        )
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
                Text(
                    text = if (expanded) "Ascunde documente" else "Arata documente",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expanded) {
                Divider(color = Border)

                if (car.ownerNotes.isNotBlank()) {
                    Text(
                        text = "Observatii client:",
                        color = Gold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = car.ownerNotes,
                        color = MutedText
                    )
                    Divider(color = Border)
                }

                if (car.documents.isEmpty()) {
                    Text(
                        text = "Nu exista documente adaugate.",
                        color = Color.White
                    )
                } else {
                    car.documents.forEach { document ->
                        DocumentRow(
                            car = car,
                            document = document,
                            onDeleteDocument = onDeleteDocument,
                            onUpdateDocumentExpiry = onUpdateDocumentExpiry
                        )
                    }
                }

                AddDocumentForm(
                    carId = car.id,
                    existingDocuments = car.documents,
                    onAddDocument = onAddDocument
                )

                Divider(color = Border)

                Button(
                    onClick = {
                        showDeleteCarDialog.value = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Sterge masina",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
        label = {
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                fontSize = 13.sp
            )
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = Color.White,
            fontSize = 14.sp
        ),
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
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg
        )
    )
}

@Composable
private fun PremiumLightCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Danger)
            ) {
                Text(
                    text = "Da, sterge",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Gold)
            ) {
                Text(
                    text = "Anuleaza",
                    color = Navy
                )
            }
        }
    )
}

private fun validateCarInput(
    brand: String,
    model: String,
    plate: String,
    yearText: String,
    engine: String,
    ownerName: String,
    ownerPhone: String,
    ownerEmail: String
): String {
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

    if (cleanBrand.isBlank()) {
        errors.add("Marca este obligatorie.")
    } else if (!isValidBrand(cleanBrand)) {
        errors.add("Marca nu este valida. Foloseste doar litere, spatii sau cratima, maximum 25 caractere.")
    }

    if (cleanModel.isBlank()) {
        errors.add("Modelul este obligatoriu.")
    } else if (!isValidModel(cleanModel)) {
        errors.add("Modelul nu este valid. Foloseste litere, cifre, spatii sau cratima, maximum 25 caractere.")
    }

    if (cleanPlate.isBlank()) {
        errors.add("Numarul de inmatriculare este obligatoriu.")
    } else if (!isValidPlate(cleanPlate)) {
        errors.add("Numarul de inmatriculare nu este valid. Foloseste 3-12 caractere: litere, cifre, spatii sau cratima.")
    }

    if (cleanYear.isBlank()) {
        errors.add("Anul fabricatiei este obligatoriu.")
    } else if (parsedYear == null) {
        errors.add("Anul fabricatiei trebuie sa fie numeric.")
    } else if (parsedYear !in 1950..maxAllowedYear) {
        errors.add("Anul fabricatiei trebuie sa fie intre 1950 si $maxAllowedYear.")
    }

    if (cleanEngine.isNotBlank() && !isValidEngine(cleanEngine)) {
        errors.add("Motorizarea nu este valida. Exemple acceptate: 1.6 TDI, 2.0 benzina, electric, hybrid.")
    }

    if (cleanOwnerName.isNotBlank() && !isReasonableName(cleanOwnerName)) {
        errors.add("Numele clientului nu este valid. Foloseste minimum 2 caractere si fara cifre.")
    }

    if (cleanOwnerPhone.isNotBlank() && !isValidPhone(cleanOwnerPhone)) {
        errors.add("Telefonul clientului nu este valid. Trebuie sa contina intre 8 si 15 cifre.")
    }

    if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) {
        errors.add("Emailul clientului nu este valid sau pare scris gresit.")
    }

    return errors.joinToString(separator = "\n")
}

private fun isValidBrand(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž -]{2,25}$""").matches(value.trim())
}

private fun isValidModel(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž0-9 -]{1,25}$""").matches(value.trim())
}

private fun isValidPlate(value: String): Boolean {
    return Regex("""^[A-Z0-9 -]{3,12}$""").matches(value.trim().uppercase())
}

private fun isValidEngine(value: String): Boolean {
    val cleanValue = value.trim().lowercase()

    if (cleanValue in listOf("electric", "hibrid", "hybrid", "benzina", "diesel", "gpl", "gaz")) {
        return true
    }

    val pattern = Regex("""^[0-9](\.[0-9])? ?[A-Za-zÀ-ž0-9 -]{0,15}$""")

    return pattern.matches(cleanValue) && cleanValue.length <= 20
}

private fun isValidPhone(value: String): Boolean {
    return value.filter { it.isDigit() }.length in 8..15
}

private fun isValidEmail(value: String): Boolean {
    val email = value.trim().lowercase()
    val emailPattern = Regex("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")

    if (!emailPattern.matches(email)) {
        return false
    }

    val suspiciousParts = listOf(
        "..",
        ".,",
        ",.",
        ".coom",
        ".comm",
        "@yaoo.",
        "@yahooo.",
        "@yahho.",
        "@gmai.",
        "@gmial.",
        "@hotmial.",
        "@outlok."
    )

    return suspiciousParts.none { email.contains(it) }
}

private fun isReasonableName(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž -]{2,40}$""").matches(value.trim())
}

private fun statusColor(document: DocumentUi): Color {
    return when (document.severity()) {
        DocumentSeverity.EXPIRED -> Danger
        DocumentSeverity.CRITICAL -> Danger
        DocumentSeverity.SOON -> Warning
        DocumentSeverity.OK -> Ok
    }
}
