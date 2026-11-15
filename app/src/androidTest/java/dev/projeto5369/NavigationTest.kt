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
    @Test fun searchLinkValidationAndLocalSettings() {
        compose.onNodeWithText("Pesquisa",useUnmergedTree=true).performClick()
        compose.onNodeWithText("URL do YouTube ou ID").performTextInput("invalid")
        compose.onNodeWithText("Abrir vídeo",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Informe um link do YouTube ou ID válido.").assertIsDisplayed()
        compose.onNodeWithText("Ajustes",useUnmergedTree=true).performClick()
        compose.onNodeWithText("40 min").performClick()
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Favoritos locais").assertIsDisplayed()
    }
    @Test fun opensVideoDetailsWithoutCatalogAndSavesFavorite() {
        compose.onNodeWithText("Pesquisa",useUnmergedTree=true).performClick()
        compose.onNodeWithText("URL do YouTube ou ID").performTextInput("https://youtu.be/aqz-KE-bpKQ")
        compose.onNodeWithText("Abrir vídeo",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Reproduzir",useUnmergedTree=true).assertIsDisplayed()
        compose.onNodeWithText("Favoritar no aparelho").performScrollTo().performClick()
        compose.onNodeWithText("Biblioteca",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Favoritos locais").assertIsDisplayed()
    }
}
