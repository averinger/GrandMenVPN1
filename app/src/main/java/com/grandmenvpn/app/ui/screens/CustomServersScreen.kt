package com.grandmenvpn.app.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grandmenvpn.app.config.CustomServer
import com.grandmenvpn.app.config.CustomServerManager
import com.grandmenvpn.app.parser.ParserRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomServersScreen(
    manager: CustomServerManager,
    onServerSelected: (CustomServer) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val scope = rememberCoroutineScope()
    val servers = remember { mutableStateListOf<CustomServer>() }
    var dialogOpen by remember { mutableStateOf(false) }
    var jsonInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { scope.launch { importUri(context, it) } }
    }

    LaunchedEffect(Unit) {
        scope.launch { servers.addAll(manager.load()) }
    }

    fun importJson(text: String) {
        val outbound = ParserRegistry.parse(text)
        if (outbound == null) {
            status = "Не удалось распарсить JSON"
            return
        }
        val preview = text.lines().firstOrNull()?.take(60) ?: ""
        val server = CustomServer(name = "Импортированный ($preview)", config = text, outboundJson = outbound)
        scope.launch {
            manager.add(server)
            servers.add(server)
            status = "Сервер добавлен"
        }
        dialogOpen = false
        jsonInput = ""
    }

    fun importUri(ctx: Context, uri: Uri) {
        scope.launch(Dispatchers.IO) {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                val text = input.bufferedReader().readText()
                withContext(Dispatchers.Main) {
                    jsonInput = text
                    dialogOpen = true
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Мои серверы") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF222222))
        )

        LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) {
            items(servers) { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = s.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = s.config.lines().firstOrNull()?.take(70) ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = s.enabled, onCheckedChange = { _ -> })
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { filePicker.launch("*/*") }) {
                Text("📂 Загрузить JSON")
            }
            Button(onClick = { dialogOpen = true }) {
                Text("📋 Вставить JSON")
            }
        }

        status?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false; jsonInput = "" },
            title = { Text("Вставить JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = { importJson(jsonInput) }) { Text("Применить") }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false; jsonInput = "" }) { Text("Отмена") }
            }
        )
    }
}