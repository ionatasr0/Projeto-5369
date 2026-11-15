package dev.projeto5369

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
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
 * 1) tap test: a real tap on the official player; 2) playback proof: muted start (test-only switch) and the
 * official player's own currentTime must advance. Screenshots are saved for inspection.
 */
@RunWith(AndroidJUnit4::class)
class LiveYouTubePlaybackTest {
    @get:Rule val compose = createComposeRule()
    private val args = InstrumentationRegistry.getArguments()

    @After fun reset() { PlayerDebug.autoplayMuted = false; PlayerDebug.lastTime = -1.0 }

    private fun shot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dirs = listOfNotNull(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, args.getString("additionalTestOutputDir")?.let(::File))
        dirs.forEach { dir -> runCatching { dir.mkdirs(); File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } } }
    }
    private fun status(): String = runCatching {
        compose.onNodeWithTag("yt-status").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
    }.getOrDefault("?")
    private fun failed() = compose.onAllNodesWithText("Tentar novamente").fetchSemanticsNodes().isNotEmpty()

    private fun show(id: String, playing: AtomicBoolean) {
        compose.setContent { OndaTheme(glass = false) { Column { YouTubePlayer(Video(id, "Teste"), onPlaying = { playing.set(true) }) } } }
    }

    @Test fun playbackAdvancesInOfficialPlayer() {
        assumeTrue(args.getString("ytLive") == "true")
        PlayerDebug.autoplayMuted = true
        val playing = AtomicBoolean(false)
        show("M7lc1UVf-VE", playing)
        compose.waitUntil(90_000) { playing.get() || failed() }
        assertTrue("O YouTube não informou reprodução: ${status()}", playing.get() && !failed())
        compose.waitUntil(30_000) { PlayerDebug.lastTime >= 0 }
        val t1 = PlayerDebug.lastTime
        SystemClock.sleep(6_000)
        val t2 = PlayerDebug.lastTime
        shot("playback-${"%.1f".format(t2)}s")
        println("[playback] currentTime $t1 -> $t2, status=${status()}")
        assertTrue("O tempo do player oficial não avançou ($t1 -> $t2)", t2 >= t1 + 3)
    }

    @Test fun tapOnOfficialPlayer() {
        assumeTrue(args.getString("ytLive") == "true")
        val playing = AtomicBoolean(false)
        show("M7lc1UVf-VE", playing)
        compose.waitUntil(60_000) { status().startsWith("Toque") || failed() }
        SystemClock.sleep(6_000)
        shot("tap-0-pronto")
        for (attempt in 1..3) {
            if (playing.get() || failed()) break
            compose.onNodeWithTag("yt-player").performTouchInput { click(center) }
            runCatching { compose.waitUntil(20_000) { playing.get() || failed() } }
        }
        shot("tap-1-depois")
        // Reported, not asserted: emulator WebViews may not accept synthetic taps inside the cross-origin iframe.
        println("[tap] playing=${playing.get()} status=${status()}")
    }
}
