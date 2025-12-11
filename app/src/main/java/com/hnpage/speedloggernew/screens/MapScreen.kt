package com.hnpage.speedloggernew.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.hnpage.speedloggernew.global.DataInterface
import com.hnpage.speedloggernew.services.LocationUploader
import com.hnpage.speedloggernew.utils.GlobalFunction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

// Màu đường theo tốc độ – đẹp, chuẩn Material You
private fun getSpeedColor(speedKmh: Float): Color {
    return when {
        speedKmh < 20 -> Color(0xFF2196F3)  // Xanh dương
        speedKmh < 40 -> Color(0xFF4CAF50)  // Xanh lá
        speedKmh < 70 -> Color(0xFFFFC107)  // Vàng
        speedKmh < 100 -> Color(0xFFFF5722) // Cam
        else -> Color(0xFFE91E63)           // Đỏ hồng
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(locations: List<DataInterface.LocationData2>) {
    val context = LocalContext.current
    var isMapVisible by remember { mutableStateOf(true) }
    var followLastLocation by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            locations.lastOrNull()?.toLatLng() ?: LatLng(21.0285, 105.8542), 15f
        )
    }

    // Tự động di chuyển camera khi có điểm mới và đang bật theo dõi
    LaunchedEffect(locations.lastOrNull(), followLastLocation) {
        if (locations.isNotEmpty() && followLastLocation) {
            val last = locations.first()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(last.toLatLng(), 16f),
                durationMs = 800
            )
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ==================== HEADER SIÊU GỌN + THOÁNG ====================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thông tin hành trình
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hành trình đã ghi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${locations.size} điểm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }

                    // Nút Sync nhỏ gọn, bo tròn, nổi bật
                    Button(
                        onClick = {
                            if (!isSyncing) {
                                isSyncing = true
                                locations.forEach { loc ->
                                    LocationUploader.sendLocation2(
                                        loc.latitude, loc.longitude,
                                        GlobalFunction().convertTimestampToDateTime2(loc.timeStamp.toLong()),
                                        loc.speed
                                    ) {}
                                }
                                coroutineScope.launch {
                                    delay(3000)
                                    isSyncing = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSyncing) Color(0xFF666666) else Color(0xFF00E676)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Đang sync", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ==================== CONTROL BAR SIÊU GỌN – CHỈ 1 DÒNG ====================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút Ẩn/Hiện bản đồ → chỉ còn icon tròn nhỏ xinh
                IconButton(
                    onClick = { isMapVisible = !isMapVisible },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isMapVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Nút Theo dõi điểm mới → gọn thành icon + text ngắn
                FilterChip(
                    onClick = { followLastLocation = !followLastLocation },
                    selected = followLastLocation,
                    label = { Text("Theo dõi mới", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (followLastLocation) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                Spacer(Modifier.weight(1f))

                // Nếu cần thêm nút khác thì để đây
            }

            // ==================== BẢN ĐỒ CHIẾM TỐI ĐA DIỆN TÍCH ====================
            AnimatedVisibility(visible = isMapVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.weight(1f)) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            compassEnabled = true
                        )
                    ) {
                        // [Giữ nguyên phần vẽ Polyline + Marker như cũ của đại ca]
                        if (locations.isNotEmpty()) {
                            val step = max(1, locations.size / 400)
                            for (i in locations.indices step step) {
                                if (i + step < locations.size) {
                                    val start = locations[i].toLatLng()
                                    val end = locations[i + step].toLatLng()
                                    val speedKmh = locations[i].speed * 3.6f
                                    Polyline(points = listOf(start, end), color = getSpeedColor(speedKmh), width = 12f)
                                }
                            }
                            locations.filterIndexed { index, _ -> index % step == 0 }.forEach { loc ->
                                val speedKmh = (loc.speed * 3.6f).toInt()
                                Marker(
                                    state = MarkerState(position = loc.toLatLng()),
                                    title = "Tốc độ: $speedKmh km/h",
                                    snippet = GlobalFunction().convertTimestampToDateTime(loc.timeStamp.toLong())
                                )
                            }
                        }
                    }

                    // FAB nhỏ gọn ở góc phải dưới
                    if (followLastLocation && locations.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                val last = locations.first()
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(last.toLatLng(), 17f),
                                        800
                                    )
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(20.dp)
                                .size(52.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

// Extension vẫn giữ nguyên
fun DataInterface.LocationData2.toLatLng() = LatLng(latitude, longitude)