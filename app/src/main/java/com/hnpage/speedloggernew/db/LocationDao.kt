package com.hnpage.speedloggernew.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hnpage.speedloggernew.LocationData
import com.hnpage.speedloggernew.global.DataInterface

@Dao
interface LocationDao {

    // Lưu dữ liệu tốc độ và tọa độ
    @Insert
    suspend fun insert(location: DataInterface.LocationData2)

    // Lấy tất cả dữ liệu
    @Query("SELECT * FROM location_data ORDER BY timestamp DESC")
    fun getAllLocations(): LiveData<List<DataInterface.LocationData2>>

    // Lấy dữ liệu theo ID
    @Query("SELECT * FROM location_data WHERE id = :id")
    fun getLocationById(id: Long): LiveData<DataInterface.LocationData2>
}
