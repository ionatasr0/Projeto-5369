package dev.projeto5369

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.projeto5369.core.Video
import dev.projeto5369.ui.OndaTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

/** Real native playback only: PLAYING plus advancing native Media3 position. No IFrame simulation. */
@RunWith(AndroidJUnit4::class)
class LiveYouTubePlaybackTest {
    @get:Rule val compose=createComposeRule()
    @After fun reset() { PlayerDebug.autoplayMuted=false; PlayerDebug.lastTime=-1.0; PlayerDebug.firstFrame=false }
    @Test fun playbackAdvancesInNativePlayer() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("ytLive") == "true")
        PlayerDebug.autoplayMuted=true; PlayerDebug.firstFrame=false
        val selected=mutableStateOf("jNQXAC9IVRw")
        val playing=AtomicBoolean(false)
        compose.setContent { OndaTheme(glass=false) { Column { YouTubePlayer(Video(selected.value,"Vídeo público"),onPlaying={playing.set(true)}) } } }
        compose.waitUntil(55_000) { (playing.get() && PlayerDebug.firstFrame) || compose.onAllNodesWithText("Tentar novamente").fetchSemanticsNodes().isNotEmpty() }
        assertTrue("Native player did not enter PLAYING; inspect sanitized source/category logs",playing.get())
        assertTrue("Native video did not render a decoded first frame",PlayerDebug.firstFrame)
        val start=PlayerDebug.lastTime
        compose.waitUntil(15_000) { PlayerDebug.lastTime >= start+3 }
        assertTrue(PlayerDebug.lastTime >= start+3)
    }
}
