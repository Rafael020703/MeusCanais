package com.meuscanais

import com.meuscanais.data.model.EpgResponse
import com.meuscanais.data.network.RetrofitClient
import org.junit.Test
import org.junit.Assert.*

class EpgParsingTest {
    @Test
    fun testEpgParsing() {
        val xml = """
            <tv>
                <channel id="canal-1">
                    <display-name>Globo HD</display-name>
                    <icon src="http://example.com/globo.png"/>
                </channel>
                <programme start="20260831180000 +0000" stop="20260831190000 +0000" channel="canal-1">
                    <title>Jornal Nacional</title>
                    <desc>Telejornal brasileiro.</desc>
                    <category>Jornalismo</category>
                    <category>Notícias</category>
                </programme>
            </tv>
        """.trimIndent()
        
        try {
            val response = RetrofitClient.xml.decodeFromString(EpgResponse.serializer(), xml)
            
            assertNotNull(response)
            assertEquals(1, response.channels?.size)
            assertEquals("canal-1", response.channels?.get(0)?.id)
            assertEquals("Globo HD", response.channels?.get(0)?.displayName)
            assertEquals("http://example.com/globo.png", response.channels?.get(0)?.icon?.src)
            
            assertEquals(1, response.programmes?.size)
            assertEquals("canal-1", response.programmes?.get(0)?.channelId)
            assertEquals("Jornal Nacional", response.programmes?.get(0)?.title)
            assertEquals(2, response.programmes?.get(0)?.categories?.size)
            assertEquals("Jornalismo", response.programmes?.get(0)?.categories?.get(0)?.name)
            
            println("EPG Parsing Real Success!")
        } catch (e: Exception) {
            println("EPG Parsing Real Failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
