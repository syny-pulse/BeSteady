package com.besteady.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrillRecordDao {
    
    @Query("SELECT * FROM drill_records ORDER BY startTime DESC")
    fun getAllDrills(): Flow<List<DrillRecord>>
    
    @Query("SELECT * FROM drill_records WHERE id = :id")
    suspend fun getDrillById(id: Long): DrillRecord?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrill(drillRecord: DrillRecord): Long
    
    @Update
    suspend fun updateDrill(drillRecord: DrillRecord)
    
    @Delete
    suspend fun deleteDrill(drillRecord: DrillRecord)
    
    @Query("DELETE FROM drill_records")
    suspend fun deleteAllDrills()
    
    @Query("SELECT COUNT(*) FROM drill_records")
    suspend fun getDrillCount(): Int
}

