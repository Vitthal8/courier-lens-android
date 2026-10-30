package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ShipmentEntity
import com.example.export.ManifestExportService
import com.example.ui.dialogs.ExportManifestDialog
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.ShipmentDetailEditScreen
import com.example.ui.screens.ShipmentListScreen
import com.example.ui.theme.CourierLensTheme
import com.example.ui.viewmodel.ShipmentViewModel
import kotlinx.coroutines.launch

enum class AppScreen {
    LIST,
    CAMERA,
    DETAIL_EDIT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CourierLensTheme {
                CourierLensApp()
            }
        }
    }
}

@Composable
fun CourierLensApp(
    viewModel: ShipmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentScreen by remember { mutableStateOf(AppScreen.LIST) }
    var isCameraBatchMode by remember { mutableStateOf(false) }
    var activeEditingShipment by remember { mutableStateOf<ShipmentEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Android Photo Picker
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(25)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.processGalleryUris(context, uris) { count ->
                currentScreen = AppScreen.LIST
            }
        }
    }

    // Snackbar notifications
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Export result trigger
    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { result ->
            ManifestExportService.shareExportFile(context, result)
            viewModel.clearExportResult()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.LIST -> {
                ShipmentListScreen(
                    uiState = uiState,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onStatusFilterChange = { viewModel.setStatusFilter(it) },
                    onCourierFilterChange = { viewModel.setCourierFilter(it) },
                    onToggleSelect = { viewModel.toggleSelection(it) },
                    onSelectAll = { viewModel.selectAll() },
                    onClearSelection = { viewModel.clearSelection() },
                    onDeleteSelected = { viewModel.deleteSelected() },
                    onUpdateStatusForSelected = { viewModel.updateStatusForSelected(it) },
                    onItemClick = { shipment ->
                        activeEditingShipment = shipment
                        currentScreen = AppScreen.DETAIL_EDIT
                    },
                    onStartCameraScan = { isBatch ->
                        isCameraBatchMode = isBatch
                        currentScreen = AppScreen.CAMERA
                    },
                    onImportPhotos = { uris ->
                        viewModel.processGalleryUris(context, uris) {}
                    },
                    onOpenExportDialog = {
                        showExportDialog = true
                    }
                )
            }

            AppScreen.CAMERA -> {
                BackHandler {
                    currentScreen = AppScreen.LIST
                }

                CameraScannerScreen(
                    isBatchModeInitially = isCameraBatchMode,
                    onCaptured = { bitmap, isBatch ->
                        if (isBatch) {
                            // Auto save in batch mode without leaving camera
                            viewModel.processCapturedBitmap(bitmap, autoSave = true) {}
                        } else {
                            // Single scan: process and open editor
                            viewModel.processCapturedBitmap(bitmap, autoSave = false) { shipment ->
                                activeEditingShipment = shipment
                                currentScreen = AppScreen.DETAIL_EDIT
                            }
                        }
                    },
                    onGalleryPick = {
                        galleryPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onBack = {
                        currentScreen = AppScreen.LIST
                    }
                )
            }

            AppScreen.DETAIL_EDIT -> {
                BackHandler {
                    currentScreen = AppScreen.LIST
                }

                val shipmentToEdit = activeEditingShipment ?: ShipmentEntity()

                ShipmentDetailEditScreen(
                    initialShipment = shipmentToEdit,
                    onSave = { updated ->
                        viewModel.saveShipment(updated)
                        currentScreen = AppScreen.LIST
                    },
                    onDelete = { toDelete ->
                        viewModel.deleteShipment(toDelete)
                        currentScreen = AppScreen.LIST
                    },
                    onBack = {
                        currentScreen = AppScreen.LIST
                    }
                )
            }
        }

        // Export Dialog
        if (showExportDialog) {
            ExportManifestDialog(
                totalFilteredCount = uiState.filteredShipments.size,
                selectedCount = uiState.selectedIds.size,
                onDismiss = { showExportDialog = false },
                onExport = { format, exportSelectedOnly ->
                    viewModel.exportManifest(context, format, exportSelectedOnly)
                    showExportDialog = false
                }
            )
        }
    }
}
