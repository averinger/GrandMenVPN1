package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomJsonParserTest {

    private val parser = CustomJsonParser()

    @Test
    fun `берёт первый outbound и ставит тег proxy`() {
        val cfg = """
            {
              "outbounds": [
                {"type": "vless", "tag": "my-node", "server": "1.2.3.4", "server_port": 443, "uuid": "u"},
                {"type": "direct", "tag": "direct"}
              ]
            }
        """.trimIndent()
        val ob = firstNode(parser.parse(cfg))
        assertEquals("vless", str(ob, "type"))
        assertEquals("proxy", str(ob, "tag"))
        assertEquals("1.2.3.4", str(ob, "server"))
    }

    @Test
    fun `поддерживает одиночный outbound объект`() {
        val ob = firstNode(parser.parse("""{"type":"trojan","server":"t.com","server_port":443,"password":"p"}"""))
        assertEquals("trojan", str(ob, "type"))
        assertEquals("proxy", str(ob, "tag"))
    }

    @Test
    fun `поддерживает endpoints`() {
        val ob = firstNode(parser.parse("""{"endpoints":[{"type":"wireguard","tag":"wg","private_key":"k"}]}"""))
        assertEquals("wireguard", str(ob, "type"))
        assertEquals("proxy", str(ob, "tag"))
    }

    @Test
    fun `возвращает null для невалидного json`() {
        assertNull(parser.parse("{не json"))
        assertNull(parser.parse("просто текст"))
    }
}
