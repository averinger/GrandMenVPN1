package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.text.Charsets
import java.net.URI
import java.nio.charset.Charsets
import java.util.Base64

// Import internal json instance from SingBoxJson.kt
import com.grandmenvpn.app.parser.json

/**
 * vmess://BASE64(JSON) — формат v2rayN.
 */
class VmessParser : ConfigParser {

    override fun supports(input: String): Boolean = input.trim().lowercase().startsWith("vmess://")

    override fun parse(input: String): String? {
        val raw = input.trim().substringAfter("vmess://").substringBefore('#')
        if (raw.isEmpty()) return null
        val decoded = runCatching { String(Base64.getMimeDecoder().decode(raw), Charsets.UTF_8) }
            .getOrNull() ?: return null
        val data = runCatching { json.parseToJsonElement(decoded) as? JsonObject }.getOrNull() ?: return null

        val host = data.str("add") ?: return null
        val id = data.str("id") ?: return null
        val port = data.str("port")?.toIntOrNull() ?: return null
        val network = (data.str("net") ?: "tcp").lowercase()
        val sni = data.str("sni") ?: data.str("host") ?: host

        val outbound = buildJsonObject {
            put("type", JsonPrimitive("vmess"))
            put("tag", JsonPrimitive("proxy"))
            put("server", JsonPrimitive(host))
            put("server_port", port)
            put("uuid", JsonPrimitive(id))
            put("alter_id", data.str("aid")?.toIntOrNull() ?: 0)
            put("security", JsonPrimitive(data.str("scy") ?: "auto"))

            if (data.str("tls") == "tls") {
                putJsonObject("tls") {
                    put("enabled", true)
                    put("server_name", JsonPrimitive(sni))
                    put("insecure", false)
                    data.str("alpn")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { alpn -> put("alpn", buildJsonArray { alpn.forEach { add(JsonPrimitive(it)) } }) }
                    data.str("fp")?.takeIf { it.isNotEmpty() }?.let { fp ->
                        putJsonObject("utls") {
                            put("enabled", true)
                            put("fingerprint", JsonPrimitive(fp))
                        }
                    }
                }
            }
            transport(network, emptyMap(), sni, sni)?.let { put("transport", it) }
        }
        return outboundJson(outbound)
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it.isNotEmpty() }
}
