package com.autodoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val Navy = Color(0xFF111827)
private val Gold = Color(0xFFD4AF37)
private val SoftBg = Color(0xFFF5F1EA)
private val CardBg = Color(0xFF1F2937)
private val MutedText = Color(0xFFD1D5DB)
private val Danger = Color(0xFFDC2626)

@Composable
fun SettingsScreen(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Navy),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Setari",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Backup, restaurare si informatii aplicatie",
                    color = Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
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
                    fontWeight = FontWeight.Bold
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
                        .height(56.dp)
                ) {
                    Text(
                        text = "Export backup",
                        color = Navy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Button(
                    onClick = onImportBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Import backup",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Info aplicatie",
                    color = Navy,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "AutoDoc / CarGuard Business",
                    color = Navy,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Aplicatie pentru gestionarea masinilor, documentelor auto si notificarilor de expirare.",
                    color = Color(0xFF4B5563),
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}