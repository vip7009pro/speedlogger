package com.hnpage.speedloggernew

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.hnpage.speedloggernew.ui.theme.SpeedLoggerNewTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Path
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import co.yml.charts.axis.AxisData
import co.yml.charts.ui.linechart.LineChart as YMLLineChart
import co.yml.charts.ui.linechart.model.Line as YMLLine
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.hnpage.speedloggernew.db.LocationViewModel
import com.hnpage.speedloggernew.global.DataInterface
import com.hnpage.speedloggernew.global.LocalData
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import kotlinx.coroutines.delay
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {
    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData: StateFlow<LocationData?> = _locationData.asStateFlow()
    private val _speedOffset = MutableStateFlow<Float?>(0f)
    val speedOffset: StateFlow<Float?> = _speedOffset.asStateFlow()

    fun updateLocation(speed: Float, lat: Double, lng: Double) {
        _locationData.value = LocationData(speed, lat, lng)
    }

    fun updateSpeedOffset(offSet: Float) {
        _speedOffset.value = offSet
    }
}


class MainActivity : ComponentActivity() {
    private val locationViewModel: LocationViewModel by viewModels()
    private val REQUEST_CODE_STORAGE_PERMISSIONS = 100

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Storage permissions granted")
            } else {
                Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val viewModel: MainViewModel by viewModels()
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.extras?.let {
                val speed = it.getFloat("speed", 0f)
                val lat = it.getDouble("lat", 0.0)
                val lng = it.getDouble("lng", 0.0)
                viewModel.updateLocation(speed, lat, lng)
                Log.d("MainActivity", "Received location update: $speed, $lat, $lng")
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        ///registerReceiver(locationReceiver, IntentFilter("LOCATION_UPDATE"))
        val intentFilter = IntentFilter("LOCATION_UPDATE")
        // Android 13+ yêu cầu cờ RECEIVER_NOT_EXPORTED hoặc RECEIVER_EXPORTED
        registerReceiver(
            locationReceiver,
            intentFilter,
            RECEIVER_NOT_EXPORTED // Sử dụng NOT_EXPORTED nếu chỉ nhận broadcast từ app của bạn
        )
        Log.d("MainActivity", "Receiver registered")
        viewModel.speedOffset.value?.let { LocationForegroundService.startService(this, it) }
        Log.d("MainActivity", "Attempted to start service")
        if (isAndroidAutoConnected()) {
            startCarAppService()
            Log.d("MainActivity", "CarAppService started")
        }
        //LocationForegroundService.startService(this)
        setContent {
            val locationViewModel: LocationViewModel by viewModels()
            SpeedLoggerNewTheme {
                MainScreen(viewModel = viewModel,
                    lctvm = locationViewModel,
                    onStopService = { stopService() },
                    onOpenExcelLog = { openExcelLog() },
                    onStartService = { startService() })
            }
        }
    }


    private fun startCarAppService() {
        val intent = Intent(this, CarAppService::class.java)
        startService(intent)
    }

    private fun isAndroidAutoConnected(): Boolean {
        // Logic kiểm tra kết nối Android Auto (ví dụ: sử dụng CarAppService)
        return true // Tạm thời luôn trả về true, bạn cần thêm logic thực tế
    }

    private fun stopService() {
        val intent = Intent(this, LocationForegroundService::class.java)
        stopService(intent) // Dừng service
        Log.d("MainActivity", "Service stop requested")
    }

    private fun startService() {
        viewModel.speedOffset.value?.let { LocationForegroundService.startService(this, it) }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100
            )
        } else {
            // Khởi động service nếu đã có quyền
            viewModel.speedOffset.value?.let { LocationForegroundService.startService(this, it) }
        }
    }

    private fun openExcelLog() {
        val file = File(getExternalFilesDir(null), "speed_history.xlsx")
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                this, "${applicationContext.packageName}.fileprovider", // Định nghĩa FileProvider
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Kiểm tra xem có ứng dụng nào có thể xử lý intent không
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d("MainActivity", "Excel file opened")
            } else {
                Toast.makeText(this, "No app available to open Excel files", Toast.LENGTH_SHORT)
                    .show()
                Log.e("MainActivity", "No app available to open Excel files")
            }
        } else {
            Toast.makeText(this, "Excel file not found", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "Excel file not found")
        }
    }

    override fun onDestroy() {
        unregisterReceiver(locationReceiver)
        super.onDestroy()
    }
}

