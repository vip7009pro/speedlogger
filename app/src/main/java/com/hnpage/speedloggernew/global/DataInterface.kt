package com.hnpage.speedloggernew.global

import androidx.room.Entity
import androidx.room.PrimaryKey

class DataInterface {
    @Entity(tableName = "location_data")
    data class LocationData2(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val timeStamp: String,
        val speed: Float,
        val latitude: Double,
        val longitude: Double,
    )
}