package com.hnpage.speedloggernew.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnpage.speedloggernew.MainViewModel
import com.hnpage.speedloggernew.components.UIComponents
import com.hnpage.speedloggernew.db.LocationViewModel
import com.hnpage.speedloggernew.format
import com.hnpage.speedloggernew.global.DataInterface
import com.hnpage.speedloggernew.global.LocalData
import com.hnpage.speedloggernew.services.LocationForegroundService
import com.hnpage.speedloggernew.utils.GlobalFunction
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.systemBarsPadding // Thêm import này

class SpeedLogScreens {
    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun MainScreen(
        viewModel: MainViewModel,
        lctvm: LocationViewModel,
        onStopService: () -> Unit,
        onStartService: () -> Unit
    ) {
        val locationData by viewModel.locationData.collectAsState()
        val speedOffsetData by viewModel.speedOffset.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(key1 = 1) {
            val savedOffset: String = LocalData().getData(context, "speedoffset")
            if (savedOffset != "") viewModel.updateSpeedOffset(savedOffset.toFloat())
            viewModel.speedOffset.value?.let { LocationForegroundService.startService(context, it) }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(color = Color.Gray)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(5.dp)
                .systemBarsPadding() // Thêm modifier này để tránh bị che bởi thanh thông báo
        ) {
            // Speed and Location Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${locationData?.speed?.times(3.6)?.format(2)} km/h",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Lat: ${locationData?.lat?.format(4)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Lng: ${locationData?.lng?.format(4)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Service Control Buttons Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f).height(40.dp).padding(end = 8.dp),
                        onClick = onStartService,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "START SERVICE", fontSize = 14.sp)
                    }
                    Button(
                        modifier = Modifier.weight(1f).height(40.dp).padding(start = 8.dp),
                        onClick = onStopService,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "STOP SERVICE", fontSize = 14.sp)
                    }
                }
            }

            // Speed Offset Adjustment Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Speed Offset: %.2f km/h".format(viewModel.speedOffset.value),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        speedOffsetData?.let { offset ->
                            Slider(
                                value = offset,
                                onValueChange = { newValue ->
                                    viewModel.updateSpeedOffset(newValue)
                                },
                                onValueChangeFinished = {
                                    viewModel.speedOffset.value?.let {
                                        LocationForegroundService.startService(context, it)
                                        LocalData().saveData(context, "speedoffset", it.toString())
                                    }

                                },
                                valueRange = -10f..10f,
                                steps = 200,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.width(5.dp))
                        Button(
                            modifier = Modifier.width(80.dp).height(36.dp).padding(0.dp),
                            onClick = {
                                viewModel.updateSpeedOffset(0f)
                                viewModel.speedOffset.value?.let {
                                    LocationForegroundService.startService(context, 0f)
                                }
                                LocalData().saveData(context, "speedoffset", 0.toString())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "RESET", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Charts and Map
            UIComponents().ChartLine3(lctvm)
            Spacer(Modifier.height(16.dp))
            ShowMap(lctvm)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ShowMap(lctViewModel: LocationViewModel) {
        // Khởi tạo ngày giờ mặc định
        var selectedFromDateTime by remember { mutableStateOf(GlobalFunction().getCurrentDate("yyyy-MM-dd 00:00:00")) }
        var selectedToDateTime by remember { mutableStateOf(GlobalFunction().getCurrentDate("yyyy-MM-dd 23:59:59")) }

        // Trạng thái hiển thị dialog chọn ngày và giờ
        var showFromDatePicker by remember { mutableStateOf(false) }
        var showFromTimePicker by remember { mutableStateOf(false) }
        var showToDatePicker by remember { mutableStateOf(false) }
        var showToTimePicker by remember { mutableStateOf(false) }

        // Trạng thái cho DatePicker và TimePicker
        val fromDatePickerState = rememberDatePickerState()
        val fromTimePickerState = rememberTimePickerState(initialHour = 0, initialMinute = 0, is24Hour = false)
        val toDatePickerState = rememberDatePickerState()
        val toTimePickerState = rememberTimePickerState(initialHour = 23, initialMinute = 59, is24Hour = false)

        // Lấy danh sách dữ liệu trong khoảng thời gian
        val dataList = lctViewModel.getSpeedRecordsInRange(
            GlobalFunction().stringToTimestamp(selectedFromDateTime),
            GlobalFunction().stringToTimestamp(selectedToDateTime)
        ).collectAsState(initial = emptyList())

        // Danh sách dữ liệu vị trí
        var locationDataList = remember { mutableStateListOf<DataInterface.LocationData2>() }
        var showDialog by remember { mutableStateOf(false) }

        // Giao diện chính
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tiêu đề
            Text(
                text = "Select Date Range",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 20.sp
            )

            // Nút chọn From DateTime và To DateTime
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showFromDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "From Date",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp
                    )
                }
                Button(
                    onClick = { showToDatePicker = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "To Date",
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 16.sp
                    )
                }
            }

            // Khoảng cách
            Spacer(modifier = Modifier.height(16.dp))

            // Hiển thị ngày giờ đã chọn
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "From: $selectedFromDateTime",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To: $selectedToDateTime",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Khoảng cách trước bản đồ
            Spacer(modifier = Modifier.height(16.dp))

            // Bản đồ
            MapScreen(dataList.value)
        }

        // Dialog chọn ngày cho From Date
        if (showFromDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showFromDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showFromDatePicker = false
                            showFromTimePicker = true
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showFromDatePicker = false }) { Text("Hủy") }
                }
            ) {
                DatePicker(state = fromDatePickerState)
            }
        }

        // Dialog chọn giờ cho From Date
        if (showFromTimePicker) {
            DatePickerDialog(
                onDismissRequest = { showFromTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            fromDatePickerState.selectedDateMillis?.let { dateMillis ->
                                val dateTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(dateMillis),
                                    ZoneId.systemDefault()
                                ).withHour(fromTimePickerState.hour)
                                    .withMinute(fromTimePickerState.minute)

                                selectedFromDateTime = dateTime.format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                )
                            }
                            showFromTimePicker = false
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showFromTimePicker = false }) { Text("Hủy") }
                }
            ) {
                TimePicker(state = fromTimePickerState)
            }
        }

        // Dialog chọn ngày cho To Date
        if (showToDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showToDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showToDatePicker = false
                            showToTimePicker = true
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showToDatePicker = false }) { Text("Hủy") }
                }
            ) {
                DatePicker(state = toDatePickerState)
            }
        }

        // Dialog chọn giờ cho To Date
        if (showToTimePicker) {
            DatePickerDialog(
                onDismissRequest = { showToTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            toDatePickerState.selectedDateMillis?.let { dateMillis ->
                                val dateTime = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(dateMillis),
                                    ZoneId.systemDefault()
                                ).withHour(toTimePickerState.hour)
                                    .withMinute(toTimePickerState.minute)

                                selectedToDateTime = dateTime.format(
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                )
                            }
                            showToTimePicker = false
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showToTimePicker = false }) { Text("Hủy") }
                }
            ) {
                TimePicker(state = toTimePickerState)
            }
        }
    }


}