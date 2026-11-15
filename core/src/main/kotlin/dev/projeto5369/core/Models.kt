package dev.projeto5369.core

import kotlinx.serialization.Serializable

@Serializable
data class Video(
    val id: String, val title: String, val channel: String = "", val channelId: String? = null,
    val durationSeconds: Long? = null, val thumbnail: String = "", val description: String = "",
    val isShort: Boolean = false,
)
@Serializable
data class Channel(val id: String, val title: String, val description: String = "")
data class Page<T>(val items: List<T>, val next: Cursor? = null)
data class Cursor(val token: String, val endpoint: String, val context: String)
data class ChannelPage(val channel: Channel, val videos: Page<Video>)
interface Catalog {
    suspend fun search(query: String): Page<Video>
    suspend fun next(cursor: Cursor): Page<Video>
    /** Vertical short videos for the Shorts area (kept separate from the long-video catalog). */
    suspend fun shorts(query: String): Page<Video>
    suspend fun video(id: String): Video
    suspend fun channel(id: String): ChannelPage
}
class CatalogException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { NETWORK, BLOCKED, RATE_LIMIT, SCHEMA, UNAVAILABLE }
}
@Serializable
data class Library(
    val history: List<Video> = emptyList(), val favorites: List<Video> = emptyList(),
    val recentSearches: List<String> = emptyList(), val minimumMinutes: Int = 20,
    val hideWatched: Boolean = false,
    /** Visual theme only: AMOLED black (false) or Onda Glass (true). */
    val glass: Boolean = false,
    /** Channels followed on this device only (not a YouTube subscription). */
    val channels: List<Channel> = emptyList(),
) {
    fun visible(videos: List<Video>): List<Video> = videos.filter { video ->
        !video.isShort && (minimumMinutes == 0 || (video.durationSeconds ?: -1) >= minimumMinutes * 60L) &&
            (!hideWatched || history.none { it.id == video.id })
    }
    fun watched(video: Video) = copy(history = (listOf(video) + history.filterNot { it.id == video.id }).take(200))
    fun favorite(video: Video) = copy(favorites = if (favorites.any { it.id == video.id }) favorites.filterNot { it.id == video.id }
        else (listOf(video) + favorites).take(500))
    fun follow(channel: Channel) = copy(channels = if (channels.any { it.id == channel.id }) channels.filterNot { it.id == channel.id }
        else (listOf(channel.copy(description = "")) + channels).take(100))
    fun forget(query: String) = copy(recentSearches = recentSearches.filterNot { it == query })
    fun searched(query: String) = copy(recentSearches = (listOf(query.trim()) + recentSearches.filterNot { it == query.trim() }).filter { it.isNotBlank() }.take(20))
}
enum class PlaybackState { LOADING, READY, PLAYING, PAUSED, ENDED, ERROR }
fun playerState(code: Int): PlaybackState = when(code) {
    1 -> PlaybackState.PLAYING; 2 -> PlaybackState.PAUSED; 0 -> PlaybackState.ENDED
    5 -> PlaybackState.READY; else -> PlaybackState.LOADING
}
/** Public video IDs and regular YouTube links only; no arbitrary URL passed to WebView. */
fun videoId(input: String): String? {
    val raw = input.trim()
    if (Regex("[A-Za-z0-9_-]{11}").matches(raw)) return raw
    return runCatching {
        val uri = java.net.URI(raw)
        if (uri.scheme !in setOf("https", "http")) return null
        val host = uri.host?.lowercase()
        val id = when(host) {
            "youtu.be" -> uri.path.removePrefix("/").substringBefore('/')
            "youtube.com", "www.youtube.com", "m.youtube.com" -> if(uri.path == "/watch") {
                uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
            } else if(uri.path.startsWith("/embed/")) uri.path.removePrefix("/embed/").substringBefore('/') else null
            else -> null
        }
        id?.takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) }
    }.getOrNull()
}
