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
import com.autodoc.ui.formatDate
import com.autodoc.ui.isManuallyNotified
import com.autodoc.ui.severity
import com.autodoc.ui.shouldNotifyClient
import kotlin.math.abs

private enum class DocumentFilter {
    ALL,
    URGENT,
    EXPIRED,
    SOON,
    OK
}

private data class DocumentWithCar(
    val car: CarUi,
    val document: DocumentUi
)

private data class DocumentsStats(
    val totalDocuments: Int,
    val expiredDocuments: Int,
    val urgentDocuments: Int,
    val soonDocuments: Int,
    val okDocuments: Int,
    val clientsToNotify: Int
)

@Composable
fun DocumentsScreen(
    cars: List<CarUi>,
    onMarkDocumentManuallyNotified: (documentId: Int) -> Unit = {}
) {
    val activeFilter = remember { mutableStateOf(DocumentFilter.URGENT) }
    val notifyIndex = remember { mutableStateOf(0) }
    val context = LocalContext.current

    val allDocuments = remember(cars) {
        cars.flatMap { car ->
            car.documents.map { document ->
                DocumentWithCar(car, document)
            }
        }
    }

    val stats = remember(allDocuments) {
        calculateDocumentsStats(allDocuments)
    }

    val filteredDocuments = remember(allDocuments, activeFilter.value) {
        filterDocuments(
            documents = allDocuments,
            filter = activeFilter.value
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DeepBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeaderDocuments(stats = stats)

        NotifyNextButton(
            clientsToNotifyCount = stats.clientsToNotify,
            onClick = {
                val documentsToNotify = allDocuments
                    .filter { it.canNotifyClient() }
                    .sortedBy { it.document.daysLeft }

                if (documentsToNotify.isNotEmpty()) {
                    val safeIndex = notifyIndex.value % documentsToNotify.size
                    val item = documentsToNotify[safeIndex]

                    notifyClient(context, item.car, item.document)
                    onMarkDocumentManuallyNotified(item.document.id)
                    notifyIndex.value = safeIndex + 1

                    showNotificationToast(context, item.car, item.document)
                }
            }
        )

        FilterButtons(
            activeFilter = activeFilter.value,
            resultCount = filteredDocuments.size,
            onFilterChange = {
                activeFilter.value = it
            }
        )

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

private fun calculateDocumentsStats(
    documents: List<DocumentWithCar>
): DocumentsStats {
    return DocumentsStats(
        totalDocuments = documents.size,
        expiredDocuments = documents.count {
            it.document.severity() == DocumentSeverity.EXPIRED
        },
        urgentDocuments = documents.count {
            it.document.severity() == DocumentSeverity.CRITICAL
        },
        soonDocuments = documents.count {
            it.document.severity() == DocumentSeverity.SOON
        },
        okDocuments = documents.count {
            it.document.severity() == DocumentSeverity.OK
        },
        clientsToNotify = documents.count {
            it.canNotifyClient()
        }
    )
}

private fun filterDocuments(
    documents: List<DocumentWithCar>,
    filter: DocumentFilter
): List<DocumentWithCar> {
    return documents
        .filter { item ->
            when (filter) {
                DocumentFilter.ALL -> true
                DocumentFilter.URGENT -> item.document.severity() == DocumentSeverity.CRITICAL
                DocumentFilter.EXPIRED -> item.document.severity() == DocumentSeverity.EXPIRED
                DocumentFilter.SOON -> item.document.severity() == DocumentSeverity.SOON
                DocumentFilter.OK -> item.document.severity() == DocumentSeverity.OK
            }
        }
        .sortedBy { it.document.daysLeft }
}

private fun DocumentWithCar.canNotifyClient(): Boolean {
    val hasContact = car.ownerPhone.isNotBlank() || car.ownerEmail.isNotBlank()

    return document.shouldNotifyClient() &&
            hasContact &&
            !document.isManuallyNotified()
}

@Composable
private fun HeaderDocuments(stats: DocumentsStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.height(50.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
                    border = BorderStroke(1.dp, AppColors.Gold),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▤",
                            color = AppColors.Gold,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Documente",
                        color = Color.White,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )

                    Text(
                        text = "${stats.urgentDocuments} urgente • ${stats.expiredDocuments} expirate • ${stats.totalDocuments} total",
                        color = AppColors.Gold,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryBox("Expirate", stats.expiredDocuments.toString(), AppColors.Danger, Modifier.weight(1f))
                SummaryBox("Urgente", stats.urgentDocuments.toString(), AppColors.Danger, Modifier.weight(1f))
                SummaryBox("Curand", stats.soonDocuments.toString(), AppColors.Warning, Modifier.weight(1f))
                SummaryBox("Notif.", stats.clientsToNotify.toString(), AppColors.Gold, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NotifyNextButton(
    clientsToNotifyCount: Int,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = clientsToNotifyCount > 0,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (clientsToNotifyCount > 0) AppColors.Ok else Color(0xFF4B5563),
            disabledContainerColor = Color(0xFF4B5563)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (clientsToNotifyCount > 0) {
                "Notifica urmatorul client ($clientsToNotifyCount ramasi)"
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
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                text = title,
                color = AppColors.SoftText,
                fontSize = 10.sp,
                lineHeight = 12.sp,
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
    resultCount: Int,
    onFilterChange: (DocumentFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Filtrare documente",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$resultCount rezultate",
                color = AppColors.SoftText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterButton(
                text = "Urgente",
                selected = activeFilter == DocumentFilter.URGENT,
                modifier = Modifier.weight(1f)
            ) {
                onFilterChange(DocumentFilter.URGENT)
            }

            FilterButton(
                text = "Toate",
                selected = activeFilter == DocumentFilter.ALL,
                modifier = Modifier.weight(1f)
            ) {
                onFilterChange(DocumentFilter.ALL)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterButton(
                text = "Expirate",
                selected = activeFilter == DocumentFilter.EXPIRED,
                modifier = Modifier.weight(1f)
            ) {
                onFilterChange(DocumentFilter.EXPIRED)
            }

            FilterButton(
                text = "Curand",
                selected = activeFilter == DocumentFilter.SOON,
                modifier = Modifier.weight(1f)
            ) {
                onFilterChange(DocumentFilter.SOON)
            }

            FilterButton(
                text = "OK",
                selected = activeFilter == DocumentFilter.OK,
                modifier = Modifier.weight(1f)
            ) {
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
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppColors.Gold else AppColors.CardBg
        ),
        border = if (selected) null else BorderStroke(1.dp, AppColors.Border),
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
                color = if (selected) AppColors.Navy else AppColors.SoftText,
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
            .height(104.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nu exista documente pentru filtrul selectat.",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Schimba filtrul sau adauga documente noi.",
                color = AppColors.SoftText,
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
        DocumentSeverity.EXPIRED -> AppColors.Danger
        DocumentSeverity.CRITICAL -> AppColors.Danger
        DocumentSeverity.SOON -> AppColors.Warning
        DocumentSeverity.OK -> AppColors.Ok
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
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = document.type,
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                StatusBadge(statusLabel, color)
            }

            if (isNotified) {
                StatusBadge("NOTIFICAT", AppColors.Gold, darkText = true)
            }

            Text(
                text = statusText(document),
                color = color,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
                border = BorderStroke(1.dp, AppColors.Border),
                shape = RoundedCornerShape(17.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${car.brand} ${car.model}",
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )

                    Text(
                        text = car.plate,
                        color = AppColors.Gold,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )

                    Text(
                        text = "Expira la: ${formatDate(document.expiryDateMillis)}",
                        color = AppColors.MutedText,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )

                    Text(
                        text = "Client: ${car.ownerName.ifBlank { "nespecificat" }}",
                        color = AppColors.MutedText,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = when {
                            hasPhone -> "Telefon: ${car.ownerPhone}"
                            hasEmail -> "Email: ${car.ownerEmail}"
                            else -> "Contact lipsa"
                        },
                        color = if (hasPhone || hasEmail) AppColors.MutedText else AppColors.Warning,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            TextButton(
                onClick = onNotified,
                enabled = canNotify,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = when {
                        isNotified -> AppColors.Gold
                        !shouldNotify -> Color(0xFF4B5563)
                        hasPhone || hasEmail -> AppColors.Ok
                        else -> Color(0xFF4B5563)
                    },
                    disabledContainerColor = if (isNotified) AppColors.Gold else Color(0xFF4B5563)
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
                    color = if (isNotified) AppColors.Navy else Color.White,
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
            color = if (darkText) AppColors.Navy else Color.White,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun notifyClient(
    context: Context,
    car: CarUi,
    document: DocumentUi
) {
    when {
        car.ownerPhone.isNotBlank() -> sendWhatsAppNotification(context, car, document)
        car.ownerEmail.isNotBlank() -> sendEmailNotification(context, car, document)
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

    val message = buildClientMessage(car, document)
    val uri = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Nu s-a putut deschide WhatsApp.", Toast.LENGTH_LONG).show()
    }
}

private fun sendEmailNotification(
    context: Context,
    car: CarUi,
    document: DocumentUi
) {
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

private fun buildClientMessage(
    car: CarUi,
    document: DocumentUi
): String {
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