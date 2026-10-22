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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grandmenvpn.app.scanner.FoundServer
import com.grandmenvpn.app.scanner.ServerScanner
import com.grandmenvpn.app.vpn.VpnManager
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
    
    // УБРАЛИ 'by'. Теперь это просто объекты State, и мы дергаем .value
    val connected = remember { mutableStateOf(false) }
    val seconds = remember { mutableStateOf(0) }
    val serverIp = remember { mutableStateOf<String?>(null) }
    val status = remember { mutableStateOf("Готов к подключению") }
    val servers = remember { mutableStateListOf<FoundServer>() }
    val scanRunning = remember { mutableStateOf(false) }
    val selectedProfile = remember { mutableStateOf<FoundServer?>(null) }

    val pulseScale = remember { mutableStateOf(1f) }
    val pulseAlpha = remember { mutableStateOf(0f) }

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

    LaunchedEffect(connected.value) {
        if (connected.value) {
            launch {
                while (connected.value) {
                    delay(1000)
                    if (connected.value) seconds.value++
                }
            }
        } else {
            seconds.value = 0
        }
    }

    val timerText = "${seconds.value / 60}:${seconds.value % 60}".padStart(2, '0')

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
            vpnManager.stop(); connected.value = false; seconds.value = 0; serverIp.value = null
            status.value = "Готов к подключению"
        } else {
            val s = selectedProfile.value
            if (s != null) {
                val ok = vpnManager.start(s.outboundJson)
                if (ok) { connected.value = true; serverIp.value = s.host; status.value = "Подключён к ${s.host}" }
                else { status.value = "Ошибка подключения" }
            }
        }
    }

    fun connectTo(s: FoundServer) = scope.launch {
        selectedProfile.value = s; status.value = "Подключение…"
        val ok = vpnManager.start(s.outboundJson)
        if (ok) { connected.value = true; serverIp.value = s.host; status.value = "Подключён к ${s.host}" }
        else { connected.value = false; status.value = "Ошибка подключения" }
    }

    fun scan() = scope.launch {
        scanRunning.value = true
        val found = serverScanner.scan()
        withContext(Dispatchers.Main) { servers.clear(); servers.addAll(found); scanRunning.value = false }
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
                    IconButton(onClick = { scan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сканировать", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = status.value, style = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(modifier = Modifier.height(16.dp))

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
                    Icon(
                        imageVector = Icons.Default.PowerOff,
                        contentDescription = if (connected.value) "Отключить" else "Подключить",
                        tint = Color(0xFF121212),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = timerText,
                style = TextStyle(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            serverIp.value?.let { ip ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "IP: $ip",
                    style = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { scan() },
                enabled = !scanRunning.value && !connected.value,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (scanRunning.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (scanRunning.value) "Сканирование…" else if (connected.value) "Подключено" else "Сканировать серверы")
                }
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

            // Передаем selectedProfile.value
            ServerList(servers = servers, selectedProfile = selectedProfile.value, onConnect = ::connectTo)
        }
    }
}