package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShipmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShipmentDao {
    @Query("SELECT * FROM shipments ORDER BY createdAt DESC")
    fun getAllShipments(): Flow<List<ShipmentEntity>>

    @Query("SELECT * FROM shipments WHERE id = :id LIMIT 1")
    fun getShipmentById(id: Long): Flow<ShipmentEntity?>

    @Query("""
        SELECT * FROM shipments 
        WHERE awbNumber LIKE '%' || :query || '%' 
           OR serialNumber LIKE '%' || :query || '%'
           OR consigneeName LIKE '%' || :query || '%'
           OR consigneePhone LIKE '%' || :query || '%'
           OR city LIKE '%' || :query || '%'
           OR pincode LIKE '%' || :query || '%'
           OR referenceNumber LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchShipments(query: String): Flow<List<ShipmentEntity>>

    @Query("SELECT COUNT(*) FROM shipments")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM shipments WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>

    @Query("SELECT DISTINCT city FROM shipments WHERE city != ''")
    fun getDistinctCities(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shipment: ShipmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shipments: List<ShipmentEntity>): List<Long>

    @Update
    suspend fun update(shipment: ShipmentEntity)

    @Delete
    suspend fun delete(shipment: ShipmentEntity)

    @Query("DELETE FROM shipments WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE shipments SET status = :newStatus WHERE id IN (:ids)")
    suspend fun updateStatusForIds(ids: List<Long>, newStatus: String)

    @Query("DELETE FROM shipments")
    suspend fun deleteAll()
}
