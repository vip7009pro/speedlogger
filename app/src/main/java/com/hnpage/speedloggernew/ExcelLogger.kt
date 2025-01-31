package com.hnpage.speedloggernew

import android.content.Context
import android.util.Log
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class ExcelLogger(private val context: Context) {
    private val fileName = "speed_history.xlsx"

    // Tạo header nếu file chưa tồn tại
    private fun createHeader(sheet: Sheet) {
        val headerRow: Row = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Timestamp")
        headerRow.createCell(1).setCellValue("Speed (km/h)")
        headerRow.createCell(2).setCellValue("Latitude")
        headerRow.createCell(3).setCellValue("Longitude")
    }

    fun logData(location: android.location.Location) {
        val workbook: Workbook = if (isFileExists()) loadWorkbook() else XSSFWorkbook()
        val sheet: Sheet = workbook.getSheet("Speed History") ?: workbook.createSheet("Speed History").also { createHeader(it) }

        val newRow = sheet.createRow(sheet.lastRowNum + 1)
        newRow.createCell(0).setCellValue(Date().toString())
        newRow.createCell(1).setCellValue(location.speed * 3.6)
        newRow.createCell(2).setCellValue(location.latitude)
        newRow.createCell(3).setCellValue(location.longitude)

        saveWorkbook(workbook)
    }

    private fun isFileExists(): Boolean {
        return File(context.getExternalFilesDir(null), fileName).exists()
    }

    private fun loadWorkbook(): Workbook {
        return try {
            val file = File(context.getExternalFilesDir(null), fileName)
            XSSFWorkbook(file.inputStream())
        } catch (e: Exception) {
            XSSFWorkbook() // Tạo workbook mới nếu load thất bại
        }
    }

    private fun saveWorkbook(workbook: Workbook) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()
        } catch (e: Exception) {
            Log.e("ExcelLogger", "Error saving file", e)
        }
    }
}