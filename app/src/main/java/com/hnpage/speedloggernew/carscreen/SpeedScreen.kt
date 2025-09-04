package com.hnpage.speedloggernew.car

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class SpeedScreen(carContext: CarContext) : Screen(carContext) {
    private var currentSpeed: Float = 0f
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "LOCATION_UPDATE") {
                currentSpeed = intent.getFloatExtra("speed", 0f) * 3.6f // Chuyển m/s thành km/h
                Log.d("SpeedScreen", "Received speed: $currentSpeed km/h")
                invalidate() // Cập nhật giao diện
            }
        }
    }

    init {
        // Đăng ký receiver khi màn hình được tạo
        val filter = IntentFilter("LOCATION_UPDATE")
        ContextCompat.registerReceiver(
            carContext,
            locationReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Hủy đăng ký khi màn hình bị hủy
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                carContext.unregisterReceiver(locationReceiver)
                super.onDestroy(owner)
            }
        })
    }

    override fun onGetTemplate(): Template {
        // Tạo hàng hiển thị tốc độ
        val row = Row.Builder()
            .setTitle("Current Speed")
            .addText(String.format("%.1f km/h", currentSpeed))
            .build()

        // Tạo pane chứa hàng tốc độ
        val pane = Pane.Builder()
            .addRow(row)
            .build()

        // Trả về template với tiêu đề và pane
        return PaneTemplate.Builder(pane)
            .setHeaderAction(Action.APP_ICON)
            .setTitle("Speed Logger")
            .build()
    }
}