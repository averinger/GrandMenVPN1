package com.grandmenvpn.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.tokens.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.grandmenvpn.app.config.SplitRouteManager
import com.grandmenvpn.app.config.WhitelistManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistScreen(
    whitelistManager: WhitelistManager,
    splitRouteManager: SplitRouteManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var domains by remember { mutableStateListOf<String>() }
    var newDomain by remember { mutableStateOf("") }
    var autoSplit by remember { mutableStateOf(false) }
    var killSwitch by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch { domains.addAll(whitelistManager.load()) }
        autoSplit = splitRouteManager.isAutoSplitEnabled(context)
        killSwitch = splitRouteManager.isKillSwitchEnabled(context)
    }

    fun add() {
        val d = newDomain.trim().lowercase()
        if (d.isBlank() || d in domains) return
        scope.launch {
            whitelistManager.addDomain(d)
            domains.add(d)
            newDomain = ""
        }
    }

    fun remove(d: String) = scope.launch {
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
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { add() }) { Text("Добавить") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(domains, key = { it }) { d ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = d, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { remove(d) }) {
                        Icon(Delete, contentDescription = null)
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
