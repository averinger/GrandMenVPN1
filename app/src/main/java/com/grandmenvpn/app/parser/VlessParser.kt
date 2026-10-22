package com.grandmenvpn.app.parser

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI

/**
 * vless://UUID@host:port?security=&type=&sni=&pbk=&sid=&fp=&flow=&alpn=&path=&serviceName=&allowInsecure=
 */
class VlessParser : ConfigParser {

    override fun supports(input: String): Boolean = input.trim().lowercase().startsWith("vless://")

    override fun parse(input: String): String? {
        val raw = input.trim()
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val uuid = uri.rawUserInfo?.substringBefore(':')?.let { decode(it) }.orEmpty()
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null
        if (uuid.isEmpty()) return null

        val params = queryParams(uri.rawQuery)
        val security = queryParam(params, "security", "none")
        val network = queryParam(params, "type", "tcp").lowercase()
        val sni = queryParam(params, "sni").ifEmpty { queryParam(params, "peer") }.ifEmpty { host }

        val outbound = buildJsonObject {
            put("type", jsonPrimitive("vless"))
            put("tag", jsonPrimitive("proxy"))
            put("server", jsonPrimitive(host))
            put("server_port", port)
            put("uuid", jsonPrimitive(uuid))

            queryParam(params, "flow").takeIf { it.isNotEmpty() }?.let { put("flow", jsonPrimitive(it)) }

            if (security == "tls" || security == "reality") {
                putJsonObject("tls") {
                    put("enabled", true)
                    put("server_name", jsonPrimitive(sni))
                    put("insecure", queryParamBool(params, "allowInsecure") ||
                            queryParamBool(params, "insecure") || queryParamBool(params, "allow_insecure"))
                    queryParam(params, "alpn").takeIf { it.isNotEmpty() }?.let { alpn ->
                        put("alpn", buildJsonArray { alpn.split(',').forEach { add(jsonPrimitive(it.trim())) } })
                    }
                    if (security == "reality") {
                        putJsonObject("reality") {
                            put("enabled", true)
                            put("public_key", jsonPrimitive(queryParam(params, "pbk")))
                            put("short_id", jsonPrimitive(queryParam(params, "sid")))
                        }
                        putJsonObject("utls") {
                            put("enabled", true)
                            put("fingerprint", jsonPrimitive(queryParam(params, "fp").ifEmpty { "chrome" }))
                        }
                    } else {
                        queryParam(params, "fp").takeIf { it.isNotEmpty() }?.let { fp ->
                            putJsonObject("utls") {
                                put("enabled", true)
                                put("fingerprint", jsonPrimitive(fp))
                            }
                        }
                    }
                }
            }

            transport(network, params, host, sni)?.let { put("transport", it) }
        }
        return outboundJson(outbound)
    }
}
