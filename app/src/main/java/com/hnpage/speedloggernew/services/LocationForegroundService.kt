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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocationForegroundService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private val excelLogger3 = ExcelLogger3(this)
    private val channelId = "location_service_channel"
    private var currentSpeed = 0f
    private var currentOffset: Float = 0f
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: LocationRepository

    /* private val viewModelStore= ViewModelStore()
     private lateinit var appViewModel: AppViewModel
     private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
 */

    private val locationCallback = object : LocationCallback() {
        @SuppressLint("SuspiciousIndentation")
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->

                val speedKmh = (location.speed * 3.6f)
                currentSpeed = speedKmh + currentOffset
                location.speed = currentSpeed / 3.6f

                //Log.d("LocationService", "Speed: $currentSpeed km/h, Lat: ${location.latitude}, Lng: ${location.longitude}")

                // Gửi broadcast cho Android Auto
                sendBroadcast(Intent("LOCATION_UPDATE").apply {
                    putExtra("speed", location.speed)
                    putExtra("lat", location.latitude)
                    putExtra("lng", location.longitude)
                    `package` = "com.hnpage.speedloggernew"
                })

                // Ghi log vào Excel
                if (currentSpeed > 1) {
                    saveLocationToRoom(
                        DataInterface.LocationData2(
                            timeStamp = System.currentTimeMillis().toString(),
                            speed = location.speed * 3.6f, // Chuyển đổi sang km/h
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                    //excelLogger3.logData(location)
                }


                // Cập nhật notification với tốc độ mới nhất
                updateNotification(currentSpeed)
            }
        }
    }

    private fun saveLocationToRoom(location: DataInterface.LocationData2) {
        serviceScope.launch {
            val record = DataInterface.LocationData2(
                timeStamp = System.currentTimeMillis().toString(),
                speed = location.speed, // Chuyển đổi sang km/h
                latitude = location.latitude,
                longitude = location.longitude
            )
            repository.insertSpeedRecord(record)
        }
    }


    override fun onCreate() {
        super.onCreate()
        repository = LocationRepository(applicationContext)/*appViewModel = ViewModelProvider(
            viewModelStore,
            ViewModelProvider.NewInstanceFactory()
        )[AppViewModel::class.java]*/
        // Khởi tạo ViewModel

    }

    override fun onBind(intent: Intent?): IBinder? = null
    inner class LocationBinder : Binder() {
        fun getService(): LocationForegroundService = this@LocationForegroundService
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        if (intent != null) {
            currentOffset = intent.getFloatExtra("EXTRA_SPEED_OFFSET", 0f)
        }
        startForeground(NOTIFICATION_ID, createNotification(currentSpeed))

        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500).setWaitForAccurateLocation(true).build()

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


        return NotificationCompat.Builder(this, channelId).setContentTitle("Tracking Speed")
            .setContentText("Current Speed: %.1f km/h".format(speed))
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Thay bằng icon của bạn
            .setPriority(NotificationCompat.PRIORITY_LOW).setContentIntent(pendingIntent).build()
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

        fun startService(context: android.content.Context, speedOffset: Float) {
            val intent = Intent(context, LocationForegroundService::class.java)
            intent.putExtra(
                "EXTRA_SPEED_OFFSET", speedOffset
            )  // Truyền giá trị speedOffset vào Intent
            context.startForegroundService(intent)
        }
    }

}
