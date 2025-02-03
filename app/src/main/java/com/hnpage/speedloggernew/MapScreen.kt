package com.hnpage.speedloggernew

import androidx.compose.ui.graphics.Color
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.hnpage.speedloggernew.global.DataInterface

@Composable
fun MapScreen(locations: List<DataInterface.LocationData2>) {
    val homelocation = LatLng(21.2098921, 105.8670834) // Một vị trí mặc định
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            locations.lastOrNull()?.toLatLng() ?: homelocation,
            15f
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        // Hiển thị tất cả các điểm đã lưu trong DB
        locations.forEach { location ->
            Marker(
                state = MarkerState(position = location.toLatLng()),
                title = "Time:${convertTimestampToDateTime(location.timeStamp.toLong())} Speed: ${location.speed} km/h"
            )
        }

        locations.forEach { location ->
            Circle(
                center = LatLng(location.latitude, location.longitude),
                fillColor = Color.Red.copy(alpha = 0.8f), // Màu đỏ có độ trong suốt
                strokeColor = Color.Transparent, // Không có viền
                radius = 3.0 // Bán kính 3 mét
            )
        }

        // Vẽ đường nối các điểm
        /*Polyline(
            points = locations.map { it.toLatLng() },
            color = Color.Red,
            width = 5f
        )*/
    }
}

// Hàm chuyển đổi dữ liệu từ Room thành LatLng
fun DataInterface.LocationData2.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}
