package com.grandmenvpn.app.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Общие хелперы для тестов парсеров. */
object ParserTestSupport {

    fun parseObj(result: String?): JsonObject {
        assertNotNull(result, "парсер должен вернуть JSON")
        return Json.parseToJsonElement(result!!).jsonObject
    }

    /** Первый outbound (или endpoint) из результата. */
    fun firstNode(result: String?): JsonObject {
        val root = parseObj(result)
        val outbounds = root["outbounds"]?.let { Json.parseToJsonElement(it.toString()) }
        if (outbounds != null) {
            return (outbounds as kotlinx.serialization.json.JsonArray).first().jsonObject
        }
        val endpoints = root["endpoints"]?.let { Json.parseToJsonElement(it.toString()) }
        return (endpoints as kotlinx.serialization.json.JsonArray).first().jsonObject
    }

    fun str(obj: JsonObject, key: String): String? =
        obj[key]?.jsonPrimitive?.content

    fun int(obj: JsonObject, key: String): Int? =
        obj[key]?.jsonPrimitive?.content?.toIntOrNull()

    fun assertNotNull(value: Any?, message: String) {
        if (value == null) throw AssertionError(message)
    }
}
