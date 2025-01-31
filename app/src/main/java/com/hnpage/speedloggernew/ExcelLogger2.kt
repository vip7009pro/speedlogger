package com.hnpage.speedloggernew

import android.content.Context
import android.os.Environment
import android.util.Log
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class ExcelLogger2(private val context: Context) {
    private val fileName = "speed_history.xlsx"

    // Lưu file Excel ra thẻ nhớ ngoài
    fun logData(location: android.location.Location, customPath: String? = null) {
        val workbook: Workbook = if (isFileExists(customPath)) loadWorkbook(customPath) else createNewWorkbook()
        val sheet: Sheet = workbook.getSheet("Speed History") ?: workbook.createSheet("Speed History").also { createHeader(it) }

        val newRow = sheet.createRow(sheet.lastRowNum + 1)
        newRow.createCell(0).setCellValue(Date().toString())
        newRow.createCell(1).setCellValue(location.speed * 3.6)
        newRow.createCell(2).setCellValue(location.latitude)
        newRow.createCell(3).setCellValue(location.longitude)

        saveWorkbook(workbook, customPath)
    }

    private fun createNewWorkbook(): Workbook {
        val workbook = XSSFWorkbook() // Tạo workbook mới
        val sheet = workbook.createSheet("Speed History") // Tạo sheet mới
        createHeader(sheet) // Thêm header vào sheet
        return workbook
    }


    private fun createHeader(sheet: Sheet) {
        val headerRow: Row = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Timestamp")
        headerRow.createCell(1).setCellValue("Speed (km/h)")
        headerRow.createCell(2).setCellValue("Latitude")
        headerRow.createCell(3).setCellValue("Longitude")
    }

    private fun isFileExists(customPath: String?): Boolean {
        val file = if (customPath != null) {
            File(customPath, fileName)
        } else {
            File(context.getExternalFilesDir(null), fileName)
        }
        return file.exists()
    }

    private fun loadWorkbook(customPath: String?): Workbook {
        return try {
            val file = if (customPath != null) {
                File(customPath, fileName)
            } else {
                File(context.getExternalFilesDir(null), fileName)
            }
            XSSFWorkbook(file.inputStream())
        } catch (e: Exception) {
            XSSFWorkbook() // Tạo workbook mới nếu load thất bại
        }
    }

    private fun saveWorkbook(workbook: Workbook, customPath: String?) {
        try {
            val file = if (customPath != null) {
                File(customPath, fileName)
            } else {
                File(context.getExternalFilesDir(null), fileName)
            }
            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()
            Log.d("ExcelLogger", "File saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("ExcelLogger", "Error saving file", e)
        }
    }

    // Lấy đường dẫn thẻ nhớ ngoài
    fun getExternalStoragePath(): String? {
        val externalStorage = Environment.getExternalStorageDirectory()
        return if (externalStorage != null && externalStorage.exists()) {
            externalStorage.absolutePath
        } else {
            null
        }
    }
}