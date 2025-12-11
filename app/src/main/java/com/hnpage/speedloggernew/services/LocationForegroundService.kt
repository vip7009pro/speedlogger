package com.hnpage.speedloggernew.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hnpage.speedloggernew.MainActivity
import com.hnpage.speedloggernew.R
import com.hnpage.speedloggernew.api.ExcelLogger3
import com.hnpage.speedloggernew.db.LocationRepository
import com.hnpage.speedloggernew.global.DataInterface
import com.hnpage.speedloggernew.utils.DeviceStatusDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class LocationForegroundService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private lateinit var deviceStatusDetector: DeviceStatusDetector
    private val excelLogger3 = ExcelLogger3(this)
    private val channelId = "location_service_channel"
    private var currentSpeed = 0f
    private var currentOffset: Float = 0f
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: LocationRepository
    private var hasLaunchedMainActivity = false // Cờ để tránh mở MainActivity nhiều lần

    private val locationCallback = object : LocationCallback() {
        @SuppressLint("SuspiciousIndentation")
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                val speedKmh = (location.speed * 3.6f)
                currentSpeed = speedKmh + currentOffset
                location.speed = currentSpeed / 3.6f

                // Gửi broadcast cho Android Auto
                sendBroadcast(Intent("LOCATION_UPDATE").apply {
                    putExtra("speed", location.speed)
                    putExtra("lat", location.latitude)
                    putExtra("lng", location.longitude)
                    `package` = "com.hnpage.speedloggernew"
                })

                // Ghi log vào Excel và lưu vào Room
                if (currentSpeed > 1) {
                    val timeStamp = System.currentTimeMillis().toString()

                    saveLocationToRoom(
                        DataInterface.LocationData2(
                            timeStamp = timeStamp,
                            speed = location.speed * 3.6f,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                   /* LocationUploader.sendLocation(
                        lat = location.latitude,
                        lng = location.longitude,
                        timestamp = timeStamp,
                        speed = location.speed * 3.6f
                    )*/
                    LocationUploader.sendLocation2(location.latitude, location.longitude, timeStamp, location.speed * 3.6f) { result ->
                        when (result) {
                            "success" -> Log.d("GPS", "Đại ca lên server ngon lành!")
                            "duplicated" -> Log.i("GPS", "Đã gửi rồi, bỏ qua")
                            "error" -> Log.w("GPS", "Gửi lỗi, sẽ thử lại sau")
                        }
                    }
                }

                // Cập nhật notification với tốc độ, trạng thái sạc và Bluetooth
                updateNotification(currentSpeed)

            }
        }
    }

    private fun saveLocationToRoom(location: DataInterface.LocationData2) {
        serviceScope.launch {
            val record = DataInterface.LocationData2(
                timeStamp = System.currentTimeMillis().toString(),
                speed = location.speed,
                latitude = location.latitude,
                longitude = location.longitude
            )
            repository.insertSpeedRecord(record)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = LocationRepository(applicationContext)
        // Khởi tạo DeviceStatusDetector
        deviceStatusDetector = DeviceStatusDetector(
            context = this,
            targetBluetoothDeviceAddress = "00:14:22:01:23:45", // Thay bằng MAC thực tế
            targetBluetoothDeviceName = "CAR MULTIMEDIA" // Thay bằng tên thực tế
        )
        // Khởi tạo NotificationManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Theo dõi trạng thái sạc và Bluetooth
        observeDeviceStatus()
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocationBinder()
    }

    inner class LocationBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        if (intent != null) {
            currentOffset = intent.getFloatExtra("EXTRA_SPEED_OFFSET", 0f)
        }
        startForeground(NOTIFICATION_ID, createNotification(currentSpeed))

        // Bắt đầu lắng nghe trạng thái từ DeviceStatusDetector
        deviceStatusDetector.startListening()

        // Bắt đầu lấy vị trí
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setWaitForAccurateLocation(true)
            .build()

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } else {
            Log.e("LocationService", "Location permission not granted")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId, "Location Service", NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(speed: Float): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Lấy trạng thái sạc và Bluetooth
        val isCharging = deviceStatusDetector.isCharging.value
        val isBluetoothConnected = deviceStatusDetector.isBluetoothConnected.value

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Speed")
            .setContentText(
                "Speed: %.1f km/h | Charging: %s | Bluetooth: %s".format(
                    speed,
                    if (isCharging) "Yes" else "No",
                    if (isBluetoothConnected) "Connected" else "Disconnected"
                )
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(speed: Float) {
        val notification = createNotification(speed)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun observeDeviceStatus() {
        serviceScope.launch {
            // Theo dõi trạng thái sạc
            deviceStatusDetector.isCharging.collectLatest { isCharging ->
                Log.d("LocationService", "Charging: $isCharging")
                // Kiểm tra và mở MainActivity nếu một trong hai điều kiện thỏa mãn
                checkAndLaunchMainActivity()
            }
        }
        serviceScope.launch {
            // Theo dõi trạng thái Bluetooth
            deviceStatusDetector.isBluetoothConnected.collectLatest { isConnected ->
                Log.d("LocationService", "Bluetooth Connected: $isConnected")
                // Kiểm tra và mở MainActivity nếu một trong hai điều kiện thỏa mãn
                checkAndLaunchMainActivity()
            }
        }
    }

    private fun checkAndLaunchMainActivity() {
        val isCharging = deviceStatusDetector.isCharging.value
        val isBluetoothConnected = deviceStatusDetector.isBluetoothConnected.value

        Log.d("LocationService", "Checking conditions - Charging: $isCharging, Bluetooth: $isBluetoothConnected, HasLaunched: $hasLaunchedMainActivity")

        if ((isCharging || isBluetoothConnected) && !hasLaunchedMainActivity) {
            Log.d("LocationService", "Launching MainActivity: Charging or Bluetooth connected")
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            try {
                Log.d("launch","Launch Activity Thanh cong")
                startActivity(intent)
                hasLaunchedMainActivity = true
            } catch (e: Exception) {
                Log.e("LocationService", "Failed to launch MainActivity: ${e.message}")
            }
        } else if (!isCharging && !isBluetoothConnected) {
            // Reset cờ nếu cả hai điều kiện không còn thỏa mãn
            hasLaunchedMainActivity = false
            Log.d("LocationService", "Reset hasLaunchedMainActivity to false")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Dừng lắng nghe DeviceStatusDetector
        deviceStatusDetector.stopListening()
        // Dừng cập nhật vị trí
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        const val NOTIFICATION_ID = 1234

        fun startService(context: android.content.Context, speedOffset: Float) {
            val intent = Intent(context, LocationForegroundService::class.java)
            intent.putExtra("EXTRA_SPEED_OFFSET", speedOffset)
            context.startForegroundService(intent)
        }
    }
}