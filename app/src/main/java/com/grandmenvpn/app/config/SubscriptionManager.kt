package com.grandmenvpn.app.config

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.coroutine.CoroutineWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ListSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

val subJson = Json { ignoreUnknownKeys = true }

@Serializable
data class Subscription(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val lastUpdate: Long = 0,
    val servers: List<String> = emptyList()
)

/**
 * Хранилище подписок в файле subscriptions.json с периодическим обновлением через WorkManager.
 */
class SubscriptionManager(private val context: Context) {

    private val file = File(context.filesDir, "subscriptions.json")
    private val listSerializer = ListSerializer(Subscription.serializer())
    private val http = OkHttpClient()

    suspend fun save(subs: List<Subscription>) {
        val content = subJson.encodeToString(listSerializer, subs)
        withContext(Dispatchers.IO) { file.writeText(content) }
    }

    suspend fun load(): List<Subscription> = withContext(Dispatchers.IO) {
        if (!file.exists()) emptyList()
        else {
            val text = file.readText().trim()
            if (text.isEmpty()) emptyList()
            else subJson.decodeFromString(listSerializer, text)
        }
    }

    suspend fun add(url: String, name: String) {
        val current = load()
        save(current + Subscription(name = name, url = url))
    }

    /** Скачивает все подписки, парсит строки серверов и сохраняет результат. */
    suspend fun refreshAll(): List<Subscription> {
        val subs = load()
        val updated = mutableListOf<Subscription>()
        for (s in subs) {
            var servers = s.servers
            try {
                servers = withContext(Dispatchers.IO) {
                    http.newCall(Request.Builder().url(s.url).build()).execute().use { response ->
                        response.body?.string()?.lines()?.filter { it.isNotBlank() }?.toList() ?: emptyList()
                    }
                }
            } catch (_: Exception) { }
            updated += s.copy(lastUpdate = System.currentTimeMillis(), servers = servers)
        }
        save(updated)
        return updated
    }

    fun startPeriodic(intervalHours: Long = 6) {
        val request = PeriodicWorkRequest.Builder(
            SubscriptionUpdateWorker::class.java,
            intervalHours, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork("sub_update", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

class SubscriptionUpdateWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): androidx.work.Result {
        return try {
            SubscriptionManager(applicationContext).refreshAll()
            androidx.work.Result.success()
        } catch (_: Exception) {
            androidx.work.Result.failure()
        }
    }
}
