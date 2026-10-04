package rsv.squitv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
enum class ContentType {
    LIVE,
    MOVIE,
    SERIES,
    FAVORITE,
    HISTORY,
    DOWNLOAD;

    companion object {
        fun fromString(value: String?): ContentType {
            return when (value?.lowercase()) {
                "live" -> LIVE
                "movie", "vod" -> MOVIE
                "series" -> SERIES
                "favorite", "favorites" -> FAVORITE
                "history" -> HISTORY
                "download", "downloads" -> DOWNLOAD
                else -> LIVE // Default
            }
        }
    }

    override fun toString(): String {
        return name.lowercase()
    }
}
