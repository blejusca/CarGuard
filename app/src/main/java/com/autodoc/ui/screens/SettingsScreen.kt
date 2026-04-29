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

private val DeepBg = AppColors.DeepBg
private val Navy = AppColors.Navy
private val Gold = AppColors.Gold
private val CardBg = AppColors.CardBg
private val Border = AppColors.Border
private val MutedText = AppColors.MutedText
private val SoftText = AppColors.SoftText
private val Danger = AppColors.Danger

@Composable
fun SettingsScreen(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Navy),
            border = BorderStroke(1.dp, Border),
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
                    text = "Backup, restaurare si informatii aplicatie",
                    color = Gold,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, Border),
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
                    text = "Exporta sau importa datele aplicatiei: masini, clienti si documente.",
                    color = MutedText,
                    fontSize = 16.sp,
                    lineHeight = 21.sp
                )

                Button(
                    onClick = onExportBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = "Export backup",
                        color = Navy,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onImportBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
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
                    text = "Atentie: importul inlocuieste datele existente cu datele din fisierul backup.",
                    color = Gold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, Border),
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
                    color = Gold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Aplicatie pentru gestionarea masinilor, clientilor si documentelor auto.",
                    color = MutedText,
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
                    text = "• evidenta masini si clienti\n• documente auto: ITP, RCA, CASCO, Rovinieta, Revizie\n• notificari pentru documente expirate sau aproape de expirare\n• notificare client prin WhatsApp sau email\n• export raport PDF pentru masina\n• backup si restaurare date",
                    color = SoftText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Text(
                    text = "Versiune: 1.0",
                    color = MutedText,
                    fontSize = 13.sp
                )

                Text(
                    text = "© 2026 CarGuard Business",
                    color = SoftText,
                    fontSize = 12.sp
                )
            }
        }
    }
}