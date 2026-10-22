package com.grandmenvpn.app.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import android.content.pm.PackageManager
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.grandmenvpn.app.config.SplitRouteManager
import com.grandmenvpn.app.config.WhitelistManager
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Настройки", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Таймаут подключения", style = MaterialTheme.typography.bodyMedium)
                        Text("Секунды ожидания (по умолчанию 10)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = connectTimeout,
                        onValueChange = { connectTimeout = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("DNS", style = MaterialTheme.typography.titleMedium)
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column {
                    listOf("Авто", "Кастомный", "Системный").forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { dnsMode = index },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            RadioButton(
                                selected = dnsMode == index,
                                onClick = { dnsMode = index }
                            )
                        }
                    }
                }

                if (dnsMode == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customDns,
                        onValueChange = { customDns = it },
                        label = { Text("DNS серверы (через запятую)") },
                        placeholder = { Text("1.1.1.1, 8.8.8.8") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        Text("Российские подсети идут минуя VPN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Kill Switch", style = MaterialTheme.typography.bodyMedium)
                        Text("Блокировать интернет при разрыве VPN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { /* TODO */ },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Белый список доменов", style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Интерфейс", style = MaterialTheme.typography.titleMedium)
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showNotifications,
                        onCheckedChange = { showNotifications = it }
                    )
                    Text("Показывать уведомление VPN")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("О приложении", style = MaterialTheme.typography.titleMedium)
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Версия", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(versionName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("VPN движок", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("sing-box (libbox)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Протоколы", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC, AmneziaWG", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1A1A))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Опасная зона", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF8A80))
                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFFF8A80))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Очистить все данные", style = MaterialTheme.typography.bodyMedium)
                        Text("Удалить серверы, белый список и настройки", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { /* TODO */ }) {
                        Text("ОЧИСТИТЬ", style = TextStyle(color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showInfoDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Инструкция по использованию", color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Как пользоваться GMVPN") },
            text = {
                Text("1. Перейдите на главный экран.\n" +
                     "2. Нажмите 'Сканировать серверы' и подождите.\n" +
                     "3. Выберите сервер из списка (нажмите 'Подключить' на сервере).\n" +
                     "4. Нажмите большую кнопку 'GMVPN Connect'.\n" +
                     "5. Приятного пользования!")
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Понятно") }
            }
        )
    }
}