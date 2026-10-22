package com.grandmenvpn.app.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.grandmenvpn.app.scanner.FoundServer
import com.grandmenvpn.app.scanner.ServerScanner
import com.grandmenvpn.app.vpn.VpnManager
import com.grandmenvpn.app.config.CustomServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vpnManager: VpnManager,
    serverScanner: ServerScanner
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val customServerManager = remember { CustomServerManager(context) }
    
    val connected = remember { mutableStateOf(false) }
    val serverIp = remember { mutableStateOf<String?>(null) }
    val status = remember { mutableStateOf("Готов к подключению") }
    val servers = remember { mutableStateListOf<FoundServer>() }
    
    // Состояния для загрузки
    val scanRunning = remember { mutableStateOf(false) }
    val scanStatusText = remember { mutableStateOf("") } // Текст для экрана загрузки
    
    val selectedProfile = remember { mutableStateOf<FoundServer?>(null) }

    val pulseScale = remember { mutableStateOf(1f) }
    val pulseAlpha = remember { mutableStateOf(0f) }

    // Загрузка кастомных серверов при старте
    LaunchedEffect(Unit) {
        try {
            val customServers = customServerManager.load()
            val mappedServers = customServers.map { custom ->
                FoundServer(
                    protocol = "Custom",
                    host = custom.name,
                    port = 0,
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

    fun connect() = scope.launch {
        if (connected.value) {
            vpnManager.stop()
            connected.value = false
            serverIp.value = null
            status.value = "Готов к подключению"
        } else {
            val s = selectedProfile.value
            if (s != null) {
                status.value = "Подключение…"
                val ok = vpnManager.start(s.outboundJson)
                if (ok) {
                    connected.value = true
                    status.value = "Подключён к ${s.host}"

                    // Добавляем получение реального IP
                    serverIp.value = "Получение IP..."
                    val realIp = fetchPublicIp()
                    serverIp.value = realIp ?: s.host
                } else {
                    status.value = "Ошибка подключения"
                }
            } else {
                status.value = "Сначала выберите сервер из списка"
            }
        }
    }

    fun connectTo(s: FoundServer) = scope.launch {
        selectedProfile.value = s
        status.value = "Подключение…"
        val ok = vpnManager.start(s.outboundJson)
        if (ok) {
            connected.value = true
            status.value = "Подключён к ${s.host}"

            // Добавляем получение реального IP
            serverIp.value = "Получение IP..."
            val realIp = fetchPublicIp()
            serverIp.value = realIp ?: s.host
        } else {
            connected.value = false
            status.value = "Ошибка подключения"
        }
    }

    fun scan() = scope.launch {
        scanRunning.value = true
        scanStatusText.value = "Поиск источников..."
        status.value = "Сканирование..."
        
        try {
            // Небольшая задержка для визуального эффекта
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
        // Используем Box как контейнер для наложения экрана загрузки поверх всего
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            
            // Основной интерфейс
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
                            .align(Alignment.Center)
                    ) {
                        Text(
                            text = if (connected.value) "GMVPN Disconnect" else "GMVPN Connect",
                            color = Color(0xFF121212),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

            serverIp.value?.let { ip ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ваш IP: $ip",
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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

                // ВАЖНО: weight(1f) дает списку занять все оставшееся место на экране
                Box(modifier = Modifier.weight(1f)) {
                    ServerList(servers = servers, selectedProfile = selectedProfile.value, onConnect = ::connectTo)
                }
            }

            // ЭКРАН ЗАГРУЗКИ (Поверх всего)
            if (scanRunning.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)), // Затемнение
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