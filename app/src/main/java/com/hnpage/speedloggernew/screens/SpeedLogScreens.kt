package com.hnpage.speedloggernew.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.hnpage.speedloggernew.global.LocalData
import com.hnpage.speedloggernew.services.LocationForegroundService

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
        val isShowMap = remember { mutableStateOf(false) }

        val speedHistory = remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }
        val listOfValueFromSpeedHistory = remember { mutableStateOf(emptyList<Double>()) }



        LaunchedEffect(Unit) {
            /* while (true) {
                 speedHistory.value = readSpeedDataFromExcel(context)
                 //get list of speed from speedHistory and save into listOfValueFromSpeedHistory
                 listOfValueFromSpeedHistory.value = emptyList()
                 //reset listOfValueFromSpeedHistory to list of speed from speedHistory
                 speedHistory.value.forEach {
                     listOfValueFromSpeedHistory.value += it.second.toDouble()
                 }
                 //Log.d("SpeedList", listOfValueFromSpeedHistory.value.toString())
                 delay(1000)
             }*/

        }

        //Log.d("SpeedHistory", speedHistory.value.toString())
        //Log.d("SpeedList", listOfValueFromSpeedHistory.value.toString())


        LaunchedEffect(key1 = 1) {
            val savedOffset: String = LocalData().getData(context, "speedoffset")
            if (savedOffset != "") viewModel.updateSpeedOffset(savedOffset.toFloat())
            viewModel.speedOffset.value?.let { LocationForegroundService.startService(context, it) }
        }
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(1f)
        ) {
            Text(
                text = "${locationData?.speed?.times(3.6)?.format(2)} km/h",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Red

            )
            Row(
                horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Lat: ${locationData?.lat}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Blue
                )
                Text(
                    text = "Lng: ${locationData?.lng}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Blue
                )
            }


            Row(
                horizontalArrangement = Arrangement.Absolute.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier
                        .size(120.dp, 30.dp)
                        .fillMaxWidth(),
                    onClick = onStartService,
                    colors = ButtonColors(
                        containerColor = Color.Green,
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray,
                        disabledContainerColor = Color.DarkGray
                    )
                ) {
                    Text(
                        text = "START SERVICE",
                        style = TextStyle(color = Color.DarkGray, fontSize = 10.sp)
                    )
                }
                Button(
                    modifier = Modifier
                        .size(120.dp, 30.dp)
                        .fillMaxWidth(),
                    onClick = onStopService,
                    colors = ButtonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray,
                        disabledContainerColor = Color.DarkGray
                    )
                ) {
                    Text(
                        text = "STOP SERVICE",
                        style = TextStyle(color = Color.White, fontSize = 10.sp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Column {
                Row {
                    speedOffsetData?.let { it ->
                        Slider(value = it, onValueChange = { newValue ->
                            run {

                                viewModel.updateSpeedOffset(newValue)/*viewModel.speedOffset.value?.let {
                                LocationForegroundService.startService(
                                    context, newValue
                                )
                            }
                            LocalData().saveData(context, "speedoffset", newValue.toString())*/
                            }

                        }, onValueChangeFinished = {
                            //Log.d("newOffset", it.toString())
                            viewModel.speedOffset.value?.let {
                                LocationForegroundService.startService(
                                    context, it
                                )
                            }
                            LocalData().saveData(context, "speedoffset", it.toString())
                        }, valueRange = -10f..10f, // Adjust range as needed
                            steps = 200, // Optional: Adds steps for a more granular control
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .height(20.dp)
                                .fillMaxWidth(0.5f)
                        )
                    }
                    Text(text = "%.2f km/h".format(viewModel.speedOffset.value), fontSize = 10.sp)
                    Button(
                        modifier = Modifier
                            .size(100.dp, 30.dp)
                            .fillMaxWidth(), onClick = {
                            viewModel.updateSpeedOffset(0f)
                            viewModel.speedOffset.value?.let {
                                LocationForegroundService.startService(
                                    context, 0f
                                )
                            }
                            LocalData().saveData(context, "speedoffset", 0.toString())
                        }, colors = ButtonColors(
                            containerColor = Color.Gray,
                            contentColor = Color.White,
                            disabledContentColor = Color.Gray,
                            disabledContainerColor = Color.DarkGray
                        )
                    ) {
                        Text(
                            text = "RESET",
                            style = TextStyle(color = Color.White, fontSize = 10.sp),
                            modifier = Modifier.padding(all = 0.dp)
                        )
                    }

                }

                UIComponents().ChartLine3(lctvm)
                ShowMap(lctvm)


            }


            //SpeedChartScreen(context= LocalContext.current)
            //SpeedChartNew(listOfValueFromSpeedHistory.value)
            /*if(speedHistory.value.isNotEmpty())
            ChartLine2(speedHistory.value)*/

            /*Button(onClick = {
                openExcelFile(context, "speed_history.xlsx")
            }) {
                Text("Open Excel File")
            }*/
        }

    }

    @Composable
    fun ShowMap(lctViewModel: LocationViewModel) {
        val dataList = lctViewModel.getAllSpeedRecords().collectAsState(initial = emptyList())
        MapScreen(dataList.value)
    }


}