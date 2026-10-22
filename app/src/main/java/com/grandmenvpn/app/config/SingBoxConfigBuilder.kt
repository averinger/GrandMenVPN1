package com.grandmenvpn.app.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.parseToJsonElement
import kotlinx.serialization.json.put

/**
 * Сборщик финального конфига sing-box 1.11+ со сплит-туннелингом.
 *
 * Порядок правил route (первое совпадение выигрывает):
 *   1. Whitelist (домены из whitelist.txt) -> direct
 *   2. RU-суффиксы (.ru, .рф, .su) -> direct
 *   3. RU-домены (список 50+) -> direct
 *   4. geoip-ru.srs (rule_set) -> direct
 *   5. Остальное -> через прокси
 */
object SingBoxConfigBuilder {
    private val json = Json { ignoreUnknownKeys = true }

    const val PROXY_TAG = "proxy"
    const val DIRECT_TAG = "direct"

    /** RU-домены для быстрого локального сплита без geoip-файла (55 шт.). */
    val defaultRuDomains = listOf(
        "yandex.ru", "ya.ru", "yandex.by", "yandex.kz", "yandex.ua",
        "mail.ru", "list.ru", "bk.ru", "inbox.ru",
        "vk.com", "vk.me", "ok.ru", "odnoklassniki.ru", "rutube.ru",
        "yandex.net", "yastatic.net", "yandex.st",
        "2gis.ru", "avito.ru", "cian.ru", "drom.ru", "auto.ru",
        "kwork.ru", "habr.com", "geekbrains.ru", "stepik.org",
        "rbc.ru", "kommersant.ru", "vedomosti.ru", "gazeta.ru", "lenta.ru",
        "tass.ru", "ria.ru", "aif.ru",
        "mos.ru", "gov.ru", "kremlin.ru", "gosuslugi.ru", "nalog.ru", "pfrf.ru",
        "rt.ru", "mts.ru", "megafon.ru", "tele2.ru", "beeline.ru", "yota.ru",
        "sber.ru", "sberbank.ru", "tinkoff.ru", "vtb.ru", "alfa-bank.ru",
        "raiffeisen.ru", "ozon.ru", "wildberries.ru", "market.yandex.ru"
    )

    val defaultRuSuffixes = listOf(".ru", ".рф", ".su")

    fun build(
        proxyOutboundJson: String,
        ruDomains: List<String> = defaultRuDomains,
        ruSuffixes: List<String> = defaultRuSuffixes,
        whitelist: List<String> = emptyList(),
        useGeoipRu: Boolean = true
    ): String {
        val proxyOutbound = extractProxyOutbound(proxyOutboundJson)

        val outbounds = buildJsonArray {
            add(proxyOutbound)
            add(buildJsonObject { put("type", JsonPrimitive("direct")); put("tag", JsonPrimitive(DIRECT_TAG)) })
        }

        val rules = buildJsonArray {
            if (whitelist.isNotEmpty()) add(rule(DIRECT_TAG, domain = whitelist))
            if (ruSuffixes.isNotEmpty()) add(rule(DIRECT_TAG, domainSuffix = ruSuffixes))
            if (ruDomains.isNotEmpty()) add(rule(DIRECT_TAG, domain = ruDomains))
            if (useGeoipRu) add(rule(DIRECT_TAG, ruleSet = "geoip-ru"))
            add(rule(PROXY_TAG)) // всё остальное -> через прокси
        }

        val ruleSets = buildJsonArray {
            if (useGeoipRu) add(buildJsonObject {
                put("tag", JsonPrimitive("geoip-ru"))
                put("type", JsonPrimitive("remote"))
                put("format", JsonPrimitive("srs"))
                put("url", JsonPrimitive("https://raw.githubusercontent.com/SagerNet/sing-geosite/master/rule-set/geoip-ru.srs"))
            })
        }

        val config = buildJsonObject {
            put("outbounds", outbounds)
            put("route", buildJsonObject { put("rules", rules) })
            if (useGeoipRu) put("rule_sets", ruleSets)
        }

        return json.encodeToString(JsonObject.serializer(), config)
    }

    private fun extractProxyOutbound(proxyOutboundJson: String): JsonElement {
        val root = json.parseToJsonElement(proxyOutboundJson)
        return when (root) {
            is JsonObject -> (root["outbounds"] as? JsonArray)
                ?.filterIsInstance<JsonObject>()
                ?.firstOrNull() ?: root
            is JsonArray -> root.filterIsInstance<JsonObject>().firstOrNull() ?: root
            else -> root
        }
    }

    private fun rule(
        outbound: String,
        domain: List<String>? = null,
        domainSuffix: List<String>? = null,
        ruleSet: String? = null
    ) = buildJsonObject {
        put("outbound", JsonPrimitive(outbound))
        domain?.let { put("domain", buildJsonArray { it.forEach { add(JsonPrimitive(it)) } }) }
        domainSuffix?.let { put("domain_suffix", buildJsonArray { it.forEach { add(JsonPrimitive(it)) } }) }
        ruleSet?.let { put("rule_set", JsonPrimitive(it)) }
    }
}
