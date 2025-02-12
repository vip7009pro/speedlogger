package com.hnpage.speedloggernew.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlobalFunction {
    fun openExcelFile(context: Context, fileName: String) {
        val contentResolver = context.contentResolver

        // Truy vấn file đã lưu trong thư mục Downloads
        val uri = contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(fileName),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id.toString()).build()
            } else null
        }

        if (uri != null) {
            // Tạo intent để mở file với các app hỗ trợ
            Log.d("URL", uri.toString())
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Kiểm tra nếu có ứng dụng hỗ trợ mở file
            if (openIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(openIntent)
            } else {
                Toast.makeText(context, "No app found to open Excel file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Excel file not found", Toast.LENGTH_SHORT).show()
        }
    }
    fun stringToTimestamp(dateString: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.parse(dateString)?.time ?: 0L
    }
    fun convertTimestampToDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    fun readSpeedDataFromExcel(context: Context): List<Pair<Float, Float>> {
        val speedData = mutableListOf<Pair<Float, Float>>()

        try {
            val resolver = context.contentResolver
            val uri = getExcelFileUri(context) ?: return emptyList()

            resolver.openInputStream(uri)?.use { inputStream ->
                val workbook = XSSFWorkbook(inputStream)
                val sheet = workbook.getSheetAt(0) // Sheet đầu tiên

                for (row in sheet) {
                    if (row.rowNum == 0) continue // Bỏ qua dòng tiêu đề

                    val timeCell = row.getCell(0) // Timestamp (chuyển sang giây)
                    val speedCell = row.getCell(1) // Speed (km/h)

                    val time = row.rowNum.toFloat() // Số dòng thay cho thời gian
                    val speed = speedCell?.numericCellValue?.toFloat() ?: 0f

                    speedData.add(time to speed)
                }
                workbook.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return speedData
    }
    /**
     * Lấy Uri của file Excel trong thư mục Downloads
     */
    fun getExcelFileUri(context: Context): Uri? {
        val resolver = context.contentResolver
        return resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf("speed_history.xlsx"),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
            } else null
        }
    }

}