package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipments")
data class ShipmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serialNumber: String = "",
    val awbNumber: String = "",
    val courierPartner: String = "Generic Courier",
    val consigneeName: String = "",
    val consigneePhone: String = "",
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val country: String = "India",
    val shipperName: String = "",
    val referenceNumber: String = "",
    val weight: String = "",
    val status: String = "Scanned", // Scanned, Manifested, Dispatched, Delivered
    val rawOcrText: String = "",
    val photoPath: String? = null,
    val batchTag: String = "Default Batch",
    val createdAt: Long = System.currentTimeMillis()
)
