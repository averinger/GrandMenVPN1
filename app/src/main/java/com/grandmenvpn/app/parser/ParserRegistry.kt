package com.grandmenvpn.app.parser

/**
 * Реестр парсеров: перебирает по очереди и отдаёт первый, кто разобрал вход.
 */
object ParserRegistry {

    val parsers: List<ConfigParser> = listOf(
        VlessParser(),
        VmessParser(),
        TrojanParser(),
        ShadowsocksParser(),
        Hysteria2Parser(),
        TuicParser(),
        AwgParser(),
        CustomJsonParser()
    )

    /** Возвращает JSON sing-box или null. */
    fun parse(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        for (parser in parsers) {
            if (!parser.supports(trimmed)) continue
            val result = runCatching { parser.parse(trimmed) }.getOrNull()
            if (!result.isNullOrEmpty()) return result
        }
        return null
    }

    /** Имя протокола для отображения. */
    fun protocolName(input: String): String {
        val trimmed = input.trim().lowercase()
        return when {
            trimmed.startsWith("vless://") -> "VLESS"
            trimmed.startsWith("vmess://") -> "VMess"
            trimmed.startsWith("trojan://") -> "Trojan"
            trimmed.startsWith("ss://") -> "Shadowsocks"
            trimmed.startsWith("hysteria2://") || trimmed.startsWith("hy2://") -> "Hysteria2"
            trimmed.startsWith("tuic://") -> "TUIC"
            trimmed.contains("[interface]") || trimmed.contains("[peer]") -> "AmneziaWG / WARP"
            trimmed.startsWith("{") || trimmed.startsWith("[") -> "Пользовательский"
            else -> "Неизвестный"
        }
    }
}