fun openExcelFile(context: Context, fileName: String) {
    val contentResolver = context.contentResolver

    // Truy vấn file đã lưu trong thư mục Downloads
    val uri = contentResolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Downloads._ID),
        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
        arrayOf(fileName),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
            MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id.toString()).build()
        } else null
    }

    if (uri != null) {
        // Tạo intent để mở file với các app hỗ trợ
        Log.d("URL", uri.toString())
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Kiểm tra nếu có ứng dụng hỗ trợ mở file
        if (openIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(openIntent)
        } else {
            Toast.makeText(context, "No app found to open Excel file", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Excel file not found", Toast.LENGTH_SHORT).show()
    }
}


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    lctvm: LocationViewModel,
    onStopService: () -> Unit,
    onOpenExcelLog: () -> Unit,
    onStartService: () -> Unit
) {
    val locationData by viewModel.locationData.collectAsState()
    val speedOffsetData by viewModel.speedOffset.collectAsState()
    val context = LocalContext.current

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
        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
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
                    text = "STOP SERVICE", style = TextStyle(color = Color.White, fontSize = 10.sp)
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
                        modifier = Modifier.padding(all= 0.dp)
                    )
                }

            }





        }
        ChartLine3(lctvm)
        LocationList(lctvm)


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

fun stringToTimestamp(dateString: String): Long {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return format.parse(dateString)?.time ?: 0L
}

@Composable
fun LocationList(lctViewModel: LocationViewModel) {
    val dataList = lctViewModel.getAllSpeedRecords().collectAsState(initial = emptyList())
    val startTime: Long = stringToTimestamp("2025-02-02 00:00:00")
    val endTime: Long = stringToTimestamp("2025-02-02 23:59:59")
    //convert string datetime to long timestamp


    //val dataList = lctViewModel.getSpeedRecordsInRange(startTime, endTime).collectAsState(initial = emptyList())
    //Log.d("LocationList", dataList.value.toString())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Speed Data History",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray,
            fontStyle = FontStyle.Italic
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(dataList.value) { item ->
                LocationItem(item)
            }

        }
    }
}

fun convertTimestampToDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun LocationItem(speedData: DataInterface.LocationData2) {
    val formattedDate = convertTimestampToDateTime(speedData.timeStamp.toLong())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .background(Color.White), elevation = CardDefaults.cardElevation()
    ) {
        Column(modifier = Modifier.padding(all = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "ID: ${speedData.id}", color = Color.Gray, fontSize = 12.sp)
                Text(text = formattedDate, color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "${(speedData.speed).format(1)} km/h",
                    fontWeight = FontWeight.Bold,
                    color = Color.Blue,
                    fontSize = 12.sp
                )
               /* Text(text = "Lat: ${speedData.latitude}", color = Color.Blue, fontSize = 12.sp)
                Text(text = "Lon: ${speedData.longitude}", color = Color.Blue, fontSize = 12.sp)*/
            }
            Row(
                horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()
            ) {



            }


        }
    }
}


fun readSpeedDataFromExcel(context: Context): List<Pair<Float, Float>> {
    val speedData = mutableListOf<Pair<Float, Float>>()

    try {
        val resolver = context.contentResolver
        val uri = getExcelFileUri(context) ?: return emptyList()

        resolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0) // Sheet đầu tiên

            for (row in sheet) {
                if (row.rowNum == 0) continue // Bỏ qua dòng tiêu đề

                val timeCell = row.getCell(0) // Timestamp (chuyển sang giây)
                val speedCell = row.getCell(1) // Speed (km/h)

                val time = row.rowNum.toFloat() // Số dòng thay cho thời gian
                val speed = speedCell?.numericCellValue?.toFloat() ?: 0f

                speedData.add(time to speed)
            }
            workbook.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return speedData
}

/**
 * Lấy Uri của file Excel trong thư mục Downloads
 */
fun getExcelFileUri(context: Context): Uri? {
    val resolver = context.contentResolver
    return resolver.query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Downloads._ID),
        "${MediaStore.Downloads.DISPLAY_NAME} = ?",
        arrayOf("speed_history.xlsx"),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
            Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
        } else null
    }
}

@Composable
fun SpeedChartScreen(context: Context) {
    val speedHistory = remember { mutableStateOf(emptyList<Pair<Float, Float>>()) }

    LaunchedEffect(Unit) {
        while (true) {
            speedHistory.value = readSpeedDataFromExcel(context)
            delay(1000)
        }

    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxSpeed = speedHistory.value.maxOfOrNull { it.second } ?: 100f
            val scaleX = size.width / (speedHistory.value.size + 1)
            val scaleY = size.height / maxSpeed

            val path = Path().apply {
                moveTo(0f, size.height - (speedHistory.value.firstOrNull()?.second ?: 0f) * scaleY)
                speedHistory.value.forEach { (time, speed) ->
                    lineTo(time * scaleX, size.height - (speed * scaleY))
                }
            }

            drawPath(path, Color.Blue, style = Stroke(5f))
        }
    }
}

val axisProperties = GridProperties.AxisProperties(
    enabled = true,
    style = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f)),
    color = SolidColor(Color.Gray),
    thickness = (.5).dp,
    lineCount = 5
)

