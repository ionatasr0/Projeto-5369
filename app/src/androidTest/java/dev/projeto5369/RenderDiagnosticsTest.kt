package dev.projeto5369

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.projeto5369.core.Video
import dev.projeto5369.ui.OndaTheme
import java.io.File
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Diagnostics (CI only): why does the official player area stay black? Each variant is screenshotted and logged. */
@RunWith(AndroidJUnit4::class)
class RenderDiagnosticsTest {
    @get:Rule val compose = createComposeRule()
    private val args = InstrumentationRegistry.getArguments()
    private val variant = mutableStateOf<String?>(null)

    @After fun reset() { PlayerDebug.mode = "iframeapi"; PlayerDebug.software = false; PlayerDebug.probe = false }

    private fun shot(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "render-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun variants() {
        assumeTrue(args.getString("ytLive") == "true")
        compose.setContent { OndaTheme(glass = false) { Column { variant.value?.let { v -> key(v) { YouTubePlayer(Video("M7lc1UVf-VE", "Teste"), onPlaying = {}) } } } } }
        for (v in listOf("probe", "iframeapi", "software", "embedurl")) {
            PlayerDebug.probe = v == "probe"; PlayerDebug.software = v == "software"; PlayerDebug.mode = if (v == "embedurl") "embedurl" else "iframeapi"
            PlayerDebug.lastLog = ""
            compose.runOnIdle { variant.value = v }
            SystemClock.sleep(15_000)
            compose.waitForIdle()
            shot(v)
            println("[render] $v -> ${PlayerDebug.lastLog}")
        }
    }
}
