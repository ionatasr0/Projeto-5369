package dev.projeto5369.core

import kotlin.test.*

class DiscoveryTest {
    private fun video(id: String, channel: String) = Video(id,id,channel=channel)
    @Test fun homeEditionsChangeThePublicQueries() {
        assertNotEquals(discoveryQueries("unused",true,0),discoveryQueries("unused",true,1))
        assertEquals(3,discoveryQueries("unused",true,Int.MAX_VALUE).distinct().size)
    }
    @Test fun individualThemesAlsoVaryAcrossRefreshes() {
        assertNotEquals(discoveryQueries("viagem de ônibus",false,0),discoveryQueries("viagem de ônibus",false,1))
    }
    @Test fun mergeRemovesDuplicatesAndInterleavesSources() {
        val a=video("a","A"); val b=video("b","B"); val c=video("c","C")
        val feed=discoveryVideos(listOf(listOf(a,b),listOf(c,a)),emptySet(),emptySet())
        assertEquals(listOf("a","c","b"),feed.map { it.id })
        assertEquals(feed.size,feed.map { it.id }.distinct().size)
    }
    @Test fun previouslyShownAndWatchedVideosComeAfterNewVideos() {
        val feed=discoveryVideos(listOf(listOf(video("old","A"),video("new","B"),video("seen","C"))),setOf("old"),setOf("seen"))
        assertEquals("new",feed.first().id)
    }
    @Test fun emptySourcesAndMissingChannelMetadataRemainUsable() {
        assertTrue(discoveryVideos(emptyList(),emptySet(),emptySet()).isEmpty())
        assertEquals(2,discoveryVideos(listOf(listOf(video("a",""),video("b",""))),emptySet(),emptySet()).size)
    }
}
