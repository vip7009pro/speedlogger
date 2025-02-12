package com.hnpage.speedloggernew.db

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hnpage.speedloggernew.global.DataInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LocationRepository = LocationRepository(application)
    suspend fun insertSpeedRecord(record: DataInterface.LocationData2) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSpeedRecord(record)
        }
    }

    fun getAllSpeedRecords(): Flow<List<DataInterface.LocationData2>> {
        return repository.getAllSpeedRecords
    }

    fun getSpeedRecordsInRange(
        startTime: Long, endTime: Long
    ): Flow<List<DataInterface.LocationData2>> {
        return repository.getSpeedRecordsInRange(startTime, endTime)
    }


    fun clearAllRecords() {
        viewModelScope.launch {
            repository.clearAllRecords()
        }
    }


}
