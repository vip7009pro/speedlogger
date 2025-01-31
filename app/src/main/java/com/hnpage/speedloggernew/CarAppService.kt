package com.hnpage.speedloggernew

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Session
import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.core.content.ContextCompat
import androidx.car.app.model.MessageTemplate

class CarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session { // <-- Sử dụng Session
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                return CarLocationScreen2(carContext)
            }
        }
    }
}

class CarLocationScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("Hello Android Auto!").build()
    }
}

class CarLocationScreen2(carContext: CarContext) : Screen(carContext) {
    private var speed: String = "0 km/h"
    private var coordinates: String = "0,0"

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                speed = "${it.getFloatExtra("speed", 0f) * 3.6} km/h"
                coordinates = "${it.getDoubleExtra("lat", 0.0)}, ${it.getDoubleExtra("lng", 0.0)}"
                invalidate()
            }
        }
    }

    init {
        val filter = IntentFilter("LOCATION_UPDATE")
        ContextCompat.registerReceiver(carContext, locationReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onGetTemplate(): Template {
        return ListTemplate.Builder()
            .setSingleList(
                ItemList.Builder()
                    .addItem(
                        Row.Builder()
                            .setTitle("Speed: $speed")
                            .addText("Coordinates: $coordinates")
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
