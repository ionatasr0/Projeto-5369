package dev.projeto5369.core

import java.util.concurrent.TimeUnit
import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.Test

class CatalogTest {
    private fun fixture(name: String) = javaClass.getResource("/$name")!!.readText()
    private val context = """{"client":{"clientName":"WEB","clientVersion":"test-from-page"}}"""
    private fun html(data: String) = """<script>ytcfg.set({"INNERTUBE_CONTEXT":$context});var ytInitialData = $data;</script>"""
    @Test fun parsesSearchAndSkipsShorts() {
        val page=YouTubeParser.page(YouTubeParser.parse(fixture("search.json")),"search",YouTubeParser.parse(context))
        assertEquals(1,page.items.size); assertEquals(3723L,page.items.single().durationSeconds)
        assertEquals("Example Channel",page.items.single().channel)
        assertEquals("UC1234567890123456789012",page.items.single().channelId)
        assertEquals("page-two",page.next?.token)
    }
    @Test fun extractsEmbeddedJsonWithEscapedStrings() {
        val root=YouTubeParser.initial(html(fixture("search.json")))
        assertTrue(root.containsKey("contents")); assertEquals(YouTubeParser.parse(context),YouTubeParser.context(html("{}")))
        val strange="""<script>var ytInitialData = {"x":"a } \\\" b { c","y":{"z":1}};</script>"""
        assertEquals(1,YouTubeParser.initial(strange)["y"]!!.jsonObject["z"]!!.jsonPrimitive.int)
        assertFailsWith<CatalogException> {YouTubeParser.initial("consent required")}
    }
    @Test fun validatesDurationsAndLinks() {
        assertEquals(1200L,YouTubeParser.duration("20:00")); assertNull(YouTubeParser.duration("2:99")); assertNull(YouTubeParser.duration("LIVE"))
        assertEquals("aqz-KE-bpKQ",videoId("https://youtu.be/aqz-KE-bpKQ?t=3"))
        assertEquals("aqz-KE-bpKQ",videoId("https://www.youtube.com/watch?v=aqz-KE-bpKQ&x=1"))
        assertNull(videoId("https://evil.example/watch?v=aqz-KE-bpKQ")); assertNull(videoId("javascript:alert(1)"))
    }
    @Test fun filtersHistoryAndMaintainsBoundedLibrary() {
        val long=Video("aqz-KE-bpKQ","Long",durationSeconds=1200)
        val short=Video("jNQXAC9IVRw","Short",durationSeconds=19)
        val unknown=Video("M7lc1UVf-VE","Unknown")
        var lib=Library(); assertEquals(listOf(long),lib.visible(listOf(long,short,unknown)))
        lib=lib.watched(long).watched(long); assertEquals(1,lib.history.size)
        assertTrue(lib.copy(hideWatched=true).visible(listOf(long)).isEmpty())
        assertEquals(listOf(long),lib.favorite(long).favorites); assertTrue(lib.favorite(long).favorite(long).favorites.isEmpty())
        assertEquals(listOf("query"),lib.searched("query").searched("query").recentSearches)
        repeat(300) {lib=lib.watched(Video(it.toString(),"Test"))}; assertEquals(200,lib.history.size)
    }
    @Test fun roundTripsPersistentModel() {
        val lib=Library().watched(Video("aqz-KE-bpKQ","Vídeo")).searched("ciência").copy(minimumMinutes=40)
        assertEquals(lib,Json.decodeFromString<Library>(Json.encodeToString(lib)))
    }
    @Test fun mapsOfficialPlayerEvents() {
        assertEquals(PlaybackState.READY,playerState(5)); assertEquals(PlaybackState.PLAYING,playerState(1))
        assertEquals(PlaybackState.PAUSED,playerState(2)); assertEquals(PlaybackState.ENDED,playerState(0)); assertEquals(PlaybackState.LOADING,playerState(3))
    }
    @Test fun searchAndContinuationUseCurrentPublicContext() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(html(fixture("search.json"))))
            server.enqueue(MockResponse().setBody(fixture("continuation.json")))
            server.start()
            val catalog=YouTubeCatalog(base=server.url("/"))
            val first=catalog.search("ciência & história")
            assertEquals(2,catalog.next(first.next!!).items.size)
            val request1=server.takeRequest(); assertTrue(request1.path!!.contains("search_query=ci%C3%AAncia%20%26%20hist%C3%B3ria"))
            val request2=server.takeRequest(); assertEquals("/youtubei/v1/search",request2.path)
            val body=YouTubeParser.parse(request2.body.readUtf8()); assertEquals("page-two",body["continuation"]!!.jsonPrimitive.content)
            assertEquals(YouTubeParser.parse(context),body["context"]); assertNull(request2.getHeader("Authorization")); assertNull(request2.getHeader("Cookie"))
        }
    }
    @Test fun handlesRateLimitAndAuthorizationWithoutRetry() = runBlocking {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setResponseCode(429));server.enqueue(MockResponse().setResponseCode(403));server.enqueue(MockResponse().setResponseCode(302).setHeader("Location","https://consent.youtube.com"));server.start()
            val catalog=YouTubeCatalog(base=server.url("/"))
            assertEquals(CatalogException.Kind.RATE_LIMIT,assertFailsWith<CatalogException> {catalog.search("a")}.kind)
            assertEquals(CatalogException.Kind.BLOCKED,assertFailsWith<CatalogException> {catalog.search("a")}.kind)
            assertEquals(CatalogException.Kind.BLOCKED,assertFailsWith<CatalogException> {catalog.search("a")}.kind)
            assertEquals(3,server.requestCount)
        }
    }
    @Test fun cancelsNetworkRequest() = runBlocking {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));server.start()
            val client=OkHttpClient.Builder().callTimeout(10,TimeUnit.SECONDS).build()
            val catalog=YouTubeCatalog(client,server.url("/"))
            val job=launch {catalog.search("slow")}
            withContext(Dispatchers.IO) {assertNotNull(server.takeRequest(3,TimeUnit.SECONDS))}
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
        }
    }
    @Test fun readsVideoDetailsAndSelectedChannelTab() {
        val html="""<script>var ytInitialPlayerResponse = {"videoDetails":{"title":"Film","author":"Channel","channelId":"UC1234567890123456789012","lengthSeconds":"3600","shortDescription":"Description"}};</script>"""
        assertEquals(3600L,YouTubeParser.details(html,"aqz-KE-bpKQ").durationSeconds)
        val data=YouTubeParser.parse("""{"metadata":{"channelMetadataRenderer":{"title":"Channel","description":"About"}},"contents":{"tabs":[{"tabRenderer":{"selected":false,"content":{"videoRenderer":{"videoId":"jNQXAC9IVRw","title":{"simpleText":"Other tab"}}}}},{"tabRenderer":{"selected":true,"content":${fixture("search.json")}}}]}}""")
        val page=YouTubeParser.channel(data,"UC1234567890123456789012",YouTubeParser.parse(context))
        assertEquals("Channel",page.channel.title);assertEquals(1,page.videos.items.size)
    }
    @Test fun rejectsUnsupportedClients() = runBlocking {
        assertEquals(CatalogException.Kind.BLOCKED,assertFailsWith<CatalogException> {
            YouTubeCatalog().next(Cursor("x","browse","""{"client":{"clientName":"ANDROID"}}"""))
        }.kind)
    }
    @Test fun parsesModernChannelLockup() {
        val data=YouTubeParser.parse("""{"contents":{"lockupViewModel":{"contentId":"aqz-KE-bpKQ","contentType":"LOCKUP_CONTENT_TYPE_VIDEO","metadata":{"lockupMetadataViewModel":{"title":{"content":"Long film"},"metadata":{"contentMetadataViewModel":{"metadataRows":[{"metadataParts":[{"text":{"content":"Channel"}}]}]}}}},"contentImage":{"thumbnailViewModel":{"image":{"sources":[{"url":"https://i.ytimg.com/vi/aqz-KE-bpKQ/hqdefault.jpg"}]},"overlays":[{"thumbnailOverlayBadgeViewModel":{"thumbnailBadges":[{"thumbnailBadgeViewModel":{"text":"30:00"}}]}}]}}}}}""")
        val video=YouTubeParser.page(data,"browse",null).items.single()
        assertEquals("Long film",video.title); assertEquals(1800L,video.durationSeconds); assertEquals("Channel",video.channel)
    }
    @Test fun rejectsOversizedResponse() = runBlocking {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody("x".repeat(8*1024*1024+1)));server.start()
            val catalog=YouTubeCatalog(base=server.url("/"))
            assertEquals(CatalogException.Kind.SCHEMA,assertFailsWith<CatalogException> {catalog.search("test")}.kind)
        }
    }
    @Test fun refusesRepeatedContinuationToken() = runBlocking {
        MockWebServer().use {server ->
            server.enqueue(MockResponse().setBody(fixture("search.json")));server.start()
            val cursor=Cursor("page-two","search",context)
            assertEquals(CatalogException.Kind.SCHEMA,assertFailsWith<CatalogException> {YouTubeCatalog(base=server.url("/")).next(cursor)}.kind)
        }
    }

}
