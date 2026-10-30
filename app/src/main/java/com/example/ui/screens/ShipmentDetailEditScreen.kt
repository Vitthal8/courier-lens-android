package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ShipmentEntity
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LogisticsAmber
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShipmentDetailEditScreen(
    initialShipment: ShipmentEntity,
    onSave: (ShipmentEntity) -> Unit,
    onDelete: (ShipmentEntity) -> Unit,
    onBack: () -> Unit
) {
    var serialNumber by remember { mutableStateOf(initialShipment.serialNumber) }
    var awbNumber by remember { mutableStateOf(initialShipment.awbNumber) }
    var courierPartner by remember { mutableStateOf(initialShipment.courierPartner) }
    var consigneeName by remember { mutableStateOf(initialShipment.consigneeName) }
    var consigneePhone by remember { mutableStateOf(initialShipment.consigneePhone) }
    var addressLine by remember { mutableStateOf(initialShipment.addressLine) }
    var city by remember { mutableStateOf(initialShipment.city) }
    var state by remember { mutableStateOf(initialShipment.state) }
    var pincode by remember { mutableStateOf(initialShipment.pincode) }
    var country by remember { mutableStateOf(initialShipment.country) }
    var shipperName by remember { mutableStateOf(initialShipment.shipperName) }
    var referenceNumber by remember { mutableStateOf(initialShipment.referenceNumber) }
    var weight by remember { mutableStateOf(initialShipment.weight) }
    var status by remember { mutableStateOf(initialShipment.status) }

    var selectedOcrLine by remember { mutableStateOf<String?>(null) }
    var showAssignSheet by remember { mutableStateOf(false) }

    val commonCouriers = listOf(
        "Delhivery", "Blue Dart", "DTDC", "India Post / Speed Post",
        "Ekart Logistics", "Shadowfax", "Xpressbees", "FedEx", "DHL", "UPS"
    )

    val statuses = listOf("Scanned", "Manifested", "Dispatched", "Delivered")
    var statusExpanded by remember { mutableStateOf(false) }

    val rawLines = remember(initialShipment.rawOcrText) {
        initialShipment.rawOcrText.split("\n")
            .map { it.trim() }
            .filter { it.length > 1 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (initialShipment.id == 0L) "New Scanned Parcel" else "Edit Shipment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = serialNumber.ifEmpty { "Unassigned Serial" },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (initialShipment.id != 0L) {
                        IconButton(
                            onClick = { onDelete(initialShipment) },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = StatusAlert
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            SurfaceBottomBar(
                onSave = {
                    val updated = initialShipment.copy(
                        serialNumber = serialNumber.trim(),
                        awbNumber = awbNumber.trim(),
                        courierPartner = courierPartner.trim(),
                        consigneeName = consigneeName.trim(),
                        consigneePhone = consigneePhone.trim(),
                        addressLine = addressLine.trim(),
                        city = city.trim(),
                        state = state.trim(),
                        pincode = pincode.trim(),
                        country = country.trim(),
                        shipperName = shipperName.trim(),
                        referenceNumber = referenceNumber.trim(),
                        weight = weight.trim(),
                        status = status
                    )
                    onSave(updated)
                },
                onCancel = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Optional Parcel Photo Preview
            if (!initialShipment.photoPath.isNullOrEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AsyncImage(
                        model = initialShipment.photoPath,
                        contentDescription = "Parcel label photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Quick Tap-to-Field OCR Assistant Palette
            if (rawLines.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "On-Device OCR Text Palette (Tap line to assign):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rawLines.take(12).forEach { line ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedOcrLine = line
                                            showAssignSheet = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. Identifiers: Serial No & AWB
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Serial No") },
                    modifier = Modifier
                        .weight(0.45f)
                        .testTag("input_serial_number"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = awbNumber,
                    onValueChange = { awbNumber = it },
                    label = { Text("AWB / Tracking No *") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .weight(0.55f)
                        .testTag("input_awb_number"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // 2. Courier Partner
            Column {
                OutlinedTextField(
                    value = courierPartner,
                    onValueChange = { courierPartner = it },
                    label = { Text("Courier Partner") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_courier_partner"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Quick Courier Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonCouriers.forEach { courier ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (courierPartner == courier) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { courierPartner = courier }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = courier,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (courierPartner == courier) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // 3. Status Dropdown
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Shipment Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    statuses.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                status = item
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            // 4. Consignee Info
            OutlinedTextField(
                value = consigneeName,
                onValueChange = { consigneeName = it },
                label = { Text("Receiver / Consignee Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_consignee_name"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = consigneePhone,
                onValueChange = { consigneePhone = it },
                label = { Text("Contact Phone / Mobile") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_consignee_phone"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // 5. Destination Address
            OutlinedTextField(
                value = addressLine,
                onValueChange = { addressLine = it },
                label = { Text("Delivery Address Line") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = LogisticsAmber
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_address_line"),
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(10.dp)
            )

            // City, PIN Code, State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_city"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("PIN Code") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_pincode"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text("State") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_state"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_country"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // 6. Shipper & Reference
            OutlinedTextField(
                value = shipperName,
                onValueChange = { shipperName = it },
                label = { Text("Shipper / Sender Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_shipper_name"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = referenceNumber,
                    onValueChange = { referenceNumber = it },
                    label = { Text("Order / Ref No") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_reference_number"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_weight"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal Sheet for Assigning Tapped OCR Line to a Field
    if (showAssignSheet && selectedOcrLine != null) {
        val line = selectedOcrLine!!
        ModalBottomSheet(
            onDismissRequest = { showAssignSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Assign OCR Text To Field:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "\"$line\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Select destination field:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AssignOptionRow("Set as AWB / Tracking No") {
                    awbNumber = line.replace("[^A-Za-z0-9]".toRegex(), "")
                    showAssignSheet = false
                }
                AssignOptionRow("Set as Consignee Name") {
                    consigneeName = line
                    showAssignSheet = false
                }
                AssignOptionRow("Set as Phone Number") {
                    consigneePhone = line.replace("[^0-9+]".toRegex(), "")
                    showAssignSheet = false
                }
                AssignOptionRow("Set as City") {
                    city = line
                    showAssignSheet = false
                }
                AssignOptionRow("Set as PIN Code") {
                    pincode = line.replace("[^0-9]".toRegex(), "").take(6)
                    showAssignSheet = false
                }
                AssignOptionRow("Set as Delivery Address") {
                    addressLine = if (addressLine.isEmpty()) line else "$addressLine, $line"
                    showAssignSheet = false
                }
                AssignOptionRow("Set as Reference / Order ID") {
                    referenceNumber = line
                    showAssignSheet = false
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AssignOptionRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SurfaceBottomBar(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_shipment_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Save Record")
            }
        }
    }
}
