package com.grandmenvpn.app.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grandmenvpn.app.config.SplitRouteManager
import com.grandmenvpn.app.config.WhitelistManager
import kotlinx.coroutines.launch
// ВАЖНО: Эти два импорта решают ошибку Property delegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    whitelistManager: WhitelistManager,
    splitRouteManager: SplitRouteManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Явно указываем тип SnapshotStateList<String>
    val domains = remember { mutableStateListOf<String>() } 
    var newDomain by remember { mutableStateOf("") }
    var autoSplit by remember { mutableStateOf(false) }
    var killSwitch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch { domains.addAll(whitelistManager.load()) }
        autoSplit = splitRouteManager.isAutoSplitEnabled(context)
        killSwitch = splitRouteManager.isKillSwitchEnabled(context)
    }

    fun addDomainToList() {
        val d = newDomain.trim().lowercase()
        if (d.isBlank() || domains.contains(d)) return
        scope.launch {
            whitelistManager.addDomain(d)
            domains.add(d)
            newDomain = ""
        }
    }

    fun removeDomainFromList(d: String) = scope.launch {
        whitelistManager.removeDomain(d)
        domains.remove(d)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Белый список (сайты без VPN)", style = MaterialTheme.typography.headlineMedium)

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newDomain,
                onValueChange = { newDomain = it },
                label = { Text("Домен") },
                placeholder = { Text("example.com") },
                supportingText = { Text("Вводите только домен, без https:// и путей", fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { addDomainToList() }) { Text("Добавить") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            // Указываем items = domains для однозначности
            items(items = domains, key = { it }) { domainItem ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Принудительно .toString() на всякий случай
                    Text(text = domainItem.toString(), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { removeDomainFromList(domainItem) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
                Divider()
            }
        }

        Text(text = "Настройки маршрутизации", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = autoSplit,
                onCheckedChange = { v ->
                    autoSplit = v
                    scope.launch { splitRouteManager.setAutoSplitEnabled(context, v) }
                }
            )
            Text("Авто-сплит RU-трафика")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = killSwitch,
                onCheckedChange = { v ->
                    killSwitch = v
                    scope.launch { splitRouteManager.setKillSwitchEnabled(context, v) }
                }
            )
            Text("Kill Switch (безопасный разрыв)")
        }
    }
}