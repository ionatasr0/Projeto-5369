package dev.projeto5369

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap=instrumentation.uiAutomation.takeScreenshot() ?: return
        java.io.File(instrumentation.targetContext.filesDir,name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    private fun tab(name: String) = compose.onNode(hasContentDescription(name) and hasAnyAncestor(hasTestTag("hotbar"))).performClick()

    @Test fun searchLinkValidationAndLocalSettings() {
        tab("Pesquisa")
        compose.onNodeWithTag("search-list").performScrollToNode(hasTestTag("link-field"))
        compose.onNodeWithTag("link-field").performTextInput("invalid")
        compose.onNodeWithText("Abrir vídeo").performClick()
        compose.onNodeWithText("Informe um link do YouTube ou ID válido.").assertIsDisplayed()
        tab("Você")
        compose.onNodeWithContentDescription("Ajustes").performClick()
        compose.onNodeWithText("40 min").performClick()
        compose.onNodeWithText("Onda Glass").performClick()
        compose.onNodeWithText("Preto AMOLED").performClick()
        compose.onNodeWithText("Duração mínima").assertIsDisplayed()
        tab("Início")
        compose.onNodeWithText("Início").assertIsDisplayed()
        screenshot("ui-home.png")
    }

    @Test fun opensVideoDetailsWithoutCatalogAndSavesFavorite() {
        tab("Pesquisa")
        compose.onNodeWithTag("search-list").performScrollToNode(hasTestTag("link-field"))
        compose.onNodeWithTag("link-field").performTextInput("https://youtu.be/aqz-KE-bpKQ")
        compose.onNodeWithText("Abrir vídeo").performClick()
        compose.onNodeWithTag("yt-player").assertExists()
        compose.onNodeWithTag("watch-list").performScrollToNode(hasText("Favoritar no aparelho"))
        compose.onNodeWithText("Favoritar no aparelho").performClick()
        compose.waitUntil(timeoutMillis=5_000) { compose.onAllNodesWithText("Remover dos favoritos").fetchSemanticsNodes().isNotEmpty() }
        screenshot("ui-watch.png")
        compose.onNodeWithContentDescription("Voltar").performClick()
        tab("Você")
        compose.onNodeWithTag("library-list").performScrollToNode(hasText("Favoritos locais"))
        compose.onNodeWithText("Favoritos locais").assertExists()
    }

    @Test fun shortsTabOpens() {
        tab("Shorts")
        compose.onNodeWithTag("shorts").assertExists()
        compose.onNodeWithText("Para você").assertExists()
    }
}
