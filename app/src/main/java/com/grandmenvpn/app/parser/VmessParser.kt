package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.util.Base64

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
            put("type", "vmess")
            put("tag", "proxy")
            put("server", host)
            put("server_port", port)
            put("uuid", id)
            put("alter_id", data.str("aid")?.toIntOrNull() ?: 0)
            put("security", data.str("scy") ?: "auto")

            if (data.str("tls") == "tls") {
                putJsonObject("tls") {
                    put("enabled", true)
                    put("server_name", sni)
                    put("insecure", false)
                    data.str("alpn")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { alpn -> put("alpn", kotlinx.serialization.json.buildJsonArray { alpn.forEach { add(it) } }) }
                    data.str("fp")?.takeIf { it.isNotEmpty() }?.let { fp ->
                        putJsonObject("utls") {
                            put("enabled", true)
                            put("fingerprint", fp)
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
