package dev.projeto5369.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons drawn from the OndaTube design (24×24). Paths marked filled are solid shapes. */
private fun icon(name: String, vararg paths: Pair<String, Boolean>): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    paths.forEach { (d, filled) ->
        if (filled) b.addPath(addPathNodes(d), fill = SolidColor(Color.Black))
        else b.addPath(addPathNodes(d), stroke = SolidColor(Color.Black), strokeLineWidth = 2.1f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }
    return b.build()
}

object OndaIcons {
    val Home = icon("home", "M12 3.5 L3.5 10.5 V20 H9.5 V14 H14.5 V20 H20.5 V10.5 Z" to false)
    val HomeFilled = icon("homeFilled", "M12 3.5 L3.5 10.5 V20 H9.5 V14 H14.5 V20 H20.5 V10.5 Z" to true)
    val Shorts = icon("shorts",
        "M10 2.5 H14 A4 4 0 0 1 18 6.5 V17.5 A4 4 0 0 1 14 21.5 H10 A4 4 0 0 1 6 17.5 V6.5 A4 4 0 0 1 10 2.5 Z" to false,
        "M10.5 9.5 V14.5 L14.5 12 Z" to true)
    val Search = icon("search", "M11 4 A7 7 0 1 1 11 18 A7 7 0 1 1 11 4 Z" to false, "M20 20 L16 16" to false)
    val User = icon("user", "M12 4 A4 4 0 1 1 12 12 A4 4 0 1 1 12 4 Z" to false,
        "M4.5 20.5 C5.5 16.5 8.5 14.5 12 14.5 C15.5 14.5 18.5 16.5 19.5 20.5" to false)
    val Back = icon("back", "M15 5 L8 12 L15 19" to false)
    val Close = icon("close", "M6 6 L18 18 M18 6 L6 18" to false)
    val Sliders = icon("sliders", "M4 7 H13 M19 7 H20 M4 17 H7 M13 17 H20" to false,
        "M16 4.5 A2.5 2.5 0 1 1 16 9.5 A2.5 2.5 0 1 1 16 4.5 Z M10 14.5 A2.5 2.5 0 1 1 10 19.5 A2.5 2.5 0 1 1 10 14.5 Z" to false)
    val Play = icon("play", "M7 4.5 V19.5 L19 12 Z" to true)
    val Heart = icon("heart", "M12 20 C12 20 3.5 14.5 3.5 8.8 A4.3 4.3 0 0 1 12 7 A4.3 4.3 0 0 1 20.5 8.8 C20.5 14.5 12 20 12 20 Z" to false)
    val HeartFilled = icon("heartFilled", "M12 20 C12 20 3.5 14.5 3.5 8.8 A4.3 4.3 0 0 1 12 7 A4.3 4.3 0 0 1 20.5 8.8 C20.5 14.5 12 20 12 20 Z" to true)
    val Share = icon("share", "M14 5 L21 12 L14 19 M21 12 H9 A6 6 0 0 0 3 18" to false)
    val External = icon("external", "M14 4 H20 V10 M20 4 L11 13" to false,
        "M18 14 V19 A1 1 0 0 1 17 20 H5 A1 1 0 0 1 4 19 V7 A1 1 0 0 1 5 6 H10" to false)
    val Mic = icon("mic", "M12 3 A3 3 0 0 1 15 6 V11 A3 3 0 0 1 9 11 V6 A3 3 0 0 1 12 3 Z" to false,
        "M5.5 11 A6.5 6.5 0 0 0 18.5 11 M12 17.5 V21" to false)
    val Clock = icon("clock", "M12 4 A8 8 0 1 1 12 20 A8 8 0 1 1 12 4 Z" to false, "M12 8 V12 L15 14" to false)
    val Plus = icon("plus", "M12 5 V19 M5 12 H19" to false)
    val Check = icon("check", "M5 12.5 L10 17.5 L19 7" to false)
    val Chevron = icon("chevron", "M6 9 L12 15 L18 9" to false)
    val Link = icon("link", "M10 14 L14 10 M8.5 11 L6.5 13 A3.5 3.5 0 0 0 11.5 18 L13.5 16 M15.5 13 L17.5 11 A3.5 3.5 0 0 0 12.5 6 L10.5 8" to false)
    val Logo = icon("logo", "M8 5.5 V18.5 L18.5 12 Z" to true, "M3 20.5 C5 18.9 6.5 18.9 8.5 20.5 S12 22.1 14 20.5" to false)
}
