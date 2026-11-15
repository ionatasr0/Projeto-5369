package dev.projeto5369.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.projeto5369.core.Video

fun durationText(seconds: Long?): String? = seconds?.let {
    val h = it / 3600; val m = (it % 3600) / 60; val s = it % 60
    if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Stable placeholder tint per video, so cards never flash white before the thumbnail arrives. */
fun tintFor(id: String): Color {
    val palette = listOf(0xFF3B4A5A, 0xFF4A3626, 0xFF2E4A1E, 0xFF1E2F5A, 0xFF4A1E3A, 0xFF5A4A1E)
    return Color(palette[(id.hashCode() and 0x7fffffff) % palette.size])
}

@Composable
fun OndaSurface(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(24.dp), strong: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val c = LocalOnda.current
    Box(modifier.clip(shape).background(if (strong) c.surfaceStrong else c.surface, shape).border(1.dp, c.border, shape), content = content)
}

@Composable
fun CircleIcon(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Boolean = false, size: Dp = 44.dp) {
    val c = LocalOnda.current
    Box(modifier.size(size).clip(CircleShape)
        .background(if (accent) c.accent else c.surface, CircleShape)
        .border(1.dp, if (accent) Color.Transparent else c.border, CircleShape)
        .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (accent) c.onAccent else c.text, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Pill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, primary: Boolean = false, selected: Boolean = false, enabled: Boolean = true) {
    val c = LocalOnda.current
    val bg = when { primary -> c.accent; selected -> Color.White; else -> c.surface }
    val fg = when { primary -> c.onAccent; selected -> Color.Black; else -> c.text }
    Row(modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(bg)
        .border(1.dp, if (primary || selected) Color.Transparent else c.border, RoundedCornerShape(50))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { this.selected = selected }
        .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Text(text, style = OndaType.button, color = if (enabled) fg else fg.copy(alpha = .4f), maxLines = 1)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 26.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = OndaType.title, color = LocalOnda.current.text, modifier = Modifier.weight(1f))
        if (action != null) Text(action, style = OndaType.button.copy(color = LocalOnda.current.accent),
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 8.dp, vertical = 10.dp))
    }
}

@Composable
fun Thumb(video: Video, modifier: Modifier, shape: Shape = RoundedCornerShape(20.dp), progress: Float? = null) {
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(tintFor(video.id), Color(0xFF0B0B0C))))) {
        AsyncImage(model = video.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        durationText(video.durationSeconds)?.let {
            Text(it, style = OndaType.small.copy(color = Color.White), modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .65f)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
        if (progress != null) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = .25f)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress).height(4.dp).background(Lime))
        }
    }
}

@Composable
fun FeaturedVideo(video: Video, badge: String, onClick: () -> Unit) {
    val c = LocalOnda.current
    Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
        .then(if (c.glass) Modifier.background(c.surface).border(1.dp, c.border, RoundedCornerShape(28.dp)).padding(8.dp) else Modifier)
        .clickable(onClickLabel = "Abrir ${video.title}", onClick = onClick)) {
        Box {
            Thumb(video, Modifier.fillMaxWidth().aspectRatio(16f / 9f), RoundedCornerShape(24.dp))
            Text(badge, style = OndaType.label.copy(color = Lime), modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = .55f)).padding(horizontal = 10.dp, vertical = 6.dp))
        }
        VideoMeta(video, Modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 6.dp), big = true)
    }
}

