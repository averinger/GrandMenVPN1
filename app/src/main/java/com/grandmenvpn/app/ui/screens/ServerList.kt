package com.grandmenvpn.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.tokens.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grandmenvpn.app.scanner.FoundServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerList(
    servers: List<FoundServer>,
    selectedProfile: FoundServer?,
    onConnect: (FoundServer) -> Unit
) {
    val pingColor = { ping: Long ->
        when {
            ping <= 0 -> Color(0xFF9E9E9E)
            ping < 80 -> Color(0xFF00E676)
            ping < 200 -> Color(0xFFFFCA28)
            else -> Color(0xFFF44336)
        }
    }

    LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)) {
        items(servers, key = { it.host + it.port }) { s ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A).copy(alpha = 0.7f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "${s.host}:${s.port}", style = TextStyle(fontSize = 15.sp, color = Color(0xFFECECEC)))
                        Text(text = s.protocol, style = TextStyle(fontSize = 12.sp, color = Color(0xFF9E9E9E)))
                    }
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(pingColor(s.pingMs)))
                    Text(text = "${s.pingMs} мс", style = TextStyle(fontSize = 14.sp, color = Color(0xFFECECEC)), modifier = Modifier.padding(horizontal = 8.dp))
                    Button(onClick = { onConnect(s) }, modifier = Modifier.align(Alignment.CenterVertically),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color(0xFF121212))) {
                        Text("Подключить")
                    }
                }
            }
        }
    }
}
