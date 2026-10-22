package com.grandmenvpn.app.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Простой логгер для сбора отладочной информации прямо в приложении.
 */
object DebugLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    fun log(tag: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = "[$time] [$tag] $message"
        
        // Добавляем в память, храним последние 200 строк
        _logs.value = (_logs.value + newLog).takeLast(200)
        
        // Дублируем в стандартный Logcat
        Log.d(tag, message)
    }

    fun error(tag: String, message: String, e: Throwable? = null) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val errorMsg = "[$time] [ERROR] [$tag] $message ${e?.message ?: ""}"
        
        _logs.value = (_logs.value + errorMsg).takeLast(200)
        Log.e(tag, message, e)
    }

    fun clear() {
        _logs.value = emptyList()
    }
}