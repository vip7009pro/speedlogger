package com.hnpage.speedloggernew.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.hnpage.speedloggernew.global.DataInterface
import com.hnpage.speedloggernew.utils.GlobalFunction
import kotlin.math.max

fun getSpeedColor(speed: Float): Color {
    return when {
        speed < 20 -> Color.Blue  // Chậm
        speed < 40 -> Color.Green // Bình thường
        speed < 60 -> Color.Yellow // Nhanh
        else -> Color.Red // Rất nhanh
    }
}

@Composable
fun MapScreen(locations: List<DataInterface.LocationData2>) {
    val showHideMap = remember { mutableStateOf(false) }
    val homelocation = LatLng(21.2098921, 105.8670834) // Một vị trí mặc định
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            locations.lastOrNull()?.toLatLng() ?: homelocation, 15f
        )
    }
    if(locations.isNotEmpty())
    {
        val lastLocation = locations.last()
        cameraPositionState.move(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(lastLocation.latitude, lastLocation.longitude), 15f
            )
        )
    }


    Column(Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                showHideMap.value = !showHideMap.value
            }) {
                Text(text="Show Map")
            }
            Button(
                onClick = {
                    if (locations.isNotEmpty()) {
                        val lastLocation2 = locations.last()
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastLocation2.latitude, lastLocation2.longitude), 15f
                            )
                        )
                    }
                },
            ) {
                Text(text = "Focus Last Marker")
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState
        ) {
            // Hiển thị tất cả các điểm đã lưu trong DB
            /*locations.forEach { location ->
                Marker(
                    state = MarkerState(position = location.toLatLng()),
                    title = "Time:${convertTimestampToDateTime(location.timeStamp.toLong())} Speed: ${location.speed} km/h",
                )
            }

            // Vẽ đường nối các điểm
            if (locations.size > 1) {
                for (i in 0 until locations.size - 1) {
                    val start = LatLng(locations[i].latitude, locations[i].longitude)
                    val end = LatLng(locations[i + 1].latitude, locations[i + 1].longitude)
                    val speed = locations[i].speed

                    Polyline(
                        points = listOf(start, end),
                        color = getSpeedColor(speed),
                        width = 10f
                    )
                }
            }
    */

            if (locations.size > 1) {
                // Vẽ đường nối giữa các điểm
                val step =
                    max(1, locations.size / 500) // Giảm số điểm hiển thị (chỉ lấy 500 điểm tối đa)
                for (i in locations.indices step step) {
                    val start = LatLng(locations[i].latitude, locations[i].longitude)
                    if (i + step < locations.size) {
                        val end =
                            LatLng(locations[i + step].latitude, locations[i + step].longitude)
                        Polyline(
                            points = listOf(start, end),
                            color = getSpeedColor(locations[i].speed),
                            width = 10f
                        )
                    }
                }
            }

            // Hiển thị các điểm vị trí dưới dạng Marker, nhưng chỉ lấy mẫu
            val sampleStep = max(1, locations.size / 500) // Chỉ hiển thị tối đa 500 điểm
            locations.filterIndexed { index, _ -> index % sampleStep == 0 }.forEach { location ->
                Marker(
                    state = MarkerState(position = location.toLatLng()),
                    title = "Time:${GlobalFunction().convertTimestampToDateTime(location.timeStamp.toLong())} Speed: ${location.speed} km/h",
                )
            }
        }


    }


}

// Hàm chuyển đổi dữ liệu từ Room thành LatLng
fun DataInterface.LocationData2.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}
