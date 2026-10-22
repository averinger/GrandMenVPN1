package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.text.Charsets
import java.util.Base64

class ShadowsocksParser : ConfigParser {

    override fun supports(input: String): Boolean = input.trim().lowercase().startsWith("ss://")

    override fun parse(input: String): String? {
        var body = input.trim().substringAfter("ss://").substringBefore('#')
        if (body.isEmpty()) return null

        if (!body.substringBefore('?').contains('@')) {
            val decoded = runCatching { String(Base64.getMimeDecoder().decode(body), Charsets.UTF_8) }
                .getOrNull()
            if (decoded != null && decoded.contains('@')) {
                body = decoded
            }
        }

        val query = body.substringAfter('?', "")
        if (queryParams(query).containsKey("plugin")) return null
        val bare = body.substringBefore('?')

        val method: String
        val password: String
        val host: String
        val port: Int

        val at = bare.lastIndexOf('@')
        if (at > 0) {
            val userInfo = bare.substring(0, at)
            val hostPort = bare.substring(at + 1)
            val hp = splitHostPort(hostPort) ?: return null
            host = hp.first
            port = hp.second

            val colon = userInfo.indexOf(':')
            if (colon <= 0) return null
            val rawMethod = userInfo.substring(0, colon)
            val rawPass = userInfo.substring(colon + 1)
            if (rawPass.isEmpty()) {
                val dec = runCatching {
                    String(Base64.getMimeDecoder().decode(rawMethod), Charsets.UTF_8)
                }.getOrNull() ?: return null
                val c2 = dec.indexOf(':')
                if (c2 <= 0) return null
                method = dec.substring(0, c2)
                password = dec.substring(c2 + 1)
            } else {
                method = decode(rawMethod)
                password = decode(rawPass)
            }
        } else {
            val parts = bare.split(':')
            if (parts.size < 4) return null
            port = parts[parts.size - 1].toIntOrNull() ?: return null
            host = parts[parts.size - 2]
            method = parts.first()
            password = parts.subList(1, parts.size - 2).joinToString(":")
        }

        if (method.isEmpty() || host.isEmpty() || port <= 0) return null

        val outbound = buildJsonObject {
            put("type", JsonPrimitive("shadowsocks"))
            put("tag", JsonPrimitive("proxy"))
            put("server", JsonPrimitive(host))
            put("server_port", port)
            put("method", JsonPrimitive(method))
            put("password", JsonPrimitive(password))
        }
        return outboundJson(outbound)
    }

    private fun splitHostPort(hostPort: String): Pair<String, Int>? {
        val idx = hostPort.lastIndexOf(':')
        if (idx <= 0) return null
        val host = hostPort.substring(0, idx).removePrefix("[").removeSuffix("]")
        val port = hostPort.substring(idx + 1).toIntOrNull() ?: return null
        return host to port
    }
}