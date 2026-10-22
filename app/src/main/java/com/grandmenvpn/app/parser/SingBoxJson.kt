package com.grandmenvpn.app.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.text.Charsets
import java.net.URLDecoder
import java.nio.charset.Charsets

internal val json = Json {
    prettyPrint = false
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Разбирает query-строку URI в карту. Значения декодируются,
 * повторяющиеся ключи берутся по первому вхождению.
 */
fun queryParams(rawQuery: String?): Map<String, String> {
    if (rawQuery.isNullOrEmpty()) return emptyMap()
    val out = LinkedHashMap<String, String>()
    for (pair in rawQuery.split('&')) {
        if (pair.isEmpty()) continue
        val idx = pair.indexOf('=')
        val key = if (idx < 0) pair else pair.substring(0, idx)
        val value = if (idx < 0) "" else pair.substring(idx + 1)
        if (key in out) continue
        out[decode(key)] = decode(value)
    }
    return out
}

fun decode(s: String): String = try {
    URLDecoder.decode(s, Charsets.UTF_8.name())
} catch (_: Exception) {
    s
}

fun queryParam(params: Map<String, String>, key: String, default: String = ""): String =
    params[key]?.takeIf { it.isNotEmpty() } ?: default

fun queryParamBool(params: Map<String, String>, key: String): Boolean =
    queryParam(params, key).lowercase() in setOf("1", "true", "yes", "on")

/** Собирает конфиг sing-box с одним outbound. */
fun outboundJson(outbound: JsonObject): String = buildJsonObject {
    put("outbounds", buildJsonArray { add(outbound) })
}.let { json.encodeToString(JsonObject.serializer(), it) }

/** Собирает конфиг sing-box с одним endpoint (sing-box 1.11+). */
fun endpointJson(endpoint: JsonObject): String = buildJsonObject {
    put("endpoints", buildJsonArray { add(endpoint) })
}.let { json.encodeToString(JsonObject.serializer(), it) }

fun JsonObject.toJsonString(): String = json.encodeToString(JsonObject.serializer(), this)

fun JsonArray.toJsonString(): String = json.encodeToString(JsonArray.serializer(), this)

internal fun JsonElement.asStringOrNull(): String? =
    if (this is kotlinx.serialization.json.JsonPrimitive && isString) content else null
