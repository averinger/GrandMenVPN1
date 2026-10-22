package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.int
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrojanParserTest {

    private val parser = TrojanParser()

    @Test
    fun `разбирает trojan с tls и grpc`() {
        val link = "trojan://p%40ss@t.example.com:8443?type=grpc&serviceName=svc&sni=t.example.com&alpn=h2%2Chttp%2F1.1"
        val ob = firstNode(parser.parse(link))
        assertEquals("trojan", str(ob, "type"))
        assertEquals("t.example.com", str(ob, "server"))
        assertEquals(8443, int(ob, "server_port"))
        assertEquals("p@ss", str(ob, "password"))
        assertTrue(ob["tls"]!!.toString().contains("\"server_name\":\"t.example.com\""))
        assertTrue(ob["transport"]!!.toString().contains("\"service_name\":\"svc\""))
    }

    @Test
    fun `security none убирает tls блок`() {
        val ob = firstNode(parser.parse("trojan://pass@1.2.3.4:443?security=none"))
        assertTrue(ob["tls"] == null)
    }
}
