package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Общий сборщик transport-блоков sing-box для ws/grpc/httpupgrade/http.
 */
internal fun transport(
    network: String,
    params: Map<String, String>,
    host: String,
    sni: String
): JsonObject? = when (network) {
    "tcp", "raw", "none", "" -> null
    "ws" -> buildJsonObject {
        put("type", JsonPrimitive("ws"))
        put("path", JsonPrimitive(queryParam(params, "path").ifEmpty { "/" }))
        if (host.isNotEmpty()) {
            putJsonObject("headers") { put("Host", JsonPrimitive(host)) }
        }
    }
    "grpc" -> buildJsonObject {
        put("type", JsonPrimitive("grpc"))
        put("service_name", JsonPrimitive(queryParam(params, "serviceName")))
    }
    "httpupgrade" -> buildJsonObject {
        put("type", JsonPrimitive("httpupgrade"))
        put("path", JsonPrimitive(queryParam(params, "path").ifEmpty { "/" }))
        if (host.isNotEmpty()) put("host", JsonPrimitive(host))
    }
    "h2", "http" -> buildJsonObject {
        put("type", JsonPrimitive("http"))
        put("path", JsonPrimitive(queryParam(params, "path").ifEmpty { "/" }))
        if (host.isNotEmpty()) {
            put("host", buildJsonArray { add(JsonPrimitive(host)) })
        }
    }
    else -> null
}

/** TLS-блок для trojan/hysteria2/tuic. */
internal fun tlsBlock(
    params: Map<String, String>,
    host: String,
    defaultAlpn: List<String> = emptyList()
): JsonObject = buildJsonObject {
    put("enabled", true)
    put("server_name", JsonPrimitive(queryParam(params, "sni").ifEmpty { host }))
    put("insecure", queryParamBool(params, "allowInsecure") ||
            queryParamBool(params, "insecure") || queryParamBool(params, "allow_insecure"))
    val alpn = queryParam(params, "alpn").takeIf { it.isNotEmpty() }
        ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: defaultAlpn
    if (alpn.isNotEmpty()) {
        put("alpn", buildJsonArray { alpn.forEach { add(JsonPrimitive(it)) } })
    }
    queryParam(params, "fp").takeIf { it.isNotEmpty() }?.let { fp ->
        putJsonObject("utls") {
            put("enabled", true)
            put("fingerprint", JsonPrimitive(fp))
        }
    }
}
