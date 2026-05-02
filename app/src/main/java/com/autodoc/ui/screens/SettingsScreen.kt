package com.autodoc.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var showConfirmDialog by remember { mutableStateOf(false) }

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
            onUpgradeClick = {
                if (!isProPlan) {
                    showConfirmDialog = true
                }
            }
        )

        BackupSettingsCard(
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup
        )

        AppInfoCard()
    }

    if (showConfirmDialog) {
        ConfirmProDialog(
            onConfirm = {
                onToggleProPlan(true)
                showConfirmDialog = false
            },
            onDismiss = {
                showConfirmDialog = false
            }
        )
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
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )

            Text(
                text = "Plan aplicatie, backup si informatii.",
                color = AppColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlanSettingsCard(
    isProPlan: Boolean,
    onUpgradeClick: () -> Unit
) {
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

            if (isProPlan) {

                Text(
                    text = "Plan Pro activ",
                    color = AppColors.Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Ai acces la masini nelimitate.",
                    color = AppColors.MutedText
                )

            } else {

                Text(
                    text = "Plan Free activ",
                    color = AppColors.Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Limita: 3 masini.",
                    color = AppColors.MutedText
                )

                Button(
                    onClick = onUpgradeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Activeaza Pro — 9.99 €",
                        color = AppColors.Navy,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmProDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Activeaza Pro", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Deblochezi masini nelimitate pentru o plata unica de 9.99 €.\n\nContinui?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold)
            ) {
                Text("Da, activeaza", color = AppColors.Navy)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
            ) {
                Text("Anuleaza", color = Color.White)
            }
        }
    )
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Backup date", color = Color.White, fontWeight = FontWeight.Bold)

            Button(onClick = onExportBackup) {
                Text("Export backup")
            }

            Button(onClick = onImportBackup) {
                Text("Import backup")
            }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("AutoDoc", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Versiune 1.0", color = AppColors.SoftText)
        }
    }
}