@Composable
fun VideoMeta(video: Video, modifier: Modifier = Modifier, big: Boolean = false) {
    val c = LocalOnda.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (big) Box(Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(tintFor(video.channel), Color(0xFF2A3138)))),
            contentAlignment = Alignment.Center) { Text(video.channel.take(1).uppercase(), style = OndaType.button, color = Color.White) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(video.title, style = if (big) OndaType.cardTitle.copy(fontSize = 17.sp) else OndaType.cardTitle, color = c.text, maxLines = if (big) 3 else 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(video.channel.takeIf { it.isNotBlank() }, durationText(video.durationSeconds)).joinToString(" · ").ifBlank { "YouTube" },
                style = OndaType.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun VideoRowCard(video: Video, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable(onClickLabel = "Abrir ${video.title}", onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Thumb(video, Modifier.width(150.dp).aspectRatio(16f / 9f), RoundedCornerShape(16.dp))
        VideoMeta(video, Modifier.weight(1f).padding(top = 2.dp))
    }
}

@Composable
fun VideoTile(video: Video, onClick: () -> Unit, width: Dp = 200.dp, progress: Float? = null) {
    val c = LocalOnda.current
    Column(Modifier.width(width).clip(RoundedCornerShape(22.dp))
        .then(if (c.glass) Modifier.background(c.surface).border(1.dp, c.border, RoundedCornerShape(22.dp)).padding(8.dp) else Modifier)
        .clickable(onClickLabel = "Abrir ${video.title}", onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Thumb(video, Modifier.fillMaxWidth().aspectRatio(16f / 9f), RoundedCornerShape(16.dp), progress)
        Text(video.title, style = OndaType.cardTitle.copy(fontSize = 14.sp), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(video.channel.ifBlank { "YouTube" }, style = OndaType.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Every state message (empty, offline, 429, consent, embed blocked...) uses the same visible card with explicit actions. */
@Composable
fun StateCard(text: String, error: Boolean, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val c = LocalOnda.current
    OndaSurface(modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = OndaType.body, color = if (error) Danger else c.text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

@Composable
fun LoadingRow(text: String = "Carregando…") {
    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(24.dp), color = Lime, strokeWidth = 3.dp)
        Text(text, style = OndaType.body, color = LocalOnda.current.muted)
    }
}

@Composable
fun ColorCard(kind: String, title: String, meta: String, colors: List<Color>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalOnda.current
    val brush = if (c.glass) Brush.linearGradient(colors.map { it.copy(alpha = .38f) }) else Brush.linearGradient(colors)
    Column(modifier.width(150.dp).height(160.dp).clip(RoundedCornerShape(24.dp)).background(brush)
        .then(if (c.glass) Modifier.border(1.dp, c.border, RoundedCornerShape(24.dp)) else Modifier)
        .clickable(role = Role.Button, onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text(kind, style = OndaType.label, color = Color.White.copy(alpha = .85f))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = OndaType.title, color = Color.White)
            Text(meta, style = OndaType.small, color = Color.White.copy(alpha = .85f))
        }
    }
}

/** The Onda folder shape: rounded card with a tab, used for the local library. */
@Composable
fun FolderCard(name: String, count: String, sub: String, color: Color, ink: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalOnda.current
    val fill = if (c.glass) color.copy(alpha = .4f) else color
    val text = if (c.glass) Color.White else ink
    Box(modifier.height(122.dp).semantics(mergeDescendants = true) {}.clickable(role = Role.Button, onClickLabel = name, onClick = onClick)) {
        Box(Modifier.padding(start = 0.dp).width(58.dp).height(14.dp).clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(fill))
        Column(Modifier.padding(top = 10.dp).fillMaxSize().clip(RoundedCornerShape(4.dp, 22.dp, 22.dp, 22.dp)).background(fill)
            .then(if (c.glass) Modifier.border(1.dp, c.border, RoundedCornerShape(4.dp, 22.dp, 22.dp, 22.dp)) else Modifier)
            .padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(count, style = OndaType.display.copy(fontSize = 30.sp), color = text)
            Column { Text(name, style = OndaType.cardTitle, color = text); Text(sub, style = OndaType.small, color = text.copy(alpha = .75f)) }
        }
    }
}

enum class Tab(val route: String, val label: String) { STATS("stats", "Seu Tube"), HOME("home", "Início"), SHORTS("shorts", "Shorts"), SEARCH("search", "Pesquisa"), YOU("library", "Você") }

@Composable
fun Hotbar(current: Tab?, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalOnda.current
    Row(modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth().height(66.dp)
        .clip(RoundedCornerShape(33.dp)).background(if (c.glass) Color(0xFF0E2016).copy(alpha = .93f) else Color(0xFF111113).copy(alpha = .96f))
        .border(1.dp, c.border, RoundedCornerShape(33.dp)).testTag("hotbar"),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        Tab.entries.forEach { tab ->
            val selected = current == tab
            val icon = when (tab) { Tab.STATS -> OndaIcons.Logo; Tab.HOME -> if (selected) OndaIcons.HomeFilled else OndaIcons.Home; Tab.SHORTS -> OndaIcons.Shorts; Tab.SEARCH -> OndaIcons.Search; Tab.YOU -> OndaIcons.User }
            Box(Modifier.size(48.dp).clip(CircleShape)
                .background(when { tab == Tab.STATS -> c.accent; selected -> Color.White.copy(alpha = .18f); else -> Color.Transparent })
                .clickable(role = Role.Tab, onClick = { onSelect(tab) })
                .semantics { contentDescription = tab.label; this.selected = selected; role = Role.Tab }, contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = when { tab == Tab.STATS -> c.onAccent; selected -> Color.White; else -> c.muted }, modifier = Modifier.size(23.dp))
            }
        }
    }
}

@Composable
fun ScreenTitle(kicker: String, title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val c = LocalOnda.current
    Row(modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(kicker, style = OndaType.label, color = c.muted)
            Text(title, style = OndaType.display, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** The Onda signature: a lime wave drifting slowly (one cheap Canvas, no bitmaps). */
@Composable
fun WaveLine(modifier: Modifier = Modifier) {
    val shift by androidx.compose.animation.core.rememberInfiniteTransition(label = "wave").animateFloat(
        0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(14_000, easing = androidx.compose.animation.core.LinearEasing)), label = "shift")
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(36.dp).padding(horizontal = 20.dp)) {
        val period = 260.dp.toPx(); val amp = 9.dp.toPx(); val mid = size.height / 2
        val path = androidx.compose.ui.graphics.Path()
        var x = -period * shift
        path.moveTo(x, mid)
        while (x < size.width + period) {
            path.cubicTo(x + period * .17f, mid - amp * 2, x + period * .33f, mid - amp * 2, x + period * .5f, mid)
            path.cubicTo(x + period * .67f, mid + amp * 2, x + period * .83f, mid + amp * 2, x + period, mid)
            x += period
        }
        clipRect { drawPath(path, Lime, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)) }
    }
}
