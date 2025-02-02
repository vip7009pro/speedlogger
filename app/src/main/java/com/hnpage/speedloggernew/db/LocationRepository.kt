package com.hnpage.speedloggernew.db

import android.content.Context
import androidx.lifecycle.LiveData
import com.hnpage.speedloggernew.global.DataInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LocationRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context).locationDao()

    suspend fun insertSpeedRecord(record: DataInterface.LocationData2) {
        withContext(Dispatchers.IO) {
            db.insert(record)
        }
    }

    val getAllSpeedRecords: Flow<List<DataInterface.LocationData2>>  = db.getAllLocations()
    fun getSpeedRecordsInRange(startTime: Long, endTime: Long): Flow<List<DataInterface.LocationData2>> {
        return db.getLocationsInRange(startTime, endTime)
    }

    suspend fun clearAllRecords() {
        withContext(Dispatchers.IO) {
            db.clearAllRecords()
        }
    }
}