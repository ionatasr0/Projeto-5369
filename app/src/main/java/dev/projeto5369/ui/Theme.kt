package dev.projeto5369.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.projeto5369.R

/** OndaTube palette: AMOLED black or Onda Glass, lime accent shared with the Onda ecosystem. */
@Immutable
data class OndaColors(
    val glass: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val border: Color,
    val text: Color = Color.White,
    val muted: Color,
    val accent: Color = Lime,
    val onAccent: Color = Color(0xFF0B1A06),
)
val Lime = Color(0xFFC5F06A)
val Danger = Color(0xFFFF8A8A)

val BlackColors = OndaColors(
    glass = false, background = Color.Black, surface = Color(0xFF161618), surfaceStrong = Color(0xFF1C1C1F),
    border = Color(0xFF26262A), muted = Color(0xFFA1A1AA),
)
val GlassColors = OndaColors(
    glass = true, background = Color(0xFF05110B), surface = Color.White.copy(alpha = .08f), surfaceStrong = Color.White.copy(alpha = .12f),
    border = Color.White.copy(alpha = .18f), muted = Color(0xFFC9D8C2),
)
val LocalOnda = staticCompositionLocalOf { BlackColors }

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_regular, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_semibold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
)
object OndaType {
    val display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 34.sp, letterSpacing = (-1.1).sp)
    val title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-.4).sp)
    val cardTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 19.sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.8.sp)
    val button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp)
}

@Composable
fun OndaTheme(glass: Boolean, content: @Composable () -> Unit) {
    val colors = if (glass) GlassColors else BlackColors
    val scheme = darkColorScheme(primary = Lime, onPrimary = colors.onAccent, background = colors.background, surface = colors.background, error = Danger)
    val base = Typography()
    val typography = Typography(
        headlineSmall = OndaType.title, titleLarge = OndaType.title, titleMedium = OndaType.cardTitle, titleSmall = OndaType.cardTitle,
        bodyLarge = OndaType.body.copy(fontSize = 16.sp), bodyMedium = OndaType.body, bodySmall = OndaType.small,
        labelLarge = OndaType.button, labelMedium = base.labelMedium.copy(fontFamily = Inter), labelSmall = base.labelSmall.copy(fontFamily = Inter),
    )
    CompositionLocalProvider(LocalOnda provides colors) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/** App background. Glass draws soft light blobs with gradients only (no runtime blur: cheap on battery). */
@Composable
fun OndaBackground(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable BoxScope.() -> Unit) {
    val c = LocalOnda.current
    Box(modifier.fillMaxSize().background(c.background).drawBehind {
        if (c.glass || tint != null) {
            val w = size.width; val h = size.height
            fun blob(color: Color, x: Float, y: Float, r: Float) =
                drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(x, y), radius = r), radius = r, center = Offset(x, y))
            if (tint != null) {
                blob(tint.copy(alpha = .55f), w * .3f, h * .12f, w * .95f)
                blob(tint.copy(alpha = .25f), w * .9f, h * .55f, w * .8f)
            }
            if (c.glass) {
                blob(Color(0xFF2F7A3A).copy(alpha = .75f), w * .05f, h * .08f, w * .95f)
                blob(Lime.copy(alpha = .22f), w * 1.0f, h * .5f, w * .9f)
                blob(Color(0xFF1F5A6A).copy(alpha = .6f), w * .1f, h * .92f, w * .9f)
            }
        }
    }, content = content)
}
