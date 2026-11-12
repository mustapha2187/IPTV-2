package com.musta.iptv.data

enum class SourceType(val key: String) {
    M3U("m3u"),
    XTREAM("xtream"),
    STALKER("stalker");

    companion object {
        fun from(key: String): SourceType? = entries.firstOrNull { it.key == key }
    }
}

/** What the phone form sends, before it is saved. */
data class SourceDraft(
    val type: SourceType,
    val name: String,
    val url: String,
    val username: String = "",
    val password: String = "",
    val mac: String = "",
)

data class Source(
    val id: Long,
    val type: SourceType,
    val name: String,
    val url: String,
    val username: String,
    val password: String,
    val mac: String,
    val loadedAt: Long,
    val channelCount: Int,
)

data class Category(val id: Long, val sourceId: Long, val name: String)

data class Channel(
    val id: Long,
    val sourceId: Long,
    val categoryId: Long,
    val number: Int,
    val name: String,
    val streamUrl: String,
    val logoUrl: String,
    val cmd: String,
    val isFavorite: Boolean,
)

/** One channel as read from a source, before it is stored. */
data class ChannelRow(
    val categoryName: String,
    val number: Int,
    val name: String,
    val streamUrl: String,
    val logoUrl: String,
    val tvgId: String,
    val cmd: String,
)

object Cat {
    const val ALL = -1L
    const val FAVORITES = -2L
}

/** How many servers of each type can be stored. Raise these numbers to allow more. */
object Limits {
    const val XTREAM = 30
    const val STALKER = 30
    const val M3U = 30

    fun forType(type: SourceType): Int = when (type) {
        SourceType.M3U -> M3U
        SourceType.XTREAM -> XTREAM
        SourceType.STALKER -> STALKER
    }
}
