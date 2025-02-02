package com.hnpage.speedloggernew.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hnpage.speedloggernew.LocationData
import com.hnpage.speedloggernew.global.DataInterface
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    // Lưu dữ liệu tốc độ và tọa độ
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Ghi đè nếu trùng ID
    suspend fun insert(location: DataInterface.LocationData2)

    // Lấy tất cả dữ liệu
    @Query("SELECT * FROM location_data ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<DataInterface.LocationData2>>

    @Query("SELECT * FROM location_data WHERE timeStamp BETWEEN :startTime AND :endTime ORDER BY timeStamp DESC")
    fun getLocationsInRange(startTime: Long, endTime: Long): Flow<List<DataInterface.LocationData2>>

    // Lấy dữ liệu theo ID
    @Query("SELECT * FROM location_data WHERE id = :id")
    fun getLocationById(id: Long): LiveData<DataInterface.LocationData2>

    @Query("DELETE FROM location_data")
    suspend fun clearAllRecords()
}
