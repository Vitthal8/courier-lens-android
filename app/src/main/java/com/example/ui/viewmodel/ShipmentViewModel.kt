package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.ShipmentEntity
import com.example.data.repository.ShipmentRepository
import com.example.export.ManifestExportService
import com.example.ocr.CourierOcrEngine
import com.example.ocr.ExtractedShipment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ShipmentUiState(
    val shipments: List<ShipmentEntity> = emptyList(),
    val filteredShipments: List<ShipmentEntity> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: String? = null, // null for All
    val courierFilter: String? = null,
    val selectedIds: Set<Long> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isScanning: Boolean = false,
    val isBatchMode: Boolean = false,
    val batchScannedCount: Int = 0,
    val activeExtractedShipment: ExtractedShipment? = null,
    val editingShipment: ShipmentEntity? = null,
    val exportResult: ManifestExportService.ExportResult? = null,
    val snackbarMessage: String? = null,
    val totalCount: Int = 0,
    val scannedTodayCount: Int = 0,
    val pendingDispatchCount: Int = 0,
    val distinctCitiesCount: Int = 0
)

class ShipmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ShipmentRepository
    private val ocrEngine = CourierOcrEngine()

    private val _uiState = MutableStateFlow(ShipmentUiState())
    val uiState: StateFlow<ShipmentUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ShipmentRepository(db.shipmentDao())

        viewModelScope.launch {
            repository.allShipments.collect { allList ->
                val todayStart = getStartOfTodayMillis()
                val todayCount = allList.count { it.createdAt >= todayStart }
                val pendingCount = allList.count { it.status == "Scanned" || it.status == "Manifested" }
                val citiesCount = allList.map { it.city.trim().lowercase(Locale.US) }.filter { it.isNotEmpty() }.distinct().size

                _uiState.update { state ->
                    val filtered = applyFilterAndSearch(
                        allList,
                        state.searchQuery,
                        state.statusFilter,
                        state.courierFilter
                    )
                    state.copy(
                        shipments = allList,
                        filteredShipments = filtered,
                        totalCount = allList.size,
                        scannedTodayCount = todayCount,
                        pendingDispatchCount = pendingCount,
                        distinctCitiesCount = citiesCount
                    )
                }
            }
        }

        // Clean up any legacy auto-seeded sample records from previous APK versions once,
        // while keeping all real user-scanned shipments intact.
        viewModelScope.launch {
            purgeLegacySampleDataOnce()
        }
    }

    private fun getStartOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilterAndSearch(
                state.shipments,
                query,
                state.statusFilter,
                state.courierFilter
            )
            state.copy(searchQuery = query, filteredShipments = filtered)
        }
    }

    fun setStatusFilter(status: String?) {
        _uiState.update { state ->
            val filtered = applyFilterAndSearch(
                state.shipments,
                state.searchQuery,
                status,
                state.courierFilter
            )
            state.copy(statusFilter = status, filteredShipments = filtered)
        }
    }

    fun setCourierFilter(courier: String?) {
        _uiState.update { state ->
            val filtered = applyFilterAndSearch(
                state.shipments,
                state.searchQuery,
                state.statusFilter,
                courier
            )
            state.copy(courierFilter = courier, filteredShipments = filtered)
        }
    }

    private fun applyFilterAndSearch(
        list: List<ShipmentEntity>,
        query: String,
        status: String?,
        courier: String?
    ): List<ShipmentEntity> {
        return list.filter { item ->
            val matchesStatus = status == null || item.status.equals(status, ignoreCase = true)
            val matchesCourier = courier == null || item.courierPartner.equals(courier, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                item.awbNumber.contains(query, ignoreCase = true) ||
                item.serialNumber.contains(query, ignoreCase = true) ||
                item.consigneeName.contains(query, ignoreCase = true) ||
                item.consigneePhone.contains(query, ignoreCase = true) ||
                item.city.contains(query, ignoreCase = true) ||
                item.pincode.contains(query, ignoreCase = true) ||
                item.referenceNumber.contains(query, ignoreCase = true)
            matchesStatus && matchesCourier && matchesQuery
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelected = if (state.selectedIds.contains(id)) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(
                selectedIds = newSelected,
                isMultiSelectMode = newSelected.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.filteredShipments.map { it.id }.toSet()
            state.copy(selectedIds = allIds, isMultiSelectMode = allIds.isNotEmpty())
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            state.copy(selectedIds = emptySet(), isMultiSelectMode = false)
        }
    }

    fun deleteSelected() {
        val idsToDelete = _uiState.value.selectedIds.toList()
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteByIds(idsToDelete)
            _uiState.update {
                it.copy(
                    selectedIds = emptySet(),
                    isMultiSelectMode = false,
                    snackbarMessage = "Deleted ${idsToDelete.size} shipments"
                )
            }
        }
    }

    fun updateStatusForSelected(newStatus: String) {
        val idsToUpdate = _uiState.value.selectedIds.toList()
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatusForIds(idsToUpdate, newStatus)
            _uiState.update {
                it.copy(
                    selectedIds = emptySet(),
                    isMultiSelectMode = false,
                    snackbarMessage = "Updated ${idsToUpdate.size} shipments to $newStatus"
                )
            }
        }
    }

    fun startBatchMode() {
        _uiState.update { it.copy(isBatchMode = true, batchScannedCount = 0) }
    }

    fun stopBatchMode() {
        _uiState.update { it.copy(isBatchMode = false) }
    }

    fun processCapturedBitmap(
        bitmap: Bitmap,
        autoSave: Boolean,
        onSuccess: (ShipmentEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            try {
                val nextIndex = (_uiState.value.shipments.size + 1)
                val photoPath = saveBitmapLocally(bitmap)
                val extracted = withContext(Dispatchers.Default) {
                    ocrEngine.processBitmap(bitmap, nextIndex)
                }

                val shipment = ShipmentEntity(
                    serialNumber = extracted.serialNumber,
                    awbNumber = extracted.awbNumber,
                    courierPartner = extracted.courierPartner,
                    consigneeName = extracted.consigneeName,
                    consigneePhone = extracted.consigneePhone,
                    addressLine = extracted.addressLine,
                    city = extracted.city,
                    state = extracted.state,
                    pincode = extracted.pincode,
                    country = extracted.country,
                    shipperName = extracted.shipperName,
                    referenceNumber = extracted.referenceNumber,
                    weight = extracted.weight,
                    status = "Scanned",
                    rawOcrText = extracted.rawText,
                    photoPath = photoPath,
                    batchTag = "Batch " + SimpleDateFormat("dd-MMM", Locale.US).format(Date())
                )

                if (autoSave) {
                    val id = withContext(Dispatchers.IO) {
                        repository.insertShipment(shipment)
                    }
                    val savedShipment = shipment.copy(id = id)
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            batchScannedCount = it.batchScannedCount + 1,
                            snackbarMessage = "Scanned #${savedShipment.serialNumber} (${savedShipment.awbNumber.ifEmpty { "Saved" }})"
                        )
                    }
                    onSuccess(savedShipment)
                } else {
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            activeExtractedShipment = extracted,
                            editingShipment = shipment
                        )
                    }
                    onSuccess(shipment)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        snackbarMessage = "OCR Scan failed: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun processGalleryUris(context: Context, uris: List<Uri>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            var successCount = 0
            val newShipments = mutableListOf<ShipmentEntity>()
            val startIndex = _uiState.value.shipments.size + 1

            withContext(Dispatchers.IO) {
                uris.forEachIndexed { idx, uri ->
                    try {
                        val extracted = ocrEngine.processUri(context, uri, startIndex + idx)
                        val persistedPhotoPath = copyUriToPersistentStorage(context, uri) ?: uri.toString()
                        newShipments.add(
                            ShipmentEntity(
                                serialNumber = extracted.serialNumber,
                                awbNumber = extracted.awbNumber,
                                courierPartner = extracted.courierPartner,
                                consigneeName = extracted.consigneeName,
                                consigneePhone = extracted.consigneePhone,
                                addressLine = extracted.addressLine,
                                city = extracted.city,
                                state = extracted.state,
                                pincode = extracted.pincode,
                                country = extracted.country,
                                shipperName = extracted.shipperName,
                                referenceNumber = extracted.referenceNumber,
                                weight = extracted.weight,
                                status = "Scanned",
                                rawOcrText = extracted.rawText,
                                photoPath = persistedPhotoPath,
                                batchTag = "Gallery Import " + SimpleDateFormat("dd-MMM", Locale.US).format(Date())
                            )
                        )
                        successCount++
                    } catch (e: Exception) {
                        // Continue processing remaining images
                    }
                }
                if (newShipments.isNotEmpty()) {
                    repository.insertShipments(newShipments)
                }
            }

            _uiState.update {
                it.copy(
                    isScanning = false,
                    snackbarMessage = "Successfully imported and OCR-parsed $successCount parcel photos!"
                )
            }
            onComplete(successCount)
        }
    }

    fun setEditingShipment(shipment: ShipmentEntity?) {
        _uiState.update { it.copy(editingShipment = shipment) }
    }

    fun saveShipment(shipment: ShipmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (shipment.id == 0L) {
                repository.insertShipment(shipment)
            } else {
                repository.updateShipment(shipment)
            }
            _uiState.update {
                it.copy(
                    editingShipment = null,
                    activeExtractedShipment = null,
                    snackbarMessage = "Shipment ${shipment.serialNumber} saved"
                )
            }
        }
    }

    fun deleteShipment(shipment: ShipmentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteShipment(shipment)
            _uiState.update {
                it.copy(
                    editingShipment = null,
                    snackbarMessage = "Shipment deleted"
                )
            }
        }
    }

    fun exportManifest(
        context: Context,
        format: ManifestExportService.ExportFormat,
        selectedOnly: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val listToExport = if (selectedOnly && _uiState.value.selectedIds.isNotEmpty()) {
                _uiState.value.shipments.filter { _uiState.value.selectedIds.contains(it.id) }
            } else {
                _uiState.value.filteredShipments
            }

            if (listToExport.isEmpty()) {
                _uiState.update { it.copy(snackbarMessage = "No shipments to export.") }
                return@launch
            }

            try {
                val result = ManifestExportService.exportShipments(context, listToExport, format)
                _uiState.update {
                    it.copy(
                        exportResult = result,
                        snackbarMessage = "Exported ${result.recordCount} items to ${result.file.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(snackbarMessage = "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun saveBitmapLocally(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        // Store in filesDir (persistent internal storage) instead of cacheDir so parcel photos
        // survive cache cleanup, app restarts, and APK updates.
        val dir = File(getApplication<Application>().filesDir, "parcel_photos").apply { mkdirs() }
        val filename = "parcel_${System.currentTimeMillis()}.jpg"
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        file.absolutePath
    }

    private fun copyUriToPersistentStorage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(getApplication<Application>().filesDir, "parcel_photos").apply { mkdirs() }
            val filename = "parcel_import_${System.currentTimeMillis()}_${uri.hashCode().toUInt()}.jpg"
            val destFile = File(dir, filename)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0L) destFile.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun purgeLegacySampleDataOnce() = withContext(Dispatchers.IO) {
        val prefs = getApplication<Application>().getSharedPreferences("courier_lens_prefs", Context.MODE_PRIVATE)
        val hasPurgedSamples = prefs.getBoolean("legacy_samples_purged_v2", false)
        if (!hasPurgedSamples) {
            repository.deleteLegacySampleShipments()
            prefs.edit().putBoolean("legacy_samples_purged_v2", true).apply()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }
}
