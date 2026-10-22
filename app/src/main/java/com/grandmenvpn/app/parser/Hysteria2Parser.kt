package com.grandmenvpn.app.parser

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI

import com.grandmenvpn.app.parser.decode
import com.grandmenvpn.app.parser.outboundJson
import com.grandmenvpn.app.parser.queryParam
import com.grandmenvpn.app.parser.queryParams
import com.grandmenvpn.app.parser.tlsBlock

/**
 * hysteria2://password@host:port?obfs=&obfs-password=&sni=&insecure=&alpn=
 * (алиас hy2://)
 */
class Hysteria2Parser : ConfigParser {

    override fun supports(input: String): Boolean {
        val low = input.trim().lowercase()
        return low.startsWith("hysteria2://") || low.startsWith("hy2://")
    }

    override fun parse(input: String): String? {
        val raw = input.trim()
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it > 0 } ?: return null

        val params = queryParams(uri.rawQuery)
        var password = uri.rawUserInfo?.let { decode(it) }.orEmpty()
        if (uri.rawUserInfo?.contains(':') == true) {
            // user:pass -> склеиваем через двоеточие
            val rawInfo = uri.rawUserInfo
            password = decode(rawInfo.substringBefore(':')) + ":" + decode(rawInfo.substringAfter(':'))
        }

        val outbound = buildJsonObject {
            put("type", jsonPrimitive("hysteria2"))
            put("tag", jsonPrimitive("proxy"))
            put("server", jsonPrimitive(host))
            put("server_port", port)
            put("password", jsonPrimitive(password))
            put("tls", tlsBlock(params, host))

            queryParam(params, "obfs").takeIf { it.isNotEmpty() }?.let { obfs ->
                putJsonObject("obfs") {
                    put("type", obfs)
                    put("password", queryParam(params, "obfs-password"))
                }
            }
        }
        return outboundJson(outbound)
    }
}
