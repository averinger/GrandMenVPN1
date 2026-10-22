package com.grandmenvpn.app.parser

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Принимает готовый конфиг sing-box (JSON) и оставляет в нём только прокси-часть:
 * первый outbound (кроме direct/block) становится тегом "proxy".
 * Поддерживает и полный конфиг, и кусок с одним outbound.
 */
class CustomJsonParser : ConfigParser {

    override fun supports(input: String): Boolean {
        val trimmed = input.trim()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return false
        return runCatching { json.parseToJsonElement(trimmed) }.isSuccess
    }

    override fun parse(input: String): String? {
        val element = runCatching { json.parseToJsonElement(input.trim()) }.getOrNull() ?: return null

        val root = when (element) {
            is JsonObject -> element
            is JsonArray -> JsonObject(mapOf("outbounds" to element))
            else -> return null
        }

        val outbounds = root["outbounds"] as? JsonArray
        if (outbounds != null) {
            val proxy = outbounds
                .filterIsInstance<JsonObject>()
                .firstOrNull { ob ->
                    val type = ob["type"]?.toString()?.trim('"') ?: ""
                    type != "direct" && type != "block" && type != "dns"
                }
                ?: return null
            return outboundJson(proxy.copyWithTag("proxy"))
        }

        val endpoints = root["endpoints"] as? JsonArray
        if (endpoints != null) {
            val endpoint = endpoints.filterIsInstance<JsonObject>().firstOrNull() ?: return null
            return endpointJson(endpoint.copyWithTag("proxy"))
        }

        // одиночный outbound-объект
        val type = root["type"]?.toString()?.trim('"')
        if (!type.isNullOrEmpty() && type != "direct" && type != "block") {
            return outboundJson(root.copyWithTag("proxy"))
        }
        return null
    }

    private fun JsonObject.copyWithTag(tag: String): JsonObject {
        val map = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>(this)
        map["tag"] = kotlinx.serialization.json.JsonPrimitive(tag)
        return JsonObject(map)
    }
}
