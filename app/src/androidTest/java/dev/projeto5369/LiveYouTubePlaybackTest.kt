package dev.projeto5369

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.projeto5369.core.Video
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live device test (CI only, -Pandroid.testInstrumentationRunnerArguments.ytLive=true):
 * the official IFrame player loads, a real tap starts it, and YouTube reports state PLAYING.
 * Screenshots are saved so the result can be inspected, not just asserted.
 */
@RunWith(AndroidJUnit4::class)
class LiveYouTubePlaybackTest {
    @get:Rule val compose = createComposeRule()
    private val args = InstrumentationRegistry.getArguments()

    private fun shot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dirs = listOfNotNull(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            args.getString("additionalTestOutputDir")?.let(::File),
        )
        dirs.forEach { dir -> runCatching { dir.mkdirs(); File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } } }
    }
    private fun status(): String = runCatching {
        compose.onNodeWithTag("yt-status").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
    }.getOrDefault("?")
    private fun failed() = compose.onAllNodesWithText("Tentar novamente").fetchSemanticsNodes().isNotEmpty()

    private fun playAndCheck(id: String, label: String) {
        val playing = AtomicBoolean(false)
        compose.setContent { MaterialTheme { Column { YouTubePlayer(Video(id, label), onPlaying = { playing.set(true) }) } } }
        compose.waitUntil(60_000) { status().startsWith("Toque") || failed() }
        SystemClock.sleep(6_000)
        shot("$id-1-pronto")
        assertTrue("Player não ficou pronto: ${status()}", !failed())
        // A real tap on the official big play button (center), retried once in case the first tap only shows controls.
        for (attempt in 1..3) {
            if (playing.get() || failed()) break
            compose.onNodeWithTag("yt-player").performTouchInput { click(center) }
            runCatching { compose.waitUntil(20_000) { playing.get() || failed() } }
            if (!playing.get()) shot("$id-tap$attempt")
        }
        SystemClock.sleep(8_000)
        shot("$id-2-tocando")
        println("[playback] $id -> ${status()}")
        assertTrue("O YouTube não informou reprodução: ${status()}", playing.get() && !failed())
    }

    @Test fun officialPlayerPlaysEmbeddableVideo() {
        assumeTrue(args.getString("ytLive") == "true")
        playAndCheck("M7lc1UVf-VE", "YouTube Developers")
    }
}
