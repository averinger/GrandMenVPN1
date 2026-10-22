package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.int
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class ShadowsocksParserTest {

    private val parser = ShadowsocksParser()

    @Test
    fun `разбирает новый формат ss с методом и паролем`() {
        val ob = firstNode(parser.parse("ss://aes-256-gcm:secret@1.2.3.4:8388"))
        assertEquals("shadowsocks", str(ob, "type"))
        assertEquals("1.2.3.4", str(ob, "server"))
        assertEquals(8388, int(ob, "server_port"))
        assertEquals("aes-256-gcm", str(ob, "method"))
        assertEquals("secret", str(ob, "password"))
    }

    @Test
    fun `разбирает старый формат ss с base64`() {
        // старый формат: ss://BASE64(method:pass@host:port)
        val body = Base64.getEncoder().encodeToString("chacha20-ietf-poly1305:pass@ss.example.com:9000".toByteArray())
        val ob = firstNode(parser.parse("ss://$body"))
        assertEquals("ss.example.com", str(ob, "server"))
        assertEquals(9000, int(ob, "server_port"))
        assertEquals("chacha20-ietf-poly1305", str(ob, "method"))
        assertEquals("pass", str(ob, "password"))
    }

    @Test
    fun `разбирает формат без собачки`() {
        val ob = firstNode(parser.parse("ss://aes-256-gcm:secret:1.2.3.4:8388"))
        assertEquals("1.2.3.4", str(ob, "server"))
        assertEquals(8388, int(ob, "server_port"))
        assertEquals("aes-256-gcm", str(ob, "method"))
        assertEquals("secret", str(ob, "password"))
    }

    @Test
    fun `отклоняет конфиги с плагином`() {
        assertNull(parser.parse("ss://aes-256-gcm:pass@1.2.3.4:8388?plugin=obfs-local%3Bobfs%3Dhttp"))
    }
}
