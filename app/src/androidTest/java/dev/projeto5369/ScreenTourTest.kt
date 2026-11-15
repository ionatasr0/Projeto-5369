package dev.projeto5369

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Live tour (CI only): real catalog data on each screen, saved as screenshots for visual review. */
@RunWith(AndroidJUnit4::class)
class ScreenTourTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val args = InstrumentationRegistry.getArguments()
    private fun shot(name: String) {
        SystemClock.sleep(2_500)
        compose.waitForIdle()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        File(dir, "tour-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun tab(name: String) = compose.onNode(hasContentDescription(name) and hasAnyAncestor(hasTestTag("hotbar"))).performClick()
    private fun waitText(text: String, ms: Long = 30_000) = runCatching {
        compose.waitUntil(ms) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun tour() {
        assumeTrue(args.getString("ytLive") == "true")
        waitText("EM DESTAQUE"); shot("01-inicio")
        compose.onNodeWithTag("home-list").performTouchInput { swipeUp() }; shot("02-inicio-rolado")
        tab("Shorts"); runCatching { compose.waitUntil(30_000) { compose.onAllNodesWithTag("short-cover").fetchSemanticsNodes().isNotEmpty() } }; shot("03-shorts")
        tab("Pesquisa"); compose.onNodeWithTag("search-field").performTextInput("documentário espaço")
        compose.onNodeWithText("Pesquisar").performClick()
        runCatching { compose.waitUntil(30_000) { compose.onAllNodesWithTag("results-list").fetchSemanticsNodes().isNotEmpty() } }; SystemClock.sleep(4_000); shot("04-resultados")
        tab("Você"); shot("05-voce")
        tab("Seu Tube"); shot("06-seu-tube")
        tab("Você"); compose.onNodeWithContentDescription("Ajustes").performClick(); shot("07-ajustes")
        compose.onNodeWithText("Onda Glass").performClick()
        tab("Início"); shot("08-inicio-glass")
        tab("Shorts"); shot("09-shorts-glass")
        runCatching { tab("Você"); compose.onNodeWithContentDescription("Ajustes").performClick(); compose.onNodeWithText("Preto AMOLED").performClick() }
    }
}
