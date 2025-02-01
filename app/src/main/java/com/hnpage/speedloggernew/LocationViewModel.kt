package com.hnpage.speedloggernew

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.hnpage.speedloggernew.db.AppDatabase
import com.hnpage.speedloggernew.db.LocationDao
import com.hnpage.speedloggernew.global.DataInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val locationDao: LocationDao = AppDatabase.getDatabase(application).locationDao()
    val allLocations: LiveData<List<DataInterface.LocationData2>> = locationDao.getAllLocations()

    // Hàm để lưu dữ liệu vào Room DB
    fun insertLocation(location: DataInterface.LocationData2) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.insert(location)
        }
    }
}
