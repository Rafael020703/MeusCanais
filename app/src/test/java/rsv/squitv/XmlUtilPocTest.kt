package rsv.squitv

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.*
import org.junit.Test
import org.junit.Assert.*

@Serializable
@XmlSerialName("tv", "", "")
data class EpgResponsePoc(
    @XmlElement(true)
    @XmlSerialName("channel", "", "")
    val channels: List<EpgChannelPoc> = emptyList(),
    @XmlElement(true)
    @XmlSerialName("programme", "", "")
    val programmes: List<EpgProgrammePoc> = emptyList()
)

@Serializable
@XmlSerialName("channel", "", "")
data class EpgChannelPoc(
    @XmlElement(false)
    val id: String,
    @XmlElement(true)
    @XmlSerialName("display-name", "", "")
    val displayName: String? = null,
    @XmlElement(true)
    val icon: EpgIconPoc? = null
)

@Serializable
@XmlSerialName("icon", "", "")
data class EpgIconPoc(
    @XmlElement(false)
    val src: String? = null
)

@Serializable
@XmlSerialName("programme", "", "")
data class EpgProgrammePoc(
    @XmlElement(false)
    val start: String,
    @XmlElement(false)
    val stop: String,
    @XmlElement(false)
    @XmlSerialName("channel", "", "")
    val channelId: String,
    @XmlElement(true)
    val title: String? = null,
    @XmlElement(true)
    @XmlSerialName("desc", "", "")
    val description: String? = null,
    @XmlElement(true)
    @XmlSerialName("category", "", "")
    val categories: List<EpgCategoryPoc> = emptyList()
)

@Serializable
@XmlSerialName("category", "", "")
data class EpgCategoryPoc(
    @XmlValue
    val name: String? = null
)

class XmlUtilPocTest {
    @Test
    fun testXmlUtilParsing() {
        val xml = """
            <tv>
                <channel id="test-1">
                    <display-name>Test Channel 1</display-name>
                    <icon src="http://logo.com/1.png"/>
                </channel>
                <programme start="20260831180000 +0000" stop="20260831190000 +0000" channel="test-1">
                    <title>Test Show</title>
                    <desc>Test Description</desc>
                    <category>Movie</category>
                    <category>Action</category>
                </programme>
            </tv>
        """.trimIndent()
        
        val xmlConfig = XML {
            // isStrict = false // Try this if supported
        }
        
        try {
            val response = xmlConfig.decodeFromString(EpgResponsePoc.serializer(), xml)
            
            assertNotNull(response)
            assertEquals(1, response.channels.size)
            assertEquals("test-1", response.channels[0].id)
            assertEquals("Test Channel 1", response.channels[0].displayName)
            assertEquals("http://logo.com/1.png", response.channels[0].icon?.src)
            
            assertEquals(1, response.programmes.size)
            assertEquals("test-1", response.programmes[0].channelId)
            assertEquals("Test Show", response.programmes[0].title)
            assertEquals(2, response.programmes[0].categories.size)
            assertEquals("Movie", response.programmes[0].categories[0].name)
            
            println("XmlUtil POC Success!")
        } catch (e: Exception) {
            println("XmlUtil POC Failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
