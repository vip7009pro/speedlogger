package com.hnpage.speedloggernew.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnpage.speedloggernew.MainViewModel
import com.hnpage.speedloggernew.components.UIComponents
import com.hnpage.speedloggernew.db.LocationViewModel
import com.hnpage.speedloggernew.format
import com.hnpage.speedloggernew.global.LocalData
import com.hnpage.speedloggernew.services.LocationForegroundService
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
class SpeedLogScreens {

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun MainScreen(
        viewModel: MainViewModel,
        lctvm: LocationViewModel,
        onStartService: () -> Unit,
        onStopService: () -> Unit
    ) {
        val context = LocalContext.current
        val locationData by viewModel.locationData.collectAsState()
        val speedOffset by viewModel.speedOffset.collectAsState()
        val isDarkTheme = isSystemInDarkTheme()

        // Load saved offset khi mở app
        LaunchedEffect(Unit) {
            val saved = LocalData().getData(context, "speedoffset")
            if (saved.isNotEmpty()) {
                viewModel.updateSpeedOffset(saved.toFloat())
            }
        }

        // Background gradient đẹp lung linh
        val backgroundBrush = if (isDarkTheme) {
            Brush.verticalGradient(listOf(Color(0xFF0F1117), Color(0xFF1A1C25)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF5F7FA), Color(0xFFE3EAF0)))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush)
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ==================== CARD TỐC ĐỘ HIỆN TẠI ====================
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = colorScheme.surfaceColorAtElevation(6.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TỐC ĐỘ HIỆN TẠI",
                            style = typography.labelMedium,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${locationData?.speed?.times(3.6)?.format(1) ?: "--"}",
                            style = typography.displayLarge.copy(
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colorScheme.primary
                        )
                        Text(
                            text = "km/h",
                            style = typography.headlineSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            InfoItem("Lat", locationData?.lat?.format(6) ?: "--")
                            InfoItem("Lng", locationData?.lng?.format(6) ?: "--")
                        }
                    }
                }

                // ==================== NÚT START / STOP ====================
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onStartService,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text("START SERVICE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onStopService,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336),
                            contentColor = Color.White
                        )
                    ) {
                        Text("STOP SERVICE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // ==================== HIỆU CHỈNH TỐC ĐỘ – PHIÊN BẢN CỰC PHẨM ====================
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = colorScheme.surfaceColorAtElevation(6.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {

                        Text(
                            text = "Hiệu chỉnh tốc độ",
                            style = typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )

                        Spacer(Modifier.height(24.dp))

                        // Dòng 1: Giá trị offset + đơn vị + nút Reset (rộng rãi, cân đối)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Giá trị offset to bự trong card tròn
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
                                modifier = Modifier.size(width = 120.dp, height = 72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = "%.1f".format(speedOffset ?: 0f),
                                        style = typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Text(
                                text = "km/h",
                                style = typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )

                            // Nút Reset bự tổ chảng, hiện full chữ
                            Button(
                                onClick = {
                                    viewModel.updateSpeedOffset(0f)
                                    LocationForegroundService.startService(context, 0f)
                                    LocalData().saveData(context, "speedoffset", "0")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF5252), // Đỏ nổi bật cho Reset
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(60.dp)
                            ) {
                                Text(
                                    text = "RESET",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Dòng 2: Slider chiếm nguyên chiều ngang, to mọng, kéo đã tay
                        Slider(
                            value = speedOffset ?: 0f,
                            onValueChange = { viewModel.updateSpeedOffset(it) },
                            onValueChangeFinished = {
                                speedOffset?.let {
                                    LocationForegroundService.startService(context, it)
                                    LocalData().saveData(context, "speedoffset", it.toString())
                                }
                            },
                            valueRange = -15f..15f,
                            steps = 59,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp), // Slider cao hơn, bấm dễ hơn
                            colors = SliderDefaults.colors(
                                thumbColor = colorScheme.primary,
                                activeTrackColor = colorScheme.primary,
                                inactiveTrackColor = colorScheme.outlineVariant,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            thumb = {
                                // Thumb to hơn, có số hiện trên đầu (cực xịn)
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "%.1f".format(speedOffset ?: 0f),
                                        color = colorScheme.onPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        )

                        // Thanh chỉ thị -15 | 0 | +15
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("-15", fontSize = 15.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("0", fontSize = 16.sp, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text("+15", fontSize = 15.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // ==================== BIỂU ĐỒ & BẢNH BẢN ĐỒ ====================
                UIComponents().ChartLine3(lctvm)
                Spacer(Modifier.height(8.dp))
                ShowMap(lctvm)
            }
        }
    }

    // Component nhỏ hiển thị Lat/Lng
    @Composable
    private fun InfoItem(label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = typography.titleMedium, fontWeight = FontWeight.Medium)
        }
    }

    // Phần ShowMap cũng được làm lại đẹp hơn (em để ngắn gọn, chỉ phần giao diện chính)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShowMap(lctViewModel: LocationViewModel) {
        // Khởi tạo ngay giá trị mặc định hôm nay, không để rỗng bao giờ
        val today = java.time.LocalDate.now()
        var fromDateTime by remember {
            mutableStateOf(today.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        }
        var toDateTime by remember {
            mutableStateOf(today.atTime(23, 59, 59).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        }

        // State cho dialog
        var showFromDatePicker by remember { mutableStateOf(false) }
        var showFromTimePicker by remember { mutableStateOf(false) }
        var showToDatePicker by remember { mutableStateOf(false) }
        var showToTimePicker by remember { mutableStateOf(false) }

        val fromDateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        val fromTimeState = rememberTimePickerState(initialHour = 0, initialMinute = 0)
        val toDateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        val toTimeState = rememberTimePickerState(initialHour = 23, initialMinute = 59)

        // Chỉ gọi query khi chắc chắn có thời gian hợp lệ
        val records by lctViewModel.getSpeedRecordsInRange(
            com.hnpage.speedloggernew.utils.GlobalFunction().stringToTimestamp(fromDateTime),
            com.hnpage.speedloggernew.utils.GlobalFunction().stringToTimestamp(toDateTime)
        ).collectAsState(initial = emptyList())

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Bản đồ hành trình",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                // 2 nút chọn thời gian đẹp hơn
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showFromDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Từ: ${fromDateTime.substring(0, 16)}", fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = { showToDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Đến: ${toDateTime.substring(0, 16)}", fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Bản đồ
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                ) {
                    MapScreen(records)
                }
            }
        }

        // ================== CÁC DIALOG CHỌN NGÀY GIỜ ==================
        if (showFromDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showFromDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showFromDatePicker = false
                        showFromTimePicker = true
                    }) { Text("Chọn giờ") }
                }
            ) {
                DatePicker(state = fromDateState)
            }
        }

        if (showFromTimePicker) {
            DatePickerDialog(
                onDismissRequest = { showFromTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        fromDateState.selectedDateMillis?.let { millis ->
                            val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                                .withHour(fromTimeState.hour)
                                .withMinute(fromTimeState.minute)
                            fromDateTime = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        }
                        showFromTimePicker = false
                    }) { Text("OK") }
                }
            ) {
                TimePicker(state = fromTimeState)
            }
        }

        // Tương tự cho To Date/Time (đại ca copy y hệt, chỉ đổi tên biến là xong)
        if (showToDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showToDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showToDatePicker = false
                        showToTimePicker = true
                    }) { Text("Chọn giờ") }
                }
            ) {
                DatePicker(state = toDateState)
            }
        }

        if (showToTimePicker) {
            DatePickerDialog(
                onDismissRequest = { showToTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        toDateState.selectedDateMillis?.let { millis ->
                            val localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
                                .withHour(toTimeState.hour)
                                .withMinute(toTimeState.minute)
                            toDateTime = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        }
                        showToTimePicker = false
                    }) { Text("OK") }
                }
            ) {
                TimePicker(state = toTimeState)
            }
        }
    }
}