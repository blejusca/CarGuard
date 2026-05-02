package com.autodoc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autodoc.ui.AppColors

@Composable
fun SettingsScreen(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    isProPlan: Boolean,
    onToggleProPlan: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DeepBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsHeaderCard()

        PlanSettingsCard(
            isProPlan = isProPlan,
            onToggleProPlan = onToggleProPlan
        )

        BackupSettingsCard(
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup
        )

        AppInfoCard()
    }
}

@Composable
private fun SettingsHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Navy),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Setari",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Plan aplicatie, backup, restaurare si informatii aplicatie.",
                color = AppColors.Gold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlanSettingsCard(
    isProPlan: Boolean,
    onToggleProPlan: (Boolean) -> Unit
) {
    val planTitle = if (isProPlan) "Plan Pro activ" else "Plan Free activ"

    val planDescription = if (isProPlan) {
        "Ai acces la masini nelimitate pe acest dispozitiv. Planul Pro este activ local."
    } else {
        "Planul Free permite maximum 3 masini. Planul Pro deblocheaza masini nelimitate."
    }

    val buttonText = if (isProPlan) "Revino la Free" else "Activeaza Pro local"
    val buttonColor = if (isProPlan) AppColors.Danger else AppColors.Gold
    val buttonTextColor = if (isProPlan) Color.White else AppColors.Navy

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, if (isProPlan) AppColors.Gold else AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Plan aplicatie",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = planTitle,
                color = AppColors.Gold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = planDescription,
                color = AppColors.MutedText,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            Button(
                onClick = { onToggleProPlan(!isProPlan) },
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = buttonText,
                    color = buttonTextColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "Nota: activarea Pro este locala pentru testare. Google Billing se poate integra ulterior.",
                color = AppColors.SoftText,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun BackupSettingsCard(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Backup date",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "Backup automat activ: aplicatia salveaza periodic un backup JSON in spatiul intern al aplicatiei.",
                color = AppColors.Gold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Backup-ul automat nu suprascrie backup-ul manual salvat in Downloads.",
                color = AppColors.SoftText,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )

            Text(
                text = "Exporta sau importa manual datele aplicatiei: masini, clienti si documente.",
                color = AppColors.MutedText,
                fontSize = 16.sp,
                lineHeight = 21.sp
            )

            Button(
                onClick = onExportBackup,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Export backup manual",
                    color = AppColors.Navy,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            Button(
                onClick = onImportBackup,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Import backup",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "Atentie: importul inlocuieste datele existente cu datele din fisierul backup. Aplicatia creeaza backup de siguranta inainte de import.",
                color = AppColors.Gold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBg),
        border = BorderStroke(1.dp, AppColors.Border),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Info aplicatie",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "AutoDoc / CarGuard Business",
                color = AppColors.Gold,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Aplicatie pentru gestionarea masinilor, clientilor si documentelor auto.",
                color = AppColors.MutedText,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "Functionalitati active:",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "• evidenta masini si clienti\n• documente auto: ITP, RCA, CASCO, Rovinieta, Revizie\n• notificare client prin WhatsApp\n• status persistent: Notificat\n• export raport PDF pentru masina\n• backup manual si backup automat\n• restaurare date din fisier backup\n• plan Free / Pro local",
                color = AppColors.SoftText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "Versiune: 1.0",
                color = AppColors.MutedText,
                fontSize = 13.sp
            )

            Text(
                text = "© 2026 CarGuard Business",
                color = AppColors.SoftText,
                fontSize = 12.sp
            )
        }
    }
}