package com.autodoc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.autodoc.ui.severity

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
