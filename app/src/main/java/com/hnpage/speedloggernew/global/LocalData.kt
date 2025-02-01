package com.hnpage.speedloggernew.global

import android.content.SharedPreferences
import android.content.Context

class LocalData {
    fun saveData(context: Context, key: String, value: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("speedlogger_prefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }
    fun getData(context: Context, key: String): String {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("speedlogger_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, "") ?: ""
    }
}