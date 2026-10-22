package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserRegistryTest {

    @Test
    fun `определяет протокол и разбирает каждый тип`() {
        val cases = mapOf(
            "vless://uuid@1.2.3.4:443" to "vless",
            "trojan://p@1.2.3.4:443" to "trojan",
            "ss://aes-256-gcm:p@1.2.3.4:8388" to "shadowsocks",
            "hysteria2://p@1.2.3.4:443" to "hysteria2",
            "tuic://uuid:p@1.2.3.4:443" to "tuic"
        )
        for ((link, type) in cases) {
            val result = ParserRegistry.parse(link)
            assertNotNull("не разобрано: $link", result)
            assertEquals(type, str(firstNode(result), "type"))
        }
    }

    @Test
    fun `определяет имя протокола`() {
        assertEquals("VLESS", ParserRegistry.protocolName("vless://x@1.2.3.4:443"))
        assertEquals("AmneziaWG / WARP", ParserRegistry.protocolName("[Interface]\nPrivateKey = k\n"))
        assertEquals("Пользовательский", ParserRegistry.protocolName("{\"outbounds\":[]}"))
        assertEquals("Неизвестный", ParserRegistry.protocolName("привет"))
    }

    @Test
    fun `пустой ввод даёт null`() {
        assertNull(ParserRegistry.parse(""))
        assertNull(ParserRegistry.parse("   "))
    }
}
