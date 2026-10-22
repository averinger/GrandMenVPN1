package com.grandmenvpn.app.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

val configJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

@Serializable
data class CustomServer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val config: String,
    val outboundJson: String,
    val addedAt: Long = System.currentTimeMillis(),
    val enabled: Boolean = true
)

/**
 * Хранилище кастомных серверов в файле custom_servers.json (внутреннее хранилище приложения).
 */
class CustomServerManager(private val context: Context) {

    private val file = File(context.filesDir, "custom_servers.json")
    private val listSerializer = ListSerializer(CustomServer.serializer())

    suspend fun save(servers: List<CustomServer>) {
        val content = configJson.encodeToString(listSerializer, servers)
        withContext(Dispatchers.IO) { file.writeText(content) }
    }

    suspend fun load(): List<CustomServer> = withContext(Dispatchers.IO) {
        if (!file.exists()) emptyList()
        else {
            val text = file.readText().trim()
            if (text.isEmpty() || !validate(text)) emptyList()
            else configJson.decodeFromString(listSerializer, text)
        }
    }

    /** Валидация: файл должен быть JSON-массивом. */
    fun validate(jsonText: String): Boolean =
        runCatching { configJson.parseToJsonElement(jsonText) }
            .map { it is kotlinx.serialization.json.JsonArray }
            .getOrDefault(false)

    suspend fun add(server: CustomServer) {
        val current = load()
        save(current + server)
    }

    suspend fun remove(id: String) {
        val current = load()
        save(current.filter { it.id != id })
    }
}
