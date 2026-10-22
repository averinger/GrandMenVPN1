package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

import com.grandmenvpn.app.parser.endpointJson

/**
 * Парсер конфига AmneziaWG (.conf) -> endpoint sing-box (type wireguard).
 * Формат:
 *   [Interface]
 *   PrivateKey = ...
 *   Address = 10.8.0.2/32
 *   DNS = 1.1.1.1
 *   [Peer]
 *   PublicKey = ...
 *   Endpoint = host:port
 *   AllowedIPs = 0.0.0.0/0, ::/0
 */
class AwgParser : ConfigParser {

    override fun supports(input: String): Boolean =
        input.contains("[Interface]", ignoreCase = true) || input.contains("[Peer]", ignoreCase = true)

    override fun parse(input: String): String? {
        val interfaceSection = mutableMapOf<String, String>()
        val peerSection = mutableMapOf<String, String>()

        var section = ""
        for (rawLine in input.removePrefix("﻿").lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue
            if (line.startsWith("[")) {
                section = line.removeSurrounding("[", "]").trim().lowercase()
                continue
            }
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val key = line.substring(0, eq).trim().lowercase()
            val value = line.substring(eq + 1).substringBefore('#').trim()
            when (section) {
                "interface" -> interfaceSection[key] = value
                "peer" -> peerSection[key] = value
            }
        }

        val privateKey = interfaceSection["privatekey"] ?: return null
        val publicKey = peerSection["publickey"] ?: return null
        val endpoint = peerSection["endpoint"] ?: return null
        if (!endpoint.contains(':')) return null

        val host: String
        val port: Int
        val colon = endpoint.lastIndexOf(':')
        host = endpoint.substring(0, colon).removePrefix("[").removeSuffix("]")
        port = endpoint.substring(colon + 1).toIntOrNull() ?: return null

        val addresses = interfaceSection["address"]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { normalizeCidr(it) }
            ?.filter { it.contains(':') }
            ?.ifEmpty { listOf("10.8.0.2/128") }
            ?: listOf("10.8.0.2/128")

        val allowedIps = peerSection["allowedips"]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: listOf("0.0.0.0/0", "::/0")

        val endpointJson = buildJsonObject {
            put("type", jsonPrimitive("wireguard"))
            put("tag", jsonPrimitive("proxy"))
            put("mtu", interfaceSection["mtu"]?.toIntOrNull() ?: 1408)
            put("private_key", jsonPrimitive(privateKey))
            put("address", buildJsonArray { addresses.forEach { add(jsonPrimitive(it)) } })
            put("peers", buildJsonArray {
                add(buildJsonObject {
                    put("address", jsonPrimitive(host))
                    put("port", port)
                    put("public_key", jsonPrimitive(publicKey))
                    put("allowed_ips", buildJsonArray { allowedIps.forEach { add(jsonPrimitive(it)) } })
                })
            })
        }
        return endpointJson(endpointJson)
    }

    private fun normalizeCidr(value: String): String {
        if (value.contains('/')) return value
        return if (value.contains(':')) "$value/128" else "$value/32"
    }
}
