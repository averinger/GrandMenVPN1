package com.grandmenvpn.app

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.tokens.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.grandmenvpn.app.config.CustomServerManager
import com.grandmenvpn.app.config.SplitRouteManager
import com.grandmenvpn.app.config.WhitelistManager
import com.grandmenvpn.app.scanner.ServerScanner
import com.grandmenvpn.app.ui.screens.CustomServersScreen
import com.grandmenvpn.app.ui.screens.HomeScreen
import com.grandmenvpn.app.ui.screens.ManualInputScreen
import com.grandmenvpn.app.ui.screens.SettingsScreen
import com.grandmenvpn.app.ui.screens.WhitelistScreen
import com.grandmenvpn.app.ui.theme.GrandMenTheme
import com.grandmenvpn.app.ui.theme.GreenAccent
import com.grandmenvpn.app.ui.theme.SurfaceDark
import com.grandmenvpn.app.ui.theme.TextSecondaryDark
import com.grandmenvpn.app.vpn.VpnManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrandMenTheme {
                GrandMenApp()
            }
        }
    }
}

private enum class AppTab(
    val title: String,
    val icon: ImageVector
) {
    Home("Главная", Icons.Filled.Home),
    Manual("Ручной ввод", Icons.Filled.Edit),
    Servers("Мои серверы", Icons.Filled.Star),
    NoVpn("Без VPN", Icons.Filled.LockOpen),
    Settings("Настройки", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrandMenApp() {
    var currentTab by remember { mutableStateOf(AppTab.Home) }

    Scaffold(
        containerColor = SurfaceDark,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenAccent,
                            selectedTextColor = GreenAccent,
                            indicatorColor = SurfaceDark,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.Home -> HomeScreen(
                    vpnManager = remember { VpnManager(context) },
                    serverScanner = ServerScanner
                )
                AppTab.Manual -> ManualInputScreen(
                    manager = remember { CustomServerManager(context) }
                )
                AppTab.Servers -> CustomServersScreen(
                    manager = remember { CustomServerManager(context) }
                )
                AppTab.NoVpn -> WhitelistScreen(
                    whitelistManager = remember { WhitelistManager(context) },
                    splitRouteManager = SplitRouteManager
                )
                AppTab.Settings -> SettingsScreen(
                    whitelistManager = remember { WhitelistManager(context) },
                    splitRouteManager = SplitRouteManager,
                    vpnManager = remember { VpnManager(context) }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Text(text = title)
}
