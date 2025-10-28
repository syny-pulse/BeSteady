package com.besteady.data

import kotlinx.coroutines.flow.Flow

class DrillRecordRepository(
    private val drillRecordDao: DrillRecordDao
) {
    
    val allDrills: Flow<List<DrillRecord>> = drillRecordDao.getAllDrills()
    
    suspend fun insertDrill(drillRecord: DrillRecord): Long {
        return drillRecordDao.insertDrill(drillRecord)
    }
    
    suspend fun getDrillById(id: Long): DrillRecord? {
        return drillRecordDao.getDrillById(id)
    }
    
    suspend fun deleteDrill(drillRecord: DrillRecord) {
        drillRecordDao.deleteDrill(drillRecord)
    }
    
    suspend fun deleteAllDrills() {
        drillRecordDao.deleteAllDrills()
    }
    
    suspend fun getDrillCount(): Int {
        return drillRecordDao.getDrillCount()
    }
}

