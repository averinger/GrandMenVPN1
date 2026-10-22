package com.grandmenvpn.app.parser

import com.grandmenvpn.app.parser.ParserTestSupport.firstNode
import com.grandmenvpn.app.parser.ParserTestSupport.int
import com.grandmenvpn.app.parser.ParserTestSupport.str
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AwgParserTest {

    private val parser = AwgParser()

    private val conf = """
        [Interface]
        PrivateKey = PRIVKEY=
        Address = 10.8.0.2/32, fd00::2/128
        DNS = 1.1.1.1
        MTU = 1280

        [Peer]
        PublicKey = PUBKEY=
        Endpoint = warp.example.com:2408
        AllowedIPs = 0.0.0.0/0, ::/0
    """.trimIndent()

    @Test
    fun `разбирает awg conf в endpoint sing-box`() {
        val result = parser.parse(conf)
        val ob = firstNode(result)
        assertEquals("wireguard", str(ob, "type"))
        assertEquals("proxy", str(ob, "tag"))
        assertEquals("PRIVKEY=", str(ob, "private_key"))
        assertEquals(1280, int(ob, "mtu"))

        val peers = ob["peers"]!!.toString()
        assertTrue(peers.contains("\"address\":\"warp.example.com\""))
        assertTrue(peers.contains("\"port\":2408"))
        assertTrue(peers.contains("PUBKEY="))
    }

    @Test
    fun `endpoint оборачивается в массив endpoints`() {
        val result = parser.parse(conf)
        assertTrue(result!!.contains("\"endpoints\""))
        assertTrue(!result.contains("\"outbounds\""))
    }

    @Test
    fun `возвращает null если нет ключей`() {
        assertNull(parser.parse("[Interface]\nAddress = 10.8.0.2/32\n"))
    }
}
