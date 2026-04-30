package com.autodoc.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentSeverity
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.documentStatusText
import com.autodoc.ui.formatDate
import com.autodoc.ui.parseDate
import com.autodoc.ui.parseDateToMillis
import com.autodoc.ui.severity
import com.autodoc.ui.shouldNotifyClient
import java.time.LocalDate

@Composable
fun DocumentRow(
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
        ConfirmDeleteDocumentDialog(
            title = "Stergere document",
            message = "Sigur vrei sa stergi documentul ${document.type}?",
            onConfirm = {
                showDeleteDocumentDialog.value = false
                onDeleteDocument(document.id)
            },
            onDismiss = {
                showDeleteDocumentDialog.value = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${document.type} - ${documentStatusText(document.daysLeft)}",
            color = statusColor(document),
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Data expirarii: ${formatDate(document.expiryDateMillis)}",
            color = AppColors.MutedText
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    showEdit.value = !showEdit.value
                    editDateError.value = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Editeaza",
                    color = AppColors.Navy,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    showDeleteDocumentDialog.value = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Sterge",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = {
                sendWhatsAppNotification(context, car, document)
            },
            enabled = canNotify,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canNotify) AppColors.Ok else Color(0xFF6B7280),
                disabledContainerColor = Color(0xFF6B7280)
            ),
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
                Text(
                    text = editDateError.value,
                    color = AppColors.Danger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            DatePickerDarkField(
                value = newDate.value,
                onChange = { newDate.value = it },
                label = "Data noua"
            )

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
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Ok),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Salveaza data noua",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDocumentDialog(
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

private fun statusColor(document: DocumentUi): Color {
    return when (document.severity()) {
        DocumentSeverity.EXPIRED -> AppColors.Danger
        DocumentSeverity.CRITICAL -> AppColors.Danger
        DocumentSeverity.SOON -> AppColors.Warning
        DocumentSeverity.OK -> AppColors.Ok
    }
}

private fun sendWhatsAppNotification(
    context: Context,
    car: CarUi,
    document: DocumentUi
) {
    val phone = normalizePhoneForWhatsApp(car.ownerPhone)

    if (phone.isBlank()) {
        return
    }

    val message = buildWhatsAppMessage(car, document)
    val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")

    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

private fun buildWhatsAppMessage(
    car: CarUi,
    document: DocumentUi
): String {
    val greeting = if (car.ownerName.isNotBlank()) {
        "Buna ziua, ${car.ownerName},"
    } else {
        "Buna ziua,"
    }

    val expiryDate = formatDate(document.expiryDateMillis)
    val status = documentStatusText(document.daysLeft)

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