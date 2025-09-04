package com.hnpage.speedloggernew.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("MissingPermission")
class DeviceStatusDetector(
    private val context: Context,
    private val targetBluetoothDeviceAddress: String? = null, // Địa chỉ MAC của thiết bị Bluetooth (tùy chọn)
    private val targetBluetoothDeviceName: String? = null // Tên thiết bị Bluetooth (tùy chọn)
) {
    // StateFlow để theo dõi trạng thái sạc
    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    // StateFlow để theo dõi trạng thái kết nối Bluetooth
    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var chargingReceiver: BroadcastReceiver? = null
    private var bluetoothReceiver: BroadcastReceiver? = null

    init {
        // Khởi tạo trạng thái ban đầu
        updateChargingStatus()
        updateBluetoothStatus()
    }

    // Bắt đầu lắng nghe các sự kiện
    fun startListening() {
        registerChargingReceiver()
        registerBluetoothReceiver()
    }

    // Dừng lắng nghe các sự kiện
    fun stopListening() {
        chargingReceiver?.let { context.unregisterReceiver(it) }
        bluetoothReceiver?.let { context.unregisterReceiver(it) }
        chargingReceiver = null
        bluetoothReceiver = null
    }

    // Kiểm tra trạng thái sạc
    private fun updateChargingStatus() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    // Kiểm tra trạng thái kết nối Bluetooth với thiết bị cụ thể
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun updateBluetoothStatus() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _isBluetoothConnected.value = false
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val targetDevice = pairedDevices?.find { device ->
            when {
                targetBluetoothDeviceAddress != null -> device.address == targetBluetoothDeviceAddress
                targetBluetoothDeviceName != null -> device.name == targetBluetoothDeviceName
                else -> false
            }
        }
        _isBluetoothConnected.value = targetDevice != null && isDeviceConnected(targetDevice)
    }

    // Kiểm tra xem thiết bị Bluetooth có đang kết nối hay không
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            // Kiểm tra trạng thái kết nối (phụ thuộc vào profile, ở đây giả sử A2DP hoặc SPP)
            val isConnectedMethod = device.javaClass.getMethod("isConnected")
            isConnectedMethod.invoke(device) as Boolean
        } catch (e: Exception) {
            // Nếu không thể gọi isConnected (API không chính thức), giả sử không kết nối
            false
        }
    }

    // Đăng ký BroadcastReceiver cho trạng thái sạc
    private fun registerChargingReceiver() {
        chargingReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> _isCharging.value = true
                    Intent.ACTION_POWER_DISCONNECTED -> _isCharging.value = false
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, chargingReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // Đăng ký BroadcastReceiver cho trạng thái Bluetooth
    private fun registerBluetoothReceiver() {
        bluetoothReceiver = object : BroadcastReceiver() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED,
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && (
                                    (targetBluetoothDeviceAddress != null && device.address == targetBluetoothDeviceAddress) ||
                                            (targetBluetoothDeviceName != null && device.name == targetBluetoothDeviceName)
                                    )) {
                            updateBluetoothStatus()
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(context, bluetoothReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}