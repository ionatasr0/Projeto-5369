package dev.projeto5369.core

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Live checks against the real YouTube site. Skipped unless run with -Pyt.live=true (CI only).
 * They prove that search, pagination, details and channel parsing work on the current public schema.
 */
class LiveYouTubeTest {
    private val live = System.getProperty("yt.live") == "true"
    private val catalog = YouTubeCatalog()

    private fun report(label: String, root: JsonObject) {
        val counts = mutableMapOf<String, Int>()
        YouTubeParser.objects(root).forEach { o ->
            o.keys.filter { it.endsWith("Renderer") || it.endsWith("ViewModel") }.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        }
        println("[$label] renderers: " + counts.entries.sortedByDescending { it.value }.take(25).joinToString { "${it.key}=${it.value}" })
    }

    @Test fun searchReturnsLongVideosAndPaginates() { runBlocking {
        assumeTrue(live)
        val first = catalog.search("documentário completo")
        println("[search] ${first.items.size} vídeos, próxima página: ${first.next != null}")
        first.items.take(5).forEach { println("  - ${it.id} | ${it.title} | ${it.channel} | ${it.durationSeconds}s | short=${it.isShort} | thumb=${it.thumbnail.take(60)}") }
        assertTrue(first.items.size >= 5, "A busca ao vivo trouxe poucos vídeos: ${first.items.size}")
        assertTrue(first.items.count { it.durationSeconds != null } >= 3, "Durações não foram lidas")
        assertTrue(first.items.all { it.title.isNotBlank() && it.thumbnail.startsWith("https://") })
        val cursor = assertNotNull(first.next, "Sem token de próxima página")
        val second = catalog.next(cursor)
        println("[search p2] ${second.items.size} vídeos")
        assertTrue(second.items.isNotEmpty(), "A segunda página veio vazia")
        assertTrue(second.items.map { it.id }.intersect(first.items.map { it.id }.toSet()).size < second.items.size)
    } }

    @Test fun detailsAndChannelLoad() { runBlocking {
        assumeTrue(live)
        val hit = catalog.search("palestra ciência").items.first { it.channelId != null }
        val details = catalog.video(hit.id)
        println("[details] ${details.id} | ${details.title} | ${details.channel} | ${details.durationSeconds}s | desc=${details.description.take(80)}")
        assertEquals(hit.id, details.id); assertTrue(details.title.isNotBlank())
        val channel = catalog.channel(hit.channelId!!)
        println("[channel] ${channel.channel.title} | ${channel.videos.items.size} vídeos | next=${channel.videos.next != null}")
        assertTrue(channel.videos.items.isNotEmpty(), "Canal sem vídeos")
    } }

    @Test fun shortsSearchIsReadable() { runBlocking {
        assumeTrue(live)
        val shorts = catalog.shorts("ônibus")
        println("[shorts] ${shorts.items.size} shorts")
        shorts.items.take(5).forEach { println("  - ${it.id} | ${it.title} | ${it.thumbnail.take(60)}") }
        assertTrue(shorts.items.size >= 3, "Poucos Shorts lidos: ${shorts.items.size}")
        assertTrue(shorts.items.all { it.isShort })
    } }

    @Test fun schemaReport() { runBlocking {
        assumeTrue(live)
        // Diagnostic only: which renderer types the current public pages use.
        val html = catalog.rawForDiagnostics("results", mapOf("search_query" to "documentário completo", "hl" to "pt", "gl" to "BR"))
        println("[results] html=${html.length} chars, ytInitialData=${html.contains("ytInitialData")}, context=${YouTubeParser.context(html) != null}")
        report("results", YouTubeParser.initial(html))
        val shortsHtml = catalog.rawForDiagnostics("results", mapOf("search_query" to "ônibus #shorts", "hl" to "pt", "gl" to "BR"))
        val root = YouTubeParser.initial(shortsHtml)
        report("results-shorts", root)
        YouTubeParser.objects(root).firstNotNullOfOrNull { it["shortsLockupViewModel"] as? JsonObject }?.let {
            println("[shortsLockupViewModel sample] " + it.toString().take(2500))
        }
        YouTubeParser.objects(root).firstNotNullOfOrNull { it["reelItemRenderer"] as? JsonObject }?.let {
            println("[reelItemRenderer sample] " + it.toString().take(2500))
        }
    } }
}
