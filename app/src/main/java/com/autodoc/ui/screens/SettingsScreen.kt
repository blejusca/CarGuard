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
import com.autodoc.BuildConfig
import com.autodoc.data.AppPlanManager
import com.autodoc.ui.AppColors

@Composable
fun SettingsScreen(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    isProPlan: Boolean,
    onBuyPro: () -> Unit
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
            onBuyPro = onBuyPro
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
    onBuyPro: () -> Unit
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
                    text = "Limita: ${AppPlanManager.FREE_PLAN_MAX_CARS} masini.",
                    color = AppColors.MutedText
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Beneficii Pro
                Text(
                    text = "Ce primesti cu Pro:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(text = "• Masini nelimitate", color = AppColors.MutedText, fontSize = 14.sp)
                Text(text = "• Toate documentele pentru fiecare masina", color = AppColors.MutedText, fontSize = 14.sp)
                Text(text = "• Notificari avansate de expirare", color = AppColors.MutedText, fontSize = 14.sp)
                Text(text = "• Plata o singura data, fara abonament", color = AppColors.Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onBuyPro,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Gold),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Cumpara Pro — plata unica",
                        color = AppColors.Navy,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "Pretul este afisat in Google Play la apasarea butonului. Plata este procesata securizat de Google Play.",
                    color = AppColors.SoftText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
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
            Text("Versiune ${BuildConfig.VERSION_NAME}", color = AppColors.SoftText)
        }
    }
}
