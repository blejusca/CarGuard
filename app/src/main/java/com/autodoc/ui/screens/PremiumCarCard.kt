package com.autodoc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentSeverity
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.documentStatusText
import com.autodoc.ui.severity
import com.autodoc.ui.validators.validateCarInput

@Composable
fun PremiumCarCard(
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
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
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
                color = AppColors.MutedText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${car.year} • ${car.engine.ifBlank { "Motorizare nespecificata" }}",
                color = AppColors.SoftText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (hasOwnerInfo(car)) {
                Divider(color = AppColors.Border)

                Text(
                    text = "Client: ${car.ownerName.ifBlank { "Nespecificat" }}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (car.ownerPhone.isNotBlank()) {
                    Text(
                        text = "Telefon: ${car.ownerPhone}",
                        color = AppColors.MutedText,
                        fontSize = 14.sp
                    )
                }

                if (car.ownerEmail.isNotBlank()) {
                    Text(
                        text = "Email: ${car.ownerEmail}",
                        color = AppColors.MutedText,
                        fontSize = 14.sp
                    )
                }
            }

            DocumentSummary(
                documentsCount = car.documents.size,
                mostUrgent = mostUrgent
            )

            CarActionButtons(
                isEditing = showEditCar.value,
                onToggleEdit = {
                    showEditCar.value = !showEditCar.value
                },
                onExportPdf = {
                    onExportCarPdf(car)
                }
            )

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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Navy),
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
                ExpandedCarContent(
                    car = car,
                    onAddDocument = onAddDocument,
                    onDeleteDocument = onDeleteDocument,
                    onUpdateDocumentExpiry = onUpdateDocumentExpiry,
                    onRequestDeleteCar = {
                        showDeleteCarDialog.value = true
                    }
                )
            }
        }
    }
}

@Composable
private fun DocumentSummary(
    documentsCount: Int,
    mostUrgent: DocumentUi?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "$documentsCount ${if (documentsCount == 1) "document" else "documente"}",
            color = AppColors.Gold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = mostUrgent?.let {
                "${it.type}: ${documentStatusText(it.daysLeft)}"
            } ?: "Fara documente",
            color = mostUrgent?.let { statusColor(it) } ?: AppColors.MutedText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun CarActionButtons(
    isEditing: Boolean,
    onToggleEdit: () -> Unit,
    onExportPdf: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onToggleEdit,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
        ) {
            Text(
                text = if (isEditing) "Inchide" else "Editeaza",
                color = AppColors.Navy,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        Button(
            onClick = onExportPdf,
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
}

@Composable
private fun ExpandedCarContent(
    car: CarUi,
    onAddDocument: (
        carId: Int,
        type: String,
        expiryDateMillis: Long,
        reminderDaysBefore: Int
    ) -> Unit,
    onDeleteDocument: (documentId: Int) -> Unit,
    onUpdateDocumentExpiry: (documentId: Int, expiryDateMillis: Long) -> Unit,
    onRequestDeleteCar: () -> Unit
) {
    Divider(color = AppColors.Border)

    if (car.ownerNotes.isNotBlank()) {
        Text(
            text = "Observatii client:",
            color = AppColors.Gold,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = car.ownerNotes,
            color = AppColors.MutedText
        )

        Divider(color = AppColors.Border)
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

    Divider(color = AppColors.Border)

    Button(
        onClick = onRequestDeleteCar,
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
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
                color = AppColors.Danger,
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
            color = AppColors.Gold,
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Ok),
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.CardBg),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppColors.Border),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Anuleaza",
                    color = AppColors.Gold,
                    fontWeight = FontWeight.Bold
                )
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
            focusedLabelColor = AppColors.Gold,
            unfocusedLabelColor = AppColors.SoftText,
            focusedBorderColor = AppColors.Border,
            unfocusedBorderColor = AppColors.Border,
            cursorColor = AppColors.Gold,
            focusedContainerColor = AppColors.FieldBg,
            unfocusedContainerColor = AppColors.FieldBg
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
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold)
            ) {
                Text(
                    text = "Anuleaza",
                    color = AppColors.Navy
                )
            }
        }
    )
}

private fun hasOwnerInfo(car: CarUi): Boolean {
    return car.ownerName.isNotBlank() ||
            car.ownerPhone.isNotBlank() ||
            car.ownerEmail.isNotBlank()
}

private fun statusColor(document: DocumentUi): Color {
    return when (document.severity()) {
        DocumentSeverity.EXPIRED -> AppColors.Danger
        DocumentSeverity.CRITICAL -> AppColors.Danger
        DocumentSeverity.SOON -> AppColors.Warning
        DocumentSeverity.OK -> AppColors.Ok
    }
}