package com.hnpage.speedloggernew

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationForegroundService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private val excelLogger3 = ExcelLogger3(this)
    private var isAndroidAutoConnected = false
    private lateinit var locationViewModel: LocationViewModel

    private val channelId = "location_service_channel"
    private var currentSpeed = 0f

    private val locationCallback = object : LocationCallback() {
        @SuppressLint("SuspiciousIndentation")
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                val speedKmh = location.speed * 3.6f
                currentSpeed = speedKmh

                Log.d("LocationService", "Speed: $speedKmh km/h, Lat: ${location.latitude}, Lng: ${location.longitude}")

                // Gửi broadcast cho Android Auto
                sendBroadcast(Intent("LOCATION_UPDATE").apply {
                    putExtra("speed", location.speed)
                    putExtra("lat", location.latitude)
                    putExtra("lng", location.longitude)
                    `package` = "com.hnpage.speedloggernew"
                })

                // Ghi log vào Excel
                if(currentSpeed > 1)
                excelLogger3.logData(location)

                // Cập nhật notification với tốc độ mới nhất
                updateNotification(speedKmh)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Khởi tạo ViewModel

    }
    override fun onBind(intent: Intent?): IBinder? = null
    inner class LocationBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(currentSpeed))

        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setWaitForAccurateLocation(true)
            .build()

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Log.e("LocationService", "Location permission not granted")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Location Service",
            NotificationManager.IMPORTANCE_LOW
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


        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Speed")
            .setContentText("Current Speed: %.1f km/h".format(speed))
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(speed: Float) {
        val notification = createNotification(speed)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    companion object {
        const val NOTIFICATION_ID = 1234

        fun startService(context: android.content.Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            context.startForegroundService(intent)
        }
    }

}
