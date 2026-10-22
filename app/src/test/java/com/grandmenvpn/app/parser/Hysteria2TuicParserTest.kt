package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.int
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Hysteria2TuicParserTest {

    private val h2 = Hysteria2Parser()
    private val tuic = TuicParser()

    @Test
    fun `разбирает hysteria2 с обфускацией`() {
        val ob = firstNode(h2.parse("hysteria2://pw@h2.example.com:443?obfs=salamander&obfs-password=obfspw&sni=h2.example.com"))
        assertEquals("hysteria2", str(ob, "type"))
        assertEquals("h2.example.com", str(ob, "server"))
        assertEquals(443, int(ob, "server_port"))
        assertEquals("pw", str(ob, "password"))
        val obfs = ob["obfs"]!!.toString()
        assertTrue(obfs.contains("\"type\":\"salamander\""))
        assertTrue(obfs.contains("obfspw"))
    }

    @Test
    fun `алиас hy2 поддерживается`() {
        assertTrue(h2.supports("hy2://pw@h2.example.com:443"))
        assertEquals("hysteria2", str(firstNode(h2.parse("hy2://pw@h2.example.com:443")), "type"))
    }

    @Test
    fun `разбирает tuic с uuid и паролем`() {
        val ob = firstNode(tuic.parse("tuic://uuid-77:pass77@tuic.example.com:443?congestion_control=bbr&udp_relay_mode=quic&sni=tuic.example.com"))
        assertEquals("tuic", str(ob, "type"))
        assertEquals("uuid-77", str(ob, "uuid"))
        assertEquals("pass77", str(ob, "password"))
        assertEquals("bbr", str(ob, "congestion_control"))
        assertEquals("quic", str(ob, "udp_relay_mode"))
    }
}
