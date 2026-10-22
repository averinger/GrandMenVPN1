package com.grandmenvpn.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.grandmenvpn.app.config.SplitRouteManager
import com.grandmenvpn.app.config.WhitelistManager
import com.grandmenvpn.app.utils.DebugLogger
import com.grandmenvpn.app.vpn.VpnManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    whitelistManager: WhitelistManager,
    splitRouteManager: SplitRouteManager,
    vpnManager: VpnManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var autoSplit by remember { mutableStateOf(false) }
    var killSwitch by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(true) }
    var dnsMode by remember { mutableStateOf(0) }
    var customDns by remember { mutableStateOf("") }
    var mtu by remember { mutableStateOf("1400") }
    var connectTimeout by remember { mutableStateOf("10") }
    var versionName by remember { mutableStateOf("1.0") }

    // Состояния для пасхалки и меню
    var tapCount by remember { mutableStateOf(0) }
    var showDevMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            autoSplit = splitRouteManager.isAutoSplitEnabled(context)
            killSwitch = splitRouteManager.isKillSwitchEnabled(context)
            try {
                versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            } catch (e: PackageManager.NameNotFoundException) {
                versionName = "1.0"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // VPN Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("VPN", style = MaterialTheme.typography.titleMedium)
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("MTU", style = MaterialTheme.typography.bodyMedium)
                        Text("Размер пакета (по умолчанию 1400)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = mtu,
                        onValueChange = { mtu = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Routing Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Маршрутизация", style = MaterialTheme.typography.titleMedium)
                Divider(modifier = Modifier.padding(vertical = 12.dp))

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
                    Column {
                        Text("Авто-сплит RU-трафика", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                    Column {
                        Text("Kill Switch (безопасный разрыв)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About & Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("О приложении", style = MaterialTheme.typography.titleMedium)
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Кнопка Инфо
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInfoDialog = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Инструкция по использованию", style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // СКРЫТАЯ КНОПКА (ПАСХАЛКА)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            tapCount++
                            if (tapCount >= 10) {
                                showDevMenu = true
                                tapCount = 0
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Версия", style = MaterialTheme.typography.bodyMedium)
                    Text(versionName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- ДИАЛОГ С ИНСТРУКЦИЕЙ ---
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Как пользоваться GMVPN") },
            text = {
                Text("1. Перейдите на главный экран.\n" +
                     "2. Нажмите 'Сканировать серверы' и подождите.\n" +
                     "3. Выберите сервер из списка (нажмите 'Подключить' на сервере).\n" +
                     "4. Нажмите большую кнопку 'GMVPN Connect'.\n" +
                     "5. Разрешите системе создать VPN-подключение.\n" +
                     "6. Приятного пользования!")
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Понятно") }
            }
        )
    }

    // --- СКРЫТОЕ МЕНЮ РАЗРАБОТЧИКА ---
    if (showDevMenu) {
        val logs by DebugLogger.logs.collectAsState()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        Dialog(
            onDismissRequest = { showDevMenu = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                color = Color(0xFF121212),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Developer Menu", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Системная инфа
                    Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}", color = Color.LightGray, fontSize = 12.sp)
                    Text("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", color = Color.LightGray, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Логи
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black).padding(8.dp)) {
                        if (logs.isEmpty()) {
                            Text("Логов пока нет...", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                logs.forEach { logLine ->
                                    Text(
                                        text = logLine,
                                        color = if (logLine.contains("[ERROR]")) Color.Red else Color.Green,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопки управления
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = {
                                val fullLog = "Device: ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nApp Ver: $versionName\n\n" + logs.joinToString("\n")
                                clipboard.setPrimaryClip(ClipData.newPlainText("Logs", fullLog))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Скопировать", color = Color.White)
                        }

                        Button(
                            onClick = { DebugLogger.clear() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Очистить", color = Color.White)
                        }

                        Button(
                            onClick = { showDevMenu = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Закрыть", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}