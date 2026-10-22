package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URI

import com.grandmenvpn.app.parser.decode
import com.grandmenvpn.app.parser.outboundJson
import com.grandmenvpn.app.parser.queryParam
import com.grandmenvpn.app.parser.queryParams
import com.grandmenvpn.app.parser.tlsBlock

/**
 * tuic://uuid:password@host:port?congestion_control=&udp_relay_mode=&sni=&alpn=&insecure=
 */
class TuicParser : ConfigParser {

    override fun supports(input: String): Boolean = input.trim().lowercase().startsWith("tuic://")

    override fun parse(input: String): String? {
        val uri = runCatching { URI(input.trim()) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null
        val rawInfo = uri.rawUserInfo ?: return null

        val uuid: String
        val password: String
        if (rawInfo.contains(':')) {
            uuid = decode(rawInfo.substringBefore(':'))
            password = decode(rawInfo.substringAfter(':'))
        } else {
            uuid = decode(rawInfo)
            password = ""
        }
        if (uuid.isEmpty()) return null

        val params = queryParams(uri.rawQuery)

        val outbound = buildJsonObject {
            put("type", JsonPrimitive("tuic"))
            put("tag", JsonPrimitive("proxy"))
            put("server", JsonPrimitive(host))
            put("server_port", port)
            put("uuid", JsonPrimitive(uuid))
            put("password", JsonPrimitive(password))
            put("tls", tlsBlock(params, host))

            queryParam(params, "congestion_control").takeIf { it.isNotEmpty() }
                ?.let { put("congestion_control", it) }
            queryParam(params, "udp_relay_mode").takeIf { it.isNotEmpty() }
                ?.let { put("udp_relay_mode", it) }
        }
        return outboundJson(outbound)
    }
}
