package com.hnpage.speedloggernew

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.hnpage.speedloggernew.db.LocationViewModel
import com.hnpage.speedloggernew.global.LocalNavHostController
import com.hnpage.speedloggernew.screens.HomeScreen
import com.hnpage.speedloggernew.screens.SpeedLogScreens
import com.hnpage.speedloggernew.services.CarAppService
import com.hnpage.speedloggernew.services.LocationForegroundService
import com.hnpage.speedloggernew.ui.theme.SpeedLoggerNewTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val REQUEST_CODE_STORAGE_PERMISSIONS = 100
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

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


    @Inject
    lateinit var navHostControllerProvider: dagger.Lazy<NavHostController>

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
        //Log.d("MainActivity", "Receiver registered")
        viewModel.speedOffset.value?.let { LocationForegroundService.startService(this, it) }
        //Log.d("MainActivity", "Attempted to start service")
        if (isAndroidAutoConnected()) {
            startCarAppService()
            //Log.d("MainActivity", "CarAppService started")
        }
        //LocationForegroundService.startService(this)
        setContent {
            val locationViewModel: LocationViewModel by viewModels()

            val navController = rememberNavController()

            // Cung cấp NavController cho toàn bộ app

            SpeedLoggerNewTheme {
                //HomeScreen()
                //AppScreen()
                CompositionLocalProvider(LocalNavHostController provides navController) {
                    HomeScreen()
                }
                SpeedLogScreens().MainScreen(viewModel = viewModel,
                    lctvm = locationViewModel,
                    onStopService = { stopService() },
                    onStartService = { startService() })
            }


            // Inject runtime instance of NavController
            navHostControllerProvider.get() // Force runtime injection



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

    override fun onDestroy() {
        unregisterReceiver(locationReceiver)
        super.onDestroy()
    }
}



fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Float.format(digits: Int) = "%.${digits}f".format(this)