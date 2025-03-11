package com.hnpage.speedloggernew.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {
    private val TARGET_PACKAGE_NAME =
        "com.hnpage.speedloggernew" // Thay bằng package name của app cần mở
    private val CHANNEL_ID = "BackgroundServiceChannel"
    private var isCharging = false
    private var isBluetoothConnected = false
    private var hasLaunched = false

    // BroadcastReceiver cho pin
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            checkAndLaunchApp()
        }
    }

    // BroadcastReceiver cho Bluetooth
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                checkBluetoothStatus()
                checkAndLaunchApp()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate() {
        super.onCreate()
        // Tạo notification channel và chạy foreground
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        // Đăng ký receivers
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        // Kiểm tra trạng thái ban đầu
        checkBatteryStatus()
        checkBluetoothStatus()
        checkAndLaunchApp()

        Log.d("BackgroundService", "Service đã khởi động")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        unregisterReceiver(bluetoothReceiver)
        Log.d("BackgroundService", "Service đã dừng")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Background Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service đang chạy")
            .setContentText("Đang theo dõi sạc và Bluetooth")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun checkBatteryStatus() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        isCharging = batteryManager.isCharging
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun checkBluetoothStatus() {
        val bluetoothAdapter = getBluetoothAdapter()
        isBluetoothConnected = bluetoothAdapter?.isEnabled == true &&
                bluetoothAdapter.bondedDevices.isNotEmpty()
    }

    // Hàm mới để lấy BluetoothAdapter một cách an toàn
    private fun getBluetoothAdapter(): BluetoothAdapter? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
    }

    private fun checkAndLaunchApp() {
        if (isCharging && isBluetoothConnected && !hasLaunched) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(TARGET_PACKAGE_NAME)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                    hasLaunched = true
                    Log.d("BackgroundService", "Đã mở ứng dụng: $TARGET_PACKAGE_NAME")
                } else {
                    Log.e("BackgroundService", "Không tìm thấy ứng dụng: $TARGET_PACKAGE_NAME")
                }
            } catch (e: Exception) {
                Log.e("BackgroundService", "Lỗi khi mở ứng dụng", e)
            }
        } else if (!isCharging || !isBluetoothConnected) {
            hasLaunched = false // Reset khi không còn thỏa mãn điều kiện
        }
    }
}