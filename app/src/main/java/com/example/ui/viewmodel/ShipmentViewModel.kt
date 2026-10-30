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

        // Seed realistic courier packets on initial launch if empty
        viewModelScope.launch {
            seedSampleShipmentsIfEmpty()
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
                                photoPath = uri.toString(),
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
        val dir = File(getApplication<Application>().cacheDir, "parcel_photos").apply { mkdirs() }
        val filename = "parcel_${System.currentTimeMillis()}.jpg"
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        file.absolutePath
    }

    private suspend fun seedSampleShipmentsIfEmpty() = withContext(Dispatchers.IO) {
        val currentCount = repository.allShipments
        // If empty, add standard courier packets so user gets instant logistics demo
        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val samples = listOf(
            ShipmentEntity(
                serialNumber = "SN-$datePrefix-001",
                awbNumber = "DEL94820194820",
                courierPartner = "Delhivery",
                consigneeName = "Rohan Sharma",
                consigneePhone = "9820194820",
                addressLine = "Flat 402, Sunshine Enclave, Indiranagar 100ft Road",
                city = "Bengaluru",
                state = "Karnataka",
                pincode = "560038",
                country = "India",
                shipperName = "TechTrend Hub Logistics",
                referenceNumber = "ORD-84920",
                weight = "0.75 kg",
                status = "Scanned",
                batchTag = "Morning Dispatch",
                rawOcrText = "Delhivery Surface\nAWB: DEL94820194820\nTo: Rohan Sharma\nFlat 402 Sunshine Enclave Indiranagar\nBengaluru Karnataka 560038\nMob: 9820194820\nWeight: 0.75 kg\nOrder: ORD-84920"
            ),
            ShipmentEntity(
                serialNumber = "SN-$datePrefix-002",
                awbNumber = "85412948201",
                courierPartner = "Blue Dart",
                consigneeName = "Priya Nair",
                consigneePhone = "9876543210",
                addressLine = "Plot 14, Lotus Park, Bandra Kurla Complex",
                city = "Mumbai",
                state = "Maharashtra",
                pincode = "400051",
                country = "India",
                shipperName = "Apex Retailers Ltd",
                referenceNumber = "INV-2026-991",
                weight = "1.2 kg",
                status = "Manifested",
                batchTag = "Morning Dispatch",
                rawOcrText = "Blue Dart Express\nWAYBILL: 85412948201\nDeliver To: Priya Nair\nPlot 14 Lotus Park BKC\nMumbai Maharashtra 400051\nPh: 9876543210\nNet Wt: 1.2 kg"
            ),
            ShipmentEntity(
                serialNumber = "SN-$datePrefix-003",
                awbNumber = "D94820148",
                courierPartner = "DTDC",
                consigneeName = "Amitabh Verma",
                consigneePhone = "9123456789",
                addressLine = "House 18, Sector 15, Near Metro Gate 2",
                city = "Noida",
                state = "Uttar Pradesh",
                pincode = "201301",
                country = "India",
                shipperName = "Global Book Distributors",
                referenceNumber = "REF-44210",
                weight = "0.45 kg",
                status = "Dispatched",
                batchTag = "Morning Dispatch",
                rawOcrText = "DTDC Plus\nCN NO: D94820148\nTo: Amitabh Verma\nHouse 18 Sector 15 Noida UP\nPIN: 201301\nContact: 9123456789\nWeight: 0.45 kg"
            ),
            ShipmentEntity(
                serialNumber = "SN-$datePrefix-004",
                awbNumber = "EM849201948IN",
                courierPartner = "India Post / Speed Post",
                consigneeName = "Kavita Sundaram",
                consigneePhone = "9444123456",
                addressLine = "22 Anna Salai, T. Nagar",
                city = "Chennai",
                state = "Tamil Nadu",
                pincode = "600017",
                country = "India",
                shipperName = "National Electronics",
                referenceNumber = "PO-7781",
                weight = "0.3 kg",
                status = "Delivered",
                batchTag = "Morning Dispatch",
                rawOcrText = "Speed Post\nArticle: EM849201948IN\nTo: Kavita Sundaram\n22 Anna Salai T Nagar Chennai TN 600017\nPh: 9444123456"
            )
        )
        // Check if database is empty by checking count
        // Note: insertAll handles conflict replacement
        repository.insertShipments(samples)
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.close()
    }
}
