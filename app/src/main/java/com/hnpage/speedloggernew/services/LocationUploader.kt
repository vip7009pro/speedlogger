package com.hnpage.speedloggernew.services

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log
import java.io.IOException

object LocationUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private const val TAG = "LocationUploader"

    // Đại ca thay URL thật vào đây
    private const val SERVER_URL = "https://hungnguyenpage.com/postlocation/postlocation.php"
//    private const val SERVER_URL = "http://10.0.2.2/save_location.php" // test emulator

    fun sendLocation(
        lat: Double,
        lng: Double,
        timestamp: String,
        speed: Float = 0f
    ) {
        Log.d(TAG, "Đang gửi điểm tới server...")
        val json = JSONObject().apply {
            put("lat", lat)
            put("long", lng)
            put("timestamp", timestamp)
            put("current_speed", speed)
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        // Gửi bất đồng bộ – không block thread chính
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Gửi thất bại đại ca ơi: ${e.message}")
                // Có thể lưu vào Room để retry sau
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    val bodyString = resp.body?.string() ?: ""
                    when {
                        resp.isSuccessful -> {
                            when {
                                bodyString.contains("\"duplicated\"") -> {
                                    Log.i(TAG, "Điểm này đã tồn tại → bỏ qua (không lỗi)")
                                }
                                bodyString.contains("\"success\"") -> {
                                    Log.d(TAG, "Đại ca đã lên server thành công!")
                                }
                            }
                        }
                        else -> {
                            Log.w(TAG, "Server trả lỗi ${resp.code}: $bodyString")
                        }
                    }
                }
            }
        })
    }


    fun sendLocation2(
        lat: Double,
        lng: Double,
        timestamp: String,
        speed: Float = 0f,
        onResult: ((String) -> Unit)? = null   // "success" | "duplicated" | "error"
    ) {
        Log.d(TAG, "Đang gửi điểm tới server...")
        val json = JSONObject().apply {
            put("lat", lat)
            put("long", lng)
            put("timestamp", timestamp)
            put("current_speed", speed)
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(SERVER_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult?.invoke("error")
                Log.e(TAG, "Lỗi mạng: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string() ?: ""
                    val result = when {
                        it.isSuccessful && body.contains("success") -> "success"
                        it.isSuccessful && body.contains("duplicated") -> "duplicated"
                        else -> "error"
                    }
                    onResult?.invoke(result)
                }
            }
        })
    }

}