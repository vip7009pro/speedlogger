package com.hnpage.speedloggernew

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationForegroundService_backup : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val excelLogger = ExcelLogger(this)
    private val excelLogger3 = ExcelLogger3(this)





    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            //Log.d("LocationService", "LocationCallback triggered") // Log khi callback được gọi

            locationResult.lastLocation?.let { location ->
                //Log.d("LocationService", "Sending broadcast: Speed=${location.speed}, Lat=${location.latitude}, Lng=${location.longitude}") // Log dữ liệu vị trí

                // Gửi broadcast với dữ liệu vị trí
                sendBroadcast(
                    Intent("LOCATION_UPDATE").apply {
                        putExtra("speed", location.speed)
                        putExtra("lat", location.latitude)
                        putExtra("lng", location.longitude)
                        `package` = "com.hnpage.speedloggernew" // Thêm package name của bạn
                    }
                )
                //excelLogger.logData(location)
                excelLogger3.logData(location)


            }
        }

        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            super.onLocationAvailability(locationAvailability)
            //Log.d("LocationService", "Location available: ${locationAvailability.isLocationAvailable}") // Log trạng thái khả dụng của vị trí
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification()) // Khởi động foreground service
        startLocationUpdates() // Bắt đầu cập nhật vị trí
        Log.d("LocationService", "Service started") // Log khi service khởi động
        return START_STICKY
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Cấu hình LocationRequest
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setWaitForAccurateLocation(true)
            .build()

        // Kiểm tra quyền trước khi yêu cầu cập nhật vị trí
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LocationService", "Location updates started") // Log khi bắt đầu cập nhật vị trí
        } else {
            Log.e("LocationService", "Location permission not granted") // Log khi không có quyền
        }
    }

    private fun createNotification(): Notification {
        val channelId = "location_service_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Tạo notification channel (cho Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo Intent để mở MainActivity khi click vào notification
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo notification
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Tracking location updates")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent) // Set PendingIntent
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback) // Dừng cập nhật vị trí khi service bị hủy
        Log.d("LocationService", "Service destroyed") // Log khi service bị hủy
    }

    companion object {
        const val NOTIFICATION_ID = 1234 // ID của notification

        // Phương thức khởi động service
        fun startService(context: android.content.Context) {
            val intent = Intent(context, LocationForegroundService_backup::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent) // Sử dụng startForegroundService cho Android 8.0+
            } else {
                context.startService(intent) // Sử dụng startService cho các phiên bản cũ hơn
            }
            Log.d("LocationService", "Service start requested") // Log khi yêu cầu khởi động service
        }
    }

}