package dev.projeto5369

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.projeto5369.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.Assert.*

class LocalStoreTest {
    @get:Rule val tmp=TemporaryFolder()
    @Test fun persistsAcrossStoreRecreation() = runTest {
        val file=tmp.newFile("library.preferences_pb"); file.delete()
        val scope1=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val first=LocalStore(PreferenceDataStoreFactory.create(scope=scope1,produceFile={file}))
        val video=Video("aqz-KE-bpKQ","Vídeo longo",durationSeconds=3600)
        first.update {it.watched(video).favorite(video).searched("ciência").copy(minimumMinutes=40,hideWatched=true)}
        val expected=first.library.first(); scope1.coroutineContext.job.cancelAndJoin()
        val scope2=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        try {
            val second=LocalStore(PreferenceDataStoreFactory.create(scope=scope2,produceFile={file}))
            assertEquals(expected,second.library.first())
            second.update {it.copy(history=emptyList(),recentSearches=emptyList())}
            assertEquals(listOf(video),second.library.first().favorites)
            assertTrue(second.library.first().history.isEmpty())
        } finally {scope2.coroutineContext.job.cancelAndJoin()}
    }
    @Test fun embedUsesValidatedIdAndOfficialApi() {
        val html=embeddedHtml("aqz-KE-bpKQ")
        assertTrue(html.contains("https://www.youtube.com/iframe_api"))
        assertTrue(html.contains("origin:'https://dev.projeto5369'"))
        assertTrue(html.contains("onError")); assertFalse(html.contains("autoplay:1"))
        assertThrows(IllegalArgumentException::class.java) {embeddedHtml("x');alert(1);")}
    }
}
