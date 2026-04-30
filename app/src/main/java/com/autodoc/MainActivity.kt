package com.autodoc

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.autodoc.data.AppPlanManager
import com.autodoc.data.BackupManager
import com.autodoc.data.DatabaseProvider
import com.autodoc.data.PdfExportManager
import com.autodoc.notification.AutoBackupWorker
import com.autodoc.notification.AutoDocNotificationScheduler
import com.autodoc.notification.DailyCheckWorker
import com.autodoc.ui.AppColors
import com.autodoc.ui.CarUi
import com.autodoc.ui.screens.DashboardScreen
import com.autodoc.ui.screens.DocumentsScreen
import com.autodoc.ui.screens.SettingsScreen
import com.autodoc.ui.theme.AutoDocTheme
import com.autodoc.viewmodel.AutoDocViewModel
import com.autodoc.viewmodel.AutoDocViewModelFactory
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AutoDocViewModel

    private var currentScreen: AppScreen by mutableStateOf(AppScreen.DASHBOARD)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Aplicatia ramane functionala chiar daca permisiunea este refuzata.
        }

    private var selectedImportUri: Uri? by mutableStateOf(null)
    private var showImportConfirmDialog: Boolean by mutableStateOf(false)

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) {
                Toast.makeText(this, "Import anulat.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            selectedImportUri = uri
            showImportConfirmDialog = true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentScreen = savedInstanceState
            ?.getString(KEY_CURRENT_SCREEN)
            ?.let { savedScreen ->
                runCatching { AppScreen.valueOf(savedScreen) }.getOrNull()
            }
            ?: AppScreen.DASHBOARD

        requestNotificationPermissionIfNeeded()
        scheduleDailyDocumentCheck()
        scheduleAutoBackup()

        val database = DatabaseProvider.getDatabase(this)
        val scheduler = AutoDocNotificationScheduler(this)
        val appPlanManager = AppPlanManager(this)

        val factory = AutoDocViewModelFactory(
            carDao = database.carDao(),
            documentDao = database.documentDao(),
            scheduler = scheduler,
            appPlanManager = appPlanManager
        )

        viewModel = ViewModelProvider(
            this,
            factory
        )[AutoDocViewModel::class.java]

        setContent {
            AutoDocTheme(dynamicColor = false) {
                val cars by viewModel.cars.collectAsState()
                val isProPlan by viewModel.isProPlan.collectAsState()
                val userMessage by viewModel.userMessage.collectAsState()

                LaunchedEffect(userMessage) {
                    val message = userMessage
                    if (!message.isNullOrBlank()) {
                        Toast.makeText(
                            this@MainActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.clearUserMessage()
                    }
                }

                if (showImportConfirmDialog) {
                    ConfirmImportBackupDialog(
                        onConfirm = {
                            val uriToImport = selectedImportUri
                            showImportConfirmDialog = false
                            selectedImportUri = null

                            if (uriToImport != null) {
                                importBackupFile(uriToImport)
                            }
                        },
                        onDismiss = {
                            showImportConfirmDialog = false
                            selectedImportUri = null
                            Toast.makeText(
                                this@MainActivity,
                                "Import anulat.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.DeepBg),
                    containerColor = AppColors.DeepBg,
                    bottomBar = {
                        BottomNavigationBar(
                            currentScreen = currentScreen,
                            onScreenSelected = { selectedScreen ->
                                currentScreen = selectedScreen
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.DeepBg)
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> {
                                DashboardScreen(
                                    cars = cars,
                                    onAddCar = { brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes ->
                                        viewModel.addCar(
                                            brand = brand,
                                            model = model,
                                            plate = plate,
                                            year = year,
                                            engine = engine,
                                            ownerName = ownerName,
                                            ownerPhone = ownerPhone,
                                            ownerEmail = ownerEmail,
                                            ownerNotes = ownerNotes
                                        )
                                    },
                                    onUpdateCar = { carId, brand, model, plate, year, engine, ownerName, ownerPhone, ownerEmail, ownerNotes ->
                                        viewModel.updateCar(
                                            carId = carId,
                                            brand = brand,
                                            model = model,
                                            plate = plate,
                                            year = year,
                                            engine = engine,
                                            ownerName = ownerName,
                                            ownerPhone = ownerPhone,
                                            ownerEmail = ownerEmail,
                                            ownerNotes = ownerNotes
                                        )
                                    },
                                    onAddDocument = { carId, type, expiry, days ->
                                        viewModel.addDocument(carId, type, expiry, days)
                                    },
                                    onDeleteDocument = { documentId ->
                                        viewModel.deleteDocument(documentId)
                                    },
                                    onUpdateDocumentExpiry = { documentId, expiryMillis ->
                                        viewModel.updateDocumentExpiry(documentId, expiryMillis)
                                    },
                                    onDeleteCar = { carId ->
                                        viewModel.deleteCar(carId)
                                    },
                                    onExportCarPdf = { car ->
                                        exportCarPdf(car)
                                    }
                                )
                            }

                            AppScreen.DOCUMENTS -> {
                                DocumentsScreen(
                                    cars = cars,
                                    onMarkDocumentManuallyNotified = { documentId ->
                                        viewModel.markDocumentManuallyNotified(documentId)
                                    }
                                )
                            }

                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    onExportBackup = {
                                        exportBackupFile()
                                    },
                                    onImportBackup = {
                                        importBackupLauncher.launch(
                                            arrayOf(
                                                "application/json",
                                                "text/plain",
                                                "application/octet-stream"
                                            )
                                        )
                                    },
                                    isProPlan = isProPlan,
                                    onToggleProPlan = { enabled ->
                                        viewModel.setProPlan(enabled)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_CURRENT_SCREEN, currentScreen.name)
        super.onSaveInstanceState(outState)
    }

    private fun exportBackupFile() {
        lifecycleScope.launch {
            try {
                val success = BackupManager.saveBackupToDownloads(this@MainActivity)

                Toast.makeText(
                    this@MainActivity,
                    if (success) {
                        "Backup salvat in Downloads: autodoc_backup.json"
                    } else {
                        "Nu s-a putut salva backup-ul in Downloads."
                    },
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Eroare la export backup: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun importBackupFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                val scheduler = AutoDocNotificationScheduler(this@MainActivity)

                val success = BackupManager.importBackupFromUri(
                    context = this@MainActivity,
                    uri = uri,
                    scheduler = scheduler
                )

                Toast.makeText(
                    this@MainActivity,
                    if (success) {
                        "Date restaurate complet. Backup de siguranta creat inainte de import si notificari reprogramate."
                    } else {
                        "Fisier backup invalid, incomplet sau fara masini valide. Datele existente nu au fost sterse."
                    },
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Eroare la import backup: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun exportCarPdf(car: CarUi) {
        lifecycleScope.launch {
            try {
                val success = PdfExportManager.exportCarPdfToDownloads(
                    context = this@MainActivity,
                    car = car
                )

                Toast.makeText(
                    this@MainActivity,
                    if (success) {
                        "PDF salvat in Downloads."
                    } else {
                        "Nu s-a putut genera PDF-ul."
                    },
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Eroare la export PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun scheduleDailyDocumentCheck() {
        val now = LocalDateTime.now()

        val todayAtNine = now
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val nextRun = if (todayAtNine.isAfter(now)) {
            todayAtNine
        } else {
            todayAtNine.plusDays(1)
        }

        val delayMillis = Duration.between(now, nextRun).toMillis()

        val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(
            1,
            TimeUnit.DAYS
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_document_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleAutoBackup() {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            1,
            TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto_backup",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    companion object {
        private const val KEY_CURRENT_SCREEN = "current_screen"
    }
}

private enum class AppScreen {
    DASHBOARD,
    DOCUMENTS,
    SETTINGS
}

private data class BottomNavItem(
    val screen: AppScreen,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

@Composable
private fun BottomNavigationBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            screen = AppScreen.DASHBOARD,
            label = "Dashboard",
            icon = Icons.Outlined.Home,
            contentDescription = "Dashboard"
        ),
        BottomNavItem(
            screen = AppScreen.DOCUMENTS,
            label = "Documente",
            icon = Icons.Outlined.List,
            contentDescription = "Documente"
        ),
        BottomNavItem(
            screen = AppScreen.SETTINGS,
            label = "Setari",
            icon = Icons.Outlined.Settings,
            contentDescription = "Setari"
        )
    )

    NavigationBar(
        containerColor = AppColors.Navy,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentScreen == item.screen,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = navItemColors()
            )
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AppColors.Gold,
    selectedTextColor = AppColors.Gold,
    indicatorColor = AppColors.CardBg,
    unselectedIconColor = AppColors.SoftText,
    unselectedTextColor = AppColors.SoftText
)

@Composable
private fun ConfirmImportBackupDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirmare import backup",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Atentie: importul va crea automat un backup de siguranta, apoi va sterge datele existente si le va inlocui cu datele din fisierul selectat.\n\nContinui?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Danger)
            ) {
                Text(
                    text = "Da, import",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
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
                    color = AppColors.Navy,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}