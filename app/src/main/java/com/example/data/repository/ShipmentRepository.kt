package com.example.data.repository

import com.example.data.db.ShipmentDao
import com.example.data.model.ShipmentEntity
import kotlinx.coroutines.flow.Flow

class ShipmentRepository(private val dao: ShipmentDao) {
    val allShipments: Flow<List<ShipmentEntity>> = dao.getAllShipments()
    val totalCount: Flow<Int> = dao.getTotalCount()
    val distinctCities: Flow<List<String>> = dao.getDistinctCities()

    fun getShipmentById(id: Long): Flow<ShipmentEntity?> = dao.getShipmentById(id)

    fun searchShipments(query: String): Flow<List<ShipmentEntity>> = dao.searchShipments(query)

    fun getCountByStatus(status: String): Flow<Int> = dao.getCountByStatus(status)

    suspend fun insertShipment(shipment: ShipmentEntity): Long = dao.insert(shipment)

    suspend fun insertShipments(shipments: List<ShipmentEntity>): List<Long> = dao.insertAll(shipments)

    suspend fun updateShipment(shipment: ShipmentEntity) = dao.update(shipment)

    suspend fun deleteShipment(shipment: ShipmentEntity) = dao.delete(shipment)

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun updateStatusForIds(ids: List<Long>, status: String) = dao.updateStatusForIds(ids, status)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun deleteLegacySampleShipments() = dao.deleteLegacySampleShipments()
}
