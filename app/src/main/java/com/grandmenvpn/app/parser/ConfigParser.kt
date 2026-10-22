package com.grandmenvpn.app.parser

/**
 * Парсер конфигов. На входе — ссылка или содержимое файла,
 * на выходе — JSON-строка sing-box (outbounds/endpoints) либо null.
 */
interface ConfigParser {
    /** Подходит ли этот вход под данный протокол. */
    fun supports(input: String): Boolean

    /** Возвращает JSON sing-box или null, если разобрать не удалось. */
    fun parse(input: String): String?
}
