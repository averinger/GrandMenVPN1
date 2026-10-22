package com.grandmenvpn.app.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.tokens.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grandmenvpn.app.config.CustomServer
import com.grandmenvpn.app.config.CustomServerManager
import com.grandmenvpn.app.parser.ParserRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(
    manager: CustomServerManager,
    onServerAdded: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { scope.launch(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                input = stream.bufferedReader().readText()
            }
        } }
    }

    fun apply(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            status = "Пустой ввод"
            return
        }
        val outbound = ParserRegistry.parse(trimmed)
        if (outbound == null) {
            status = "Не удалось распарсить конфиг"
            return
        }
        scope.launch {
            manager.add(CustomServer(name = "Ручной", config = trimmed, outboundJson = outbound))
            status = "Сервер сохранён"
            input = ""
        }
        onServerAdded()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Ручной ввод", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Конфиг сервера") },
            placeholder = { Text("vless://, vmess://, trojan://, ss:// или JSON") },
            singleLine = false,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )

        val isJson = input.trimStart().startsWith("{")
        if (isJson) {
            Text(text = "Обнаружен JSON-конфиг", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { apply(input) }) { Text("Сохранить сервер") }
            Button(onClick = { filePicker.launch(Unit) }) { Text("📂 Загрузить JSON") }
            Button(onClick = {
                val clip = clipboard.primaryClip?.getItemAt(0)?.text
                if (clip != null) input = clip.toString()
            }) { Text("📋 Из буфера") }
        }

        status?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
}
