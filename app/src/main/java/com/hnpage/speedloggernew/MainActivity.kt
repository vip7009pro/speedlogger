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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

class MainViewModel : ViewModel() {
    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData: StateFlow<LocationData?> = _locationData.asStateFlow()

    fun updateLocation(speed: Float, lat: Double, lng: Double) {
        _locationData.value = LocationData(speed, lat, lng)
    }
}



class MainActivity : ComponentActivity() {
    private val locationViewModel: LocationViewModel by viewModels()
    private val REQUEST_CODE_STORAGE_PERMISSIONS = 100
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
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
        LocationForegroundService.startService(this)
        Log.d("MainActivity", "Attempted to start service")
        if (isAndroidAutoConnected()) {
            startCarAppService()
            Log.d("MainActivity", "CarAppService started")
        }
        //LocationForegroundService.startService(this)
        setContent {
            SpeedLoggerNewTheme  { MainScreen(viewModel = viewModel, onStopService = {stopService()}, onOpenExcelLog = {openExcelLog()}, onStartService ={startService()} ) }
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
        LocationForegroundService.startService(this)
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            // Khởi động service nếu đã có quyền
            LocationForegroundService.startService(this)
        }
    }
    private fun openExcelLog() {
        val file = File(getExternalFilesDir(null), "speed_history.xlsx")
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider", // Định nghĩa FileProvider
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Kiểm tra xem có ứng dụng nào có thể xử lý intent không
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d("MainActivity", "Excel file opened")
            } else {
                Toast.makeText(this, "No app available to open Excel files", Toast.LENGTH_SHORT).show()
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


@Composable
fun MainScreen(viewModel: MainViewModel, onStopService: () -> Unit, onOpenExcelLog: () -> Unit, onStartService: () -> Unit) {
    val locationData by viewModel.locationData.collectAsState()
    val context = LocalContext.current

    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp).fillMaxWidth(1f)) {
        Text(text = "Speed: ${locationData?.speed?.times(3.6)?.format(2)} km/h", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Lat: ${locationData?.lat}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Lng: ${locationData?.lng}", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onStopService) {
            Text(text = "STOP SERVICE", style = TextStyle(color = Color.Blue))
        }
        Button(onClick =  onStartService) {
            Text(text = "START SERVICE", style = TextStyle(color = Color.Blue))
        }
        SpeedChartScreen(context= LocalContext.current)

        /*Button(onClick = {
            openExcelFile(context, "speed_history.xlsx")
        }) {
            Text("Open Excel File")
        }*/
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
        while(true)
        {
            speedHistory.value = readSpeedDataFromExcel(context)
            delay(1000)
        }

    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
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

fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Float.format(digits: Int) = "%.${digits}f".format(this)