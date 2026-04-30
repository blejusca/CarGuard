package com.autodoc.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentSeverity
import com.autodoc.ui.DocumentUi
import com.autodoc.ui.isManuallyNotified
import com.autodoc.ui.severity
import com.autodoc.ui.shouldNotifyClient
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

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

private enum class DocumentFilter {
    ALL, URGENT, EXPIRED, SOON, OK
}

private data class DocumentWithCar(
    val car: CarUi,
    val document: DocumentUi
)

@Composable
fun DocumentsScreen(
    cars: List<CarUi>,
    onMarkDocumentManuallyNotified: (documentId: Int) -> Unit = {}
) {
    val activeFilter = remember { mutableStateOf(DocumentFilter.URGENT) }
    val notifyIndex = remember { mutableStateOf(0) }
    val context = LocalContext.current

    val allDocuments = cars.flatMap { car ->
        car.documents.map { document -> DocumentWithCar(car, document) }
    }

    val expiredCount = allDocuments.count { it.document.severity() == DocumentSeverity.EXPIRED }
    val urgentCount = allDocuments.count { it.document.severity() == DocumentSeverity.CRITICAL }
    val soonCount = allDocuments.count { it.document.severity() == DocumentSeverity.SOON }
    val okCount = allDocuments.count { it.document.severity() == DocumentSeverity.OK }

    val clientsToNotifyCount = allDocuments.count { item ->
        val hasContact = item.car.ownerPhone.isNotBlank() || item.car.ownerEmail.isNotBlank()
        item.document.shouldNotifyClient() && hasContact && !item.document.isManuallyNotified()
    }

    val filteredDocuments = allDocuments
        .filter { item ->
            when (activeFilter.value) {
                DocumentFilter.ALL -> true
                DocumentFilter.URGENT -> item.document.severity() == DocumentSeverity.CRITICAL
                DocumentFilter.EXPIRED -> item.document.severity() == DocumentSeverity.EXPIRED
                DocumentFilter.SOON -> item.document.severity() == DocumentSeverity.SOON
                DocumentFilter.OK -> item.document.severity() == DocumentSeverity.OK
            }
        }
        .sortedBy { it.document.daysLeft }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeaderDocuments(
            totalDocuments = allDocuments.size,
            urgentDocuments = urgentCount,
            expiredDocuments = expiredCount,
            soonDocuments = soonCount,
            okDocuments = okCount,
            clientsToNotify = clientsToNotifyCount
        )

        FilterButtons(
            activeFilter = activeFilter.value,
            onFilterChange = { activeFilter.value = it }
        )

        TextButton(
            onClick = {
                val documentsToNotify = allDocuments
                    .filter { item ->
                        val hasContact = item.car.ownerPhone.isNotBlank() || item.car.ownerEmail.isNotBlank()
                        item.document.shouldNotifyClient() && hasContact && !item.document.isManuallyNotified()
                    }
                    .sortedBy { it.document.daysLeft }

                if (documentsToNotify.isNotEmpty()) {
                    val safeIndex = notifyIndex.value % documentsToNotify.size
                    val item = documentsToNotify[safeIndex]

                    notifyClient(context, item.car, item.document)
                    onMarkDocumentManuallyNotified(item.document.id)
                    notifyIndex.value = safeIndex + 1

                    showNotificationToast(context, item.car, item.document)
                }
            },
            enabled = clientsToNotifyCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = if (clientsToNotifyCount > 0) Ok else Color(0xFF4B5563),
                disabledContainerColor = Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (clientsToNotifyCount > 0) {
                    "Notifica urmatorul ($clientsToNotifyCount ramasi)"
                } else {
                    "Nu exista clienti de notificat"
                },
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
                softWrap = false
            )
        }

        if (filteredDocuments.isEmpty()) {
            EmptyDocumentsCard()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredDocuments) { item ->
                    DocumentItem(
                        car = item.car,
                        document = item.document,
                        isNotified = item.document.isManuallyNotified(),
                        onNotified = {
                            notifyClient(context, item.car, item.document)
                            onMarkDocumentManuallyNotified(item.document.id)
                            showNotificationToast(context, item.car, item.document)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderDocuments(
    totalDocuments: Int,
    urgentDocuments: Int,
    expiredDocuments: Int,
    soonDocuments: Int,
    okDocuments: Int,
    clientsToNotify: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    Text(text = "▤", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Documente",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = "$urgentDocuments urgente din $totalDocuments documente",
                    color = Gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryBox("Exp", expiredDocuments.toString(), Danger, Modifier.weight(1f))
            SummaryBox("Urg", urgentDocuments.toString(), Danger, Modifier.weight(1f))
            SummaryBox("Cur", soonDocuments.toString(), Warning, Modifier.weight(1f))
            SummaryBox("Notif", clientsToNotify.toString(), Gold, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(68.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                text = title,
                color = SoftText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilterButtons(
    activeFilter: DocumentFilter,
    onFilterChange: (DocumentFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Filtrare documente", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterButton("Urgente", activeFilter == DocumentFilter.URGENT, Modifier.weight(1f)) {
                onFilterChange(DocumentFilter.URGENT)
            }
            FilterButton("Toate", activeFilter == DocumentFilter.ALL, Modifier.weight(1f)) {
                onFilterChange(DocumentFilter.ALL)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterButton("Expirate", activeFilter == DocumentFilter.EXPIRED, Modifier.weight(1f)) {
                onFilterChange(DocumentFilter.EXPIRED)
            }
            FilterButton("Curand", activeFilter == DocumentFilter.SOON, Modifier.weight(1f)) {
                onFilterChange(DocumentFilter.SOON)
            }
            FilterButton("OK", activeFilter == DocumentFilter.OK, Modifier.weight(1f)) {
                onFilterChange(DocumentFilter.OK)
            }
        }
    }
}

@Composable
private fun FilterButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(42.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (selected) Gold else CardBg),
        border = if (selected) null else BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (selected) Navy else SoftText,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyDocumentsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = "Nu exista documente pentru filtrul selectat.",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Schimba filtrul sau adauga documente noi.",
                color = SoftText,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DocumentItem(
    car: CarUi,
    document: DocumentUi,
    isNotified: Boolean,
    onNotified: () -> Unit
) {
    val severity = document.severity()
    val shouldNotify = document.shouldNotifyClient()

    val color = when (severity) {
        DocumentSeverity.EXPIRED -> Danger
        DocumentSeverity.CRITICAL -> Danger
        DocumentSeverity.SOON -> Warning
        DocumentSeverity.OK -> Ok
    }

    val statusLabel = when (severity) {
        DocumentSeverity.EXPIRED -> "EXPIRAT"
        DocumentSeverity.CRITICAL -> "URGENT"
        DocumentSeverity.SOON -> "CURAND"
        DocumentSeverity.OK -> "OK"
    }

    val hasPhone = car.ownerPhone.isNotBlank()
    val hasEmail = car.ownerEmail.isNotBlank()
    val canNotify = shouldNotify && (hasPhone || hasEmail) && !isNotified

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = document.type,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )

                StatusBadge(statusLabel, color)
            }

            if (isNotified) {
                StatusBadge("NOTIFICAT", Gold, darkText = true)
            }

            Text(text = statusText(document), color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "${car.brand} ${car.model}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = car.plate, color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(text = "Data expirarii: ${formatDate(document.expiryDateMillis)}", color = MutedText, fontSize = 14.sp)
            Text(text = "Client: ${car.ownerName.ifBlank { "nespecificat" }}", color = MutedText, fontSize = 14.sp, fontWeight = FontWeight.Medium)

            Text(
                text = when {
                    hasPhone -> "Telefon: ${car.ownerPhone}"
                    hasEmail -> "Email: ${car.ownerEmail}"
                    else -> "Contact lipsa"
                },
                color = if (hasPhone || hasEmail) MutedText else Warning,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            TextButton(
                onClick = onNotified,
                enabled = canNotify,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = when {
                        isNotified -> Gold
                        !shouldNotify -> Color(0xFF4B5563)
                        hasPhone || hasEmail -> Ok
                        else -> Color(0xFF4B5563)
                    },
                    disabledContainerColor = if (isNotified) Gold else Color(0xFF4B5563)
                ),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    text = when {
                        isNotified -> "Notificat"
                        !shouldNotify -> "Nu necesita notificare"
                        hasPhone -> "Notifica prin WhatsApp"
                        hasEmail -> "Trimite email"
                        else -> "Contact lipsa"
                    },
                    color = if (isNotified) Navy else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    darkText: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            color = if (darkText) Navy else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun notifyClient(context: Context, car: CarUi, document: DocumentUi) {
    when {
        car.ownerPhone.isNotBlank() -> sendWhatsAppNotification(context, car, document)
        car.ownerEmail.isNotBlank() -> sendEmailNotification(context, car, document)
    }
}

private fun sendWhatsAppNotification(context: Context, car: CarUi, document: DocumentUi) {
    val phone = normalizePhoneForWhatsApp(car.ownerPhone)
    if (phone.isBlank()) return

    val message = buildClientMessage(car, document)
    val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nu s-a putut deschide WhatsApp.", Toast.LENGTH_LONG).show()
    }
}

private fun sendEmailNotification(context: Context, car: CarUi, document: DocumentUi) {
    val subject = "Notificare document auto - ${document.type}"
    val message = buildClientMessage(car, document)

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:${car.ownerEmail}")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, message)
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nu exista aplicatie de email instalata.", Toast.LENGTH_LONG).show()
    }
}

private fun showNotificationToast(
    context: Context,
    car: CarUi,
    document: DocumentUi
) {
    val clientName = car.ownerName.ifBlank { car.plate }
    Toast.makeText(
        context,
        "Client notificat: $clientName - ${document.type}",
        Toast.LENGTH_LONG
    ).show()
}

private fun buildClientMessage(car: CarUi, document: DocumentUi): String {
    val greeting = if (car.ownerName.isNotBlank()) {
        "Buna ziua, ${car.ownerName},"
    } else {
        "Buna ziua,"
    }

    return """
$greeting

Va contactam in legatura cu documentul auto:

Document: ${document.type}
Masina: ${car.brand} ${car.model}
Numar inmatriculare: ${car.plate}
Data expirarii: ${formatDate(document.expiryDateMillis)}
Status: ${statusText(document)}

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

private fun formatDate(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

private fun statusText(document: DocumentUi): String {
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