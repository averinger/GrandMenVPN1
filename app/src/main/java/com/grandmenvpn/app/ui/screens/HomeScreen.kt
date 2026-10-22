package com.grandmenvpn.app.ui.screens

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grandmenvpn.app.config.CustomServerManager
import com.grandmenvpn.app.scanner.FoundServer
import com.grandmenvpn.app.scanner.ServerScanner
import com.grandmenvpn.app.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vpnManager: VpnManager,
    serverScanner: ServerScanner
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val customServerManager = remember { CustomServerManager(context) }
    
    // Состояния (без делегатов)
    val connected = remember { mutableStateOf(false) }
    val serverIp = remember { mutableStateOf<String?>(null) }
    val status = remember { mutableStateOf("Готов к подключению") }
    val servers = remember { mutableStateListOf<FoundServer>() }
    val scanRunning = remember { mutableStateOf(false) }
    val scanStatusText = remember { mutableStateOf("") }
    val selectedProfile = remember { mutableStateOf<FoundServer?>(null) }

    val pulseScale = remember { mutableStateOf(1f) }
    val pulseAlpha = remember { mutableStateOf(0f) }

    // --- ЛАУНЧЕР ДЛЯ ЗАПРОСА РАЗРЕШЕНИЯ VPN ---
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Пользователь нажал "ОК" в системном окне, запускаем реальный VPN
            val s = selectedProfile.value
            if (s != null) {
                scope.launch {
                    val ok = vpnManager.start(s.outboundJson)
                    if (ok) {
                        connected.value = true
                        status.value = "Подключён к ${s.host}"
                        serverIp.value = "Получение IP..."
                        serverIp.value = fetchPublicIp() ?: s.host
                    } else {
                        status.value = "Ошибка запуска VPN"
                    }
                }
            }
        } else {
            status.value = "В разрешении на VPN отказано"
        }
    }

    // Загрузка ручных серверов с парсингом реального IP
    LaunchedEffect(Unit) {
        try {
            val customServers = customServerManager.load()
            val mappedServers = customServers.map { custom ->
                var realHost = custom.name
                var realPort = 0
                try {
                    val json = JSONObject(custom.outboundJson)
                    val outbounds = json.optJSONArray("outbounds")
                    if (outbounds != null && outbounds.length() > 0) {
                        val outbound = outbounds.getJSONObject(0)
                        realHost = outbound.optString("server", custom.name)
                        realPort = outbound.optInt("server_port", 0)
                    }
                } catch (e: Exception) { }

                FoundServer(
                    protocol = "Custom",
                    host = realHost,
                    port = realPort,
                    pingMs = 0,
                    source = "Manual",
                    config = custom.config,
                    outboundJson = custom.outboundJson
                )
            }
            servers.addAll(mappedServers)
        } catch (e: Exception) { }
    }

    // Анимация пульсации
    LaunchedEffect(connected.value) {
        pulseScale.value = 1f
        pulseAlpha.value = 0f
        if (connected.value) {
            launch {
                while (true) {
                    val start = System.currentTimeMillis()
                    while (System.currentTimeMillis() - start < 1500) {
                        val t = ((System.currentTimeMillis() - start) / 1500f).coerceAtMost(1f)
                        pulseScale.value = 1f + t * 0.5f
                        pulseAlpha.value = 0.4f * (1f - t)
                        delay(16)
                    }
                }
            }
        }
    }

    val buttonBrush = Brush.linearGradient(
        colors = if (connected.value)
            listOf(Color(0xFFF44336), Color(0xFFB71C1C))
        else
            listOf(Color(0xFF00E676), Color(0xFF009E55)),
        start = Offset(0.5f, 0f),
        end = Offset(0.5f, 1f)
    )

    // Вспомогательная функция подключения (чтобы не дублировать код)
    fun executeConnection(s: FoundServer) = scope.launch {
        status.value = "Подключение…"
        val ok = vpnManager.start(s.outboundJson)
        if (ok) {
            connected.value = true
            status.value = "Подключён к ${s.host}"
            serverIp.value = "Получение IP..."
            serverIp.value = fetchPublicIp() ?: s.host
        } else {
            connected.value = false
            status.value = "Ошибка подключения"
        }
    }

    fun connect() {
        if (connected.value) {
            scope.launch { // <--- Оборачиваем в корутину
                vpnManager.stop()
                connected.value = false
                serverIp.value = null
                status.value = "Готов к подключению"
            }
        } else {
            val s = selectedProfile.value
            if (s != null) {
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    status.value = "Запрос разрешения на VPN..."
                    vpnPermissionLauncher.launch(intent)
                } else {
                    executeConnection(s)
                }
            } else {
                status.value = "Сначала выберите сервер из списка"
            }
        }
    }

    fun connectTo(s: FoundServer) {
        selectedProfile.value = s
        val intent = VpnService.prepare(context)
        if (intent != null) {
            status.value = "Запрос разрешения на VPN..."
            vpnPermissionLauncher.launch(intent)
        } else {
            executeConnection(s)
        }
    }

    fun scan() = scope.launch {
        scanRunning.value = true
        scanStatusText.value = "Поиск источников..."
        status.value = "Сканирование..."
        
        try {
            delay(500) 
            scanStatusText.value = "Проверка рабочих серверов (TCP Ping)..."
            val found = serverScanner.scan()
            
            withContext(Dispatchers.Main) {
                val customServers = servers.filter { it.source == "Manual" }
                servers.clear()
                servers.addAll(customServers)
                servers.addAll(found)
                status.value = if (found.isEmpty()) "Свободных серверов не найдено" else "Найдено серверов: ${found.size}"
            }
        } catch (e: Exception) {
            status.value = "Ошибка сети: ${e.localizedMessage ?: "Проверьте интернет"}"
        } finally {
            scanRunning.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GrandMenVPN", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { scan() }, enabled = !scanRunning.value) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сканировать", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            // Основной UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = status.value,
                    style = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка подключения
                Box(modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)) {
                    Box(modifier = Modifier
                        .size(186.dp)
                        .background(Color(0xFF000000).copy(alpha = 0.30f), shape = CircleShape)
                        .offset(y = 8.dp)
                        .align(Alignment.Center))

                    Box(modifier = Modifier
                        .size(220.dp)
                        .alpha(pulseAlpha.value)
                        .graphicsLayer { scaleX = pulseScale.value; scaleY = pulseScale.value }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E676).copy(alpha = 0f),
                                    Color(0xFF00E676).copy(alpha = 0.4f)
                                ),
                                center = Offset(0.5f, 0.5f)
                            ),
                            shape = CircleShape
                        )
                        .align(Alignment.Center))

                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(buttonBrush, shape = CircleShape)
                            .clickable { connect() }
                            .align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (connected.value) "GMVPN\nDisconnect" else "GMVPN\nConnect",
                            color = Color(0xFF121212),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                serverIp.value?.let { ip ->
                    Text(
                        text = "Ваш IP: $ip",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { scan() },
                    enabled = !scanRunning.value && !connected.value,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(if (scanRunning.value) "Сканирование…" else "Сканировать серверы")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Доступные серверы",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ServerList(servers = servers, selectedProfile = selectedProfile.value, onConnect = ::connectTo)
                }
            }

            // Экран загрузки
            if (scanRunning.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E676),
                            modifier = Modifier.size(60.dp),
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = scanStatusText.value,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Это может занять до 30 секунд",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// Вспомогательная функция для получения IP (вне Composable)
suspend fun fetchPublicIp(): String? = withContext(Dispatchers.IO) {
    try {
        val url = java.net.URL("https://api.ipify.org")
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        null
    }
}