val lineProperties = LineProperties(
    enabled = true,
    style = StrokeStyle.Dashed(intervals = floatArrayOf(10f, 10f)),
    color = SolidColor(Color.Gray),
    thickness = (.5).dp,
)
val labelHelperProperties = LabelHelperProperties(
    enabled = true, textStyle = TextStyle.Default.copy(fontSize = 10.sp)
)

@Composable
fun SpeedChartNew(data: List<Double>) {
    //Log.d("SpeedChartNewData", data.toString())

    LineChart(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp), data = listOf(
            Line(
                label = "Speed",
                values = data,
                color = SolidColor(Color(0xFF23af92)),
                firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                secondGradientFillColor = Color.Transparent,
                //strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                //gradientAnimationDelay = 1000,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
            )
        )/* , animationMode = AnimationMode.Together(delayBuilder = {
             it * 500L
         })*/, labelHelperProperties = labelHelperProperties
    )
}


@Composable
fun ChartLine2(speedData: List<Pair<Float, Float>>) {
    //convert speedData to list of Point
    val pointsData: List<Point> = speedData.map { Point(it.first, it.second) }.takeLast(10)
    //Log.d("datalist point",pointsData.toString())
    val maxSpeed = speedData.maxOfOrNull { it.second.toInt() }?.plus(1)
    val steps = 5
    //Log.d("steps", steps.toString())
    //get screen width in dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

    val xAxisData = AxisData.Builder().axisStepSize(screenWidthDp / 10).backgroundColor(Color.White)
        .steps(pointsData.size - 1).labelData { i -> i.toString() }.labelAndAxisLinePadding(15.dp)
        .axisLabelAngle(90f).axisLabelColor(Color.Black).axisLineColor(Color.Black)
        .axisLabelFontSize(10.sp).shouldDrawAxisLineTillEnd(flag = true).build()

    val yAxisData = steps.let {
        AxisData.Builder().steps(it).backgroundColor(Color.White).labelAndAxisLinePadding(20.dp)
            .labelData { i ->
                val yScale = maxSpeed?.div(steps)
                (i * yScale!!).toString()
            }.build()
    }

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                YMLLine(
                    dataPoints = pointsData,
                    LineStyle(color = Color.Green),
                    IntersectionPoint(radius = 1.dp),
                    SelectionHighlightPoint(),
                    ShadowUnderLine(),
                    SelectionHighlightPopUp()
                )
            ),
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(),
        backgroundColor = Color.White,
        isZoomAllowed = true
    )

    if (pointsData.isNotEmpty()) YMLLineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp), lineChartData = lineChartData
    )
}

@Composable
fun ChartLine3(lctViewModel: LocationViewModel) {
    val dataList = lctViewModel.getAllSpeedRecords().collectAsState(initial = emptyList())
    //Log.d("chartline3", dataList.value.toString())


    //convert speedData to list of Point
    val pointsData: List<Point> =
        dataList.value.take(20).map { Point(it.id.toFloat(), it.speed.toFloat()) }
    //Log.d("datalist point",pointsData.toString())
    val maxSpeed = dataList.value.take(20).maxOfOrNull { it.speed.toInt() }?.plus(1)
    val steps = 5
    //Log.d("steps", steps.toString())
    //get screen width in dp
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp

    val xAxisData = AxisData.Builder().bottomPadding(100.dp).axisStepSize(screenWidthDp / 25)
        .backgroundColor(Color.White).steps(pointsData.size - 1).labelData { i ->
            convertTimestampToDateTime(dataList.value[i].timeStamp.toLong())
        }.axisLabelAngle(90f).axisLabelColor(Color.Black).axisLineColor(Color.Black)
        .axisLabelFontSize(5.sp).shouldDrawAxisLineTillEnd(flag = true)
        .labelAndAxisLinePadding(1.dp).build()

    val yAxisData = steps.let {
        AxisData.Builder().steps(it).backgroundColor(Color.White).labelAndAxisLinePadding(10.dp)
            .axisLabelFontSize(10.sp).labelData { i ->
                val yScale = maxSpeed?.div(steps)
                (i * yScale!!).toString()
            }.build()
    }

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                YMLLine(
                    dataPoints = pointsData,
                    LineStyle(color = Color.Green),
                    IntersectionPoint(radius = 1.dp),
                    SelectionHighlightPoint(),
                    ShadowUnderLine(),
                    SelectionHighlightPopUp()
                )
            ),
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(),
        backgroundColor = Color.White,
        isZoomAllowed = true
    )

    if (pointsData.isNotEmpty()) YMLLineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp), lineChartData = lineChartData
    )
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Float.format(digits: Int) = "%.${digits}f".format(this)