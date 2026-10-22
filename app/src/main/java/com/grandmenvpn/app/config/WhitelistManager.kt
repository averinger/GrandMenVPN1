package com.grandmenvpn.app.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Хранилище «белого списка» (сайты без VPN) в файле whitelist.txt
 * (внутреннее хранилище приложения).
 */
class WhitelistManager(private val context: Context) {

    private val file = File(context.filesDir, "whitelist.txt")

    suspend fun load(): List<String> = withContext(Dispatchers.IO) {
        if (!file.exists()) emptyList()
        else file.readText().lines().map { it.trim().lowercase() }.filter { it.isNotBlank() }
    }

    suspend fun save(domains: List<String>) = withContext(Dispatchers.IO) {
        file.writeText(domains.distinct().sorted().joinToString("\n"))
    }

    suspend fun addDomain(domain: String) {
        val normalized = domain.trim().lowercase()
        if (normalized.isBlank()) return
        val current = load()
        if (normalized !in current) save(current + normalized)
    }

    suspend fun removeDomain(domain: String) {
        val normalized = domain.trim().lowercase()
        val current = load()
        save(current.filter { it != normalized })
    }
}
