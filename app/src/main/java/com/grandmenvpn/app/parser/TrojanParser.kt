package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI

import com.grandmenvpn.app.parser.decode
import com.grandmenvpn.app.parser.outboundJson
import com.grandmenvpn.app.parser.queryParam
import com.grandmenvpn.app.parser.queryParams
import com.grandmenvpn.app.parser.transport

/**
 * trojan://password@host:port?type=&sni=&alpn=&fp=&path=&serviceName=&allowInsecure=
 */
class TrojanParser : ConfigParser {

    override fun supports(input: String): Boolean = input.trim().lowercase().startsWith("trojan://")

    override fun parse(input: String): String? {
        val uri = runCatching { URI(input.trim()) }.getOrNull() ?: return null
        val password = uri.rawUserInfo?.let { decode(it) }.orEmpty()
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null
        if (password.isEmpty()) return null

        val params = queryParams(uri.rawQuery)
        val network = queryParam(params, "type", "tcp").lowercase()
        val sni = queryParam(params, "sni").ifEmpty { host }

        val outbound = buildJsonObject {
            put("type", JsonPrimitive("trojan"))
            put("tag", JsonPrimitive("proxy"))
            put("server", JsonPrimitive(host))
            put("server_port", port)
            put("password", JsonPrimitive(password))

            if (queryParam(params, "security", "tls") != "none") {
                put("tls", tlsBlock(params, host))
            }
            transport(network, params, queryParam(params, "host").ifEmpty { sni }, sni)
                ?.let { put("transport", it) }
        }
        return outboundJson(outbound)
    }
}
