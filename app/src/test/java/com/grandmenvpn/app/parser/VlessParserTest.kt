package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VlessParserTest {

    private val parser = VlessParser()

    @Test
    fun `разбирает простой vless без tls`() {
        val result = parser.parse("vless://11111111-2222-3333-4444-555555555555@1.2.3.4:443")
        val ob = firstNode(result)
        assertEquals("vless", str(ob, "type"))
        assertEquals("proxy", str(ob, "tag"))
        assertEquals("1.2.3.4", str(ob, "server"))
        assertEquals(443, ParserTestSupport.int(ob, "server_port"))
        assertEquals("11111111-2222-3333-4444-555555555555", str(ob, "uuid"))
        assertTrue(str(ob, "tls") == null)
    }

    @Test
    fun `разбирает vless reality с ws транспортом`() {
        val link = "vless://uuid-1@example.com:443" +
            "?security=reality&type=ws&path=%2Fws&host=cdn.example.com" +
            "&sni=example.com&pbk=PBK&sid=SID&fp=chrome&flow=xtls-rprx-vision"
        val ob = firstNode(parser.parse(link))
        assertEquals("vless", str(ob, "type"))
        assertEquals("xtls-rprx-vision", str(ob, "flow"))

        val tls = ob["tls"]!!.toString()
        assertTrue(tls.contains("\"public_key\":\"PBK\""))
        assertTrue(tls.contains("\"short_id\":\"SID\""))

        val transport = ob["transport"]!!.toString()
        assertTrue(transport.contains("\"type\":\"ws\""))
        assertTrue(transport.contains("/ws"))
    }

    @Test
    fun `возвращает null на мусоре`() {
        assertEquals(null, parser.parse("vless://"))
        assertEquals(null, parser.parse("не ссылка"))
    }
}
