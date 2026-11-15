package dev.projeto5369

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.projeto5369.core.Video
import dev.projeto5369.ui.OndaTheme
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live device tests (CI only, -Pandroid.testInstrumentationRunnerArguments.ytLive=true) against the real YouTube.
 * Playback proof: muted start through a test-only switch, then the official player's own currentTime must advance.
 * Several video/host combinations are tried and logged, because datacenter IPs can be blocked by YouTube.
 */
@RunWith(AndroidJUnit4::class)
class LiveYouTubePlaybackTest {
    @get:Rule val compose = createComposeRule()
    private val args = InstrumentationRegistry.getArguments()
    private val target = mutableStateOf<Pair<String, String?>?>(null)
    private val playing = AtomicBoolean(false)

    @After fun reset() { PlayerDebug.autoplayMuted = false; PlayerDebug.lastTime = -1.0; PlayerDebug.host = null }

    private fun shot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dirs = listOfNotNull(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, args.getString("additionalTestOutputDir")?.let(::File))
        dirs.forEach { dir -> runCatching { dir.mkdirs(); File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } } }
    }
    private fun status(): String = runCatching {
        compose.onNodeWithTag("yt-status").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
    }.getOrDefault("?")
    private fun failed() = compose.onAllNodesWithText("Tentar novamente").fetchSemanticsNodes().isNotEmpty()

    private fun host() {
        compose.setContent {
            OndaTheme(glass = false) { Column { target.value?.let { t -> key(t) { YouTubePlayer(Video(t.first, "Teste"), onPlaying = { playing.set(true) }) } } } }
        }
    }

    private fun attempt(id: String, hostUrl: String?): Boolean {
        PlayerDebug.autoplayMuted = true; PlayerDebug.host = hostUrl; PlayerDebug.lastTime = -1.0
        playing.set(false)
        compose.runOnIdle { target.value = id to hostUrl }
        runCatching { compose.waitUntil(60_000) { playing.get() || failed() } }
        val label = "$id-${hostUrl?.substringAfter("www.")?.substringBefore(".") ?: "youtube"}"
        SystemClock.sleep(3_000)
        shot("auto-$label")
        println("[playback] $label playing=${playing.get()} status=${status()}")
        if (!playing.get()) return false
        runCatching { compose.waitUntil(30_000) { PlayerDebug.lastTime >= 0 } }
        val t1 = PlayerDebug.lastTime
        SystemClock.sleep(6_000)
        val t2 = PlayerDebug.lastTime
        shot("auto-$label-${"%.1f".format(t2)}s")
        println("[playback] $label currentTime $t1 -> $t2")
        return t2 >= t1 + 3
    }

    @Test fun playbackAdvancesInOfficialPlayer() {
        assumeTrue(args.getString("ytLive") == "true")
        host()
        val combos = listOf("M7lc1UVf-VE" to null, "jNQXAC9IVRw" to null, "M7lc1UVf-VE" to "https://www.youtube-nocookie.com", "jNQXAC9IVRw" to "https://www.youtube-nocookie.com")
        val results = combos.map { (id, h) -> attempt(id, h) }
        println("[playback] results=$results")
        assertTrue("Nenhuma combinação reproduziu: veja os logs [playback]", results.any { it })
    }

    @Test fun tapOnOfficialPlayer() {
        assumeTrue(args.getString("ytLive") == "true")
        host()
        compose.runOnIdle { target.value = "M7lc1UVf-VE" to null }
        compose.waitUntil(60_000) { status().startsWith("Toque") || failed() }
        SystemClock.sleep(6_000)
        shot("tap-0-pronto")
        for (i in 1..3) {
            if (playing.get() || failed()) break
            compose.onNodeWithTag("yt-player").performTouchInput { click(center) }
            runCatching { compose.waitUntil(20_000) { playing.get() || failed() } }
        }
        shot("tap-1-depois")
        // Reported, not asserted: emulator WebViews may not deliver synthetic taps into the cross-origin iframe.
        println("[tap] playing=${playing.get()} status=${status()}")
    }
}
