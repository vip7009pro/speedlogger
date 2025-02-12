package com.hnpage.speedloggernew.api

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.util.Date

class ExcelLogger3(private val context: Context) {
    private val fileName = "speed_history.xlsx"
    private val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun logData(location: android.location.Location) {
        try {
            val resolver = context.contentResolver

            // Kiểm tra xem file đã tồn tại trong thư mục Downloads chưa
            val uri = getExistingFileUri(resolver)

            if (uri != null) {
                // Nếu file tồn tại, mở và ghi dữ liệu vào
                appendDataToFile(resolver, uri, location)
            } else {
                // Nếu file chưa tồn tại, tạo mới và ghi dữ liệu vào
                createNewFile(resolver, location)
            }

            //Log.d("ExcelLogger", "Dữ liệu đã lưu vào Downloads")
        } catch (e: Exception) {
            Log.e("ExcelLogger", "Lỗi khi ghi file Excel", e)
            //delete speed_history.xlsx
            val file = context.getExternalFilesDir(null)?.resolve(fileName)
            file?.delete()
        }
    }

    /**
     * Kiểm tra xem file đã tồn tại trong thư mục Downloads chưa
     */
    private fun getExistingFileUri(resolver: ContentResolver): Uri? {
        return resolver.query(
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
    }

    /**
     * Ghi dữ liệu mới vào file Excel đã tồn tại
     */
    private fun appendDataToFile(resolver: ContentResolver, uri: Uri, location: android.location.Location) {
        resolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream) // Load workbook từ file hiện có
            val sheet = workbook.getSheet("Speed History") ?: workbook.createSheet("Speed History").also { createHeader(it) }
            addData(sheet, location)

            resolver.openOutputStream(uri, "wt")?.use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()
        }
    }

    /**
     * Tạo file Excel mới và ghi dữ liệu lần đầu
     */
    private fun createNewFile(resolver: ContentResolver, location: android.location.Location) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Speed History")
                createHeader(sheet)
                addData(sheet, location)
                workbook.write(outputStream)
                workbook.close()
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
        } ?: Log.e("ExcelLogger", "Không thể tạo file mới")
    }

    /**
     * Tạo dòng tiêu đề (Header) cho file Excel mới
     */
    private fun createHeader(sheet: Sheet) {
        val headerRow: Row = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Timestamp")
        headerRow.createCell(1).setCellValue("Speed (km/h)")
        headerRow.createCell(2).setCellValue("Latitude")
        headerRow.createCell(3).setCellValue("Longitude")
    }

    /**
     * Thêm dòng dữ liệu mới vào file Excel
     */
    private fun addData(sheet: Sheet, location: android.location.Location) {
        val newRow = sheet.createRow(sheet.lastRowNum + 1)
        newRow.createCell(0).setCellValue(Date().toString())
        newRow.createCell(1).setCellValue(location.speed * 3.6)
        newRow.createCell(2).setCellValue(location.latitude)
        newRow.createCell(3).setCellValue(location.longitude)
    }
}
