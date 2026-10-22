package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.int
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class VmessParserTest {

    private val parser = VmessParser()

    private fun encode(json: String): String =
        Base64.getEncoder().encodeToString(json.toByteArray())

    @Test
    fun `разбирает vmess из base64 json`() {
        val payload = encode(
            """{"v":"2","ps":"node","add":"5.6.7.8","port":"8080","id":"uuid-9","aid":"0","scy":"auto","net":"ws","path":"/vm","host":"h.example.com","tls":"tls","sni":"s.example.com"}"""
        )
        val ob = firstNode(parser.parse("vmess://$payload"))
        assertEquals("vmess", str(ob, "type"))
        assertEquals("5.6.7.8", str(ob, "server"))
        assertEquals(8080, int(ob, "server_port"))
        assertEquals("uuid-9", str(ob, "uuid"))
        assertEquals(0, int(ob, "alter_id"))
        assertEquals("auto", str(ob, "security"))
        assertTrue(ob["transport"]!!.toString().contains("\"type\":\"ws\""))
    }

    @Test
    fun `возвращает null если нет id`() {
        val payload = encode("""{"add":"1.1.1.1","port":443}""")
        assertEquals(null, parser.parse("vmess://$payload"))
    }
}
