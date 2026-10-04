package rsv.squitv.data.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.*

@Serializable
@XmlSerialName("tv", "", "")
data class EpgResponse(
    @XmlElement(true)
    @XmlSerialName("channel", "", "")
    val channels: List<EpgChannel>? = null,
    @XmlElement(true)
    @XmlSerialName("programme", "", "")
    val programmes: List<EpgProgramme>? = null
)

@Serializable
@XmlSerialName("channel", "", "")
data class EpgChannel(
    @XmlElement(false)
    val id: String,
    @XmlElement(true)
    @XmlSerialName("display-name", "", "")
    val displayName: String? = null,
    @XmlElement(true)
    val icon: EpgIcon? = null
)

@Serializable
@XmlSerialName("icon", "", "")
data class EpgIcon(
    @XmlElement(false)
    val src: String? = null
)

@Serializable
@XmlSerialName("programme", "", "")
data class EpgProgramme(
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
    val categories: List<EpgCategory>? = null
)

@Serializable
@XmlSerialName("category", "", "")
data class EpgCategory(
    @XmlValue
    val name: String? = null
)
