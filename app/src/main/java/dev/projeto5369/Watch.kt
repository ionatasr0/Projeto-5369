package dev.projeto5369

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.projeto5369.core.*
import dev.projeto5369.ui.*

/** Watch screen: official player on top, Onda chrome around it (never over it), ambient blur from the thumbnail. */
@Composable
fun WatchScreen(video: Video, detailError: String?, library: Library, upNext: List<Video>, onPlaying: () -> Unit, onFavorite: () -> Unit,
                onFollow: (Channel) -> Unit, onChannel: (String) -> Unit, open: (Video) -> Unit, onBack: () -> Unit) {
    val c = LocalOnda.current
    val context = LocalContext.current
    val favorite = library.favorites.any { it.id == video.id }
    val channelId = video.channelId?.takeIf { Regex("UC[A-Za-z0-9_-]{22}").matches(it) }
    val following = channelId != null && library.channels.any { it.id == channelId }
    var expanded by remember(video.id) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        // Same elegant cover-blur idea as the Onda player (blur needs Android 12+; older devices get a soft tint).
        if (Build.VERSION.SDK_INT >= 31) AsyncImage(video.thumbnail, null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(460.dp).blur(60.dp).alpha(if (c.glass) .55f else .4f))
        else Box(Modifier.fillMaxWidth().height(460.dp).background(Brush.verticalGradient(listOf(tintFor(video.id).copy(alpha = .6f), Color.Transparent))))
        Box(Modifier.fillMaxWidth().height(460.dp).background(Brush.verticalGradient(listOf(Color.Transparent, c.background))))
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                CircleIcon(OndaIcons.Chevron,"Voltar",onBack)
                Text("OndaTube",style=OndaType.cardTitle,color=c.text,modifier=Modifier.weight(1f).padding(horizontal=14.dp))
                CircleIcon(OndaIcons.Share,"Compartilhar",{ share(context,video) })
            }
            YouTubePlayer(video,onPlaying=onPlaying,showOpenExternal=true,modifier=Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("watch-list"),contentPadding=PaddingValues(bottom=32.dp)) {
            item("info") {
                OndaSurface(Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp).fillMaxWidth(), RoundedCornerShape(28.dp), strong = c.glass) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(video.title, style = OndaType.title.copy(fontSize = 21.sp), color = c.text)
                        durationText(video.durationSeconds)?.let { Text("Duração $it", style = OndaType.small, color = c.muted) }
                        if (video.channel.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(tintFor(video.channel), Color.Black)))
                                .then(if (channelId != null) Modifier.clickable(role = Role.Button, onClickLabel = "Ver canal") { onChannel(channelId) } else Modifier),
                                contentAlignment = Alignment.Center) { Text(video.channel.take(1).uppercase(), style = OndaType.button, color = Color.White) }
                            Column(Modifier.weight(1f)) {
                                Text(video.channel, style = OndaType.cardTitle, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (channelId != null) Text("Ver canal", style = OndaType.small.copy(color = Lime), modifier = Modifier.clickable { onChannel(channelId) })
                            }
                            if (channelId != null) Pill(if (following) "Seguindo" else "Seguir", { onFollow(Channel(channelId, video.channel)) },
                                icon = if (following) OndaIcons.Check else OndaIcons.Plus, selected = !following)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { Pill(if (favorite) "Remover dos favoritos" else "Favoritar no aparelho", onFavorite, icon = if (favorite) OndaIcons.HeartFilled else OndaIcons.Heart, primary = favorite) }
                            item { Pill("Compartilhar", { share(context, video) }, icon = OndaIcons.Share) }
                        }
                    }
                }
            }
            item("description") {
                OndaSurface(Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp).fillMaxWidth().clickable(onClickLabel = if (expanded) "Recolher descrição" else "Ver descrição") { expanded = !expanded }, RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row { Text("Descrição", style = OndaType.cardTitle, color = c.text, modifier = Modifier.weight(1f)); Text(if (expanded) "Fechar" else "Abrir", style = OndaType.button.copy(color = Lime, fontSize = 12.sp)) }
                        detailError?.let { Text(it, style = OndaType.small, color = Danger) }
                        Text(video.description.ifBlank { if (detailError == null) "Carregando informações…" else "Sem descrição disponível." }, style = OndaType.body, color = c.muted,
                            maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (upNext.isNotEmpty()) {
                item("next-title") {
                    SectionHeader("A seguir")
                    Text("Mais vídeos sobre este assunto",style=OndaType.small,color=c.muted,modifier=Modifier.padding(horizontal=20.dp,vertical=4.dp))
                }
                items(upNext, key = { "n${it.id}" }) { v -> VideoRowCard(v) { open(v) } }
            }
            }
        }
    }
}

/** Shorts: vertical pager. Only the current page creates a player, and only after a tap (battery and data friendly). */
@Composable
fun ShortsScreen(state: CatalogState, library: Library, onTopic: (String) -> Unit, more: () -> Unit, retry: () -> Unit, onPlaying: (Video) -> Unit, onFavorite: (Video) -> Unit) {
    val c = LocalOnda.current
    val context = LocalContext.current
    val items = state.items.filter { it.isShort }
    val pager = rememberPagerState(pageCount = { items.size })
    var started by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pager.currentPage, items.size, state.cursor, state.loading) {
        if (items.isNotEmpty() && pager.currentPage >= items.size - 3 && state.cursor != null && !state.loading && state.error == null) more()
    }
    LaunchedEffect(pager.currentPage) { if (items.getOrNull(pager.currentPage)?.id != started) started = null }
    Box(Modifier.fillMaxSize().background(Color.Black).testTag("shorts")) {
        if (items.isNotEmpty()) VerticalPager(pager, Modifier.fillMaxSize(), key = { items[it].id }) { page ->
            val v = items[page]
            val favorite = library.favorites.any { it.id == v.id }
            Box(Modifier.fillMaxSize()) {
                AsyncImage(v.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(24.dp) else Modifier).alpha(.55f))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .5f), Color.Transparent, Color.Black.copy(alpha = .85f)))))
                // Player area sits left of the action column, so no custom button covers the official iframe.
                Box(Modifier.fillMaxHeight().fillMaxWidth().padding(start = 12.dp, end = 76.dp, top = 120.dp, bottom = 190.dp), contentAlignment = Alignment.Center) {
                    if (started == v.id && page == pager.currentPage) {
                        YouTubePlayer(v, onPlaying = { onPlaying(v) }, aspect = 9f / 16f, showOpenExternal = false)
                    } else {
                        Box(Modifier.fillMaxHeight().aspectRatio(9f / 16f, matchHeightConstraintsFirst = true).clip(RoundedCornerShape(24.dp)).background(tintFor(v.id))
                            .clickable(role = Role.Button, onClickLabel = "Assistir ${v.title}") { started = v.id }.testTag("short-cover"), contentAlignment = Alignment.Center) {
                            AsyncImage(v.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                            Box(Modifier.size(72.dp).clip(CircleShape).background(if (c.glass) Color.White.copy(alpha = .25f) else Color.Black.copy(alpha = .5f))
                                .border(1.dp, Color.White.copy(alpha = .35f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(OndaIcons.Play, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
                Column(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 12.dp, bottom = 170.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ShortAction(if (favorite) OndaIcons.HeartFilled else OndaIcons.Heart, if (favorite) "Salvo" else "Salvar", favorite) { onFavorite(v) }
                    ShortAction(OndaIcons.Share, "Enviar") { share(context, v) }
                    ShortAction(OndaIcons.External, "YouTube") { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/shorts/${v.id}"))) } }
                }
                Column(Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 16.dp, end = 90.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(v.title, style = OndaType.cardTitle.copy(fontSize = 15.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (v.description.isNotBlank()) Text(v.description, style = OndaType.small, color = Color.White.copy(alpha = .75f), maxLines = 1)
                }
            }
        }
        Column(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 10.dp)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(SHORTS_TOPICS, key = { it.label }) { t -> Pill(t.label, { started = null; onTopic(t.query) }, selected = state.heading == t.query) }
            }
        }
        Column(Modifier.align(Alignment.Center).fillMaxWidth()) {
            if (state.loading && items.isEmpty()) LoadingRow("Buscando Shorts…")
            if (state.error != null && items.isEmpty()) StateCard(state.error, true) { Pill("Tentar novamente", retry, primary = true) }
            if (!state.loading && state.error == null && items.isEmpty()) StateCard("Nenhum Short encontrado neste tema.", false) { Pill("Tentar novamente", retry) }
        }
    }
}

@Composable
private fun ShortAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    val c = LocalOnda.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(if (active) Lime else if (c.glass) Color.White.copy(alpha = .16f) else Color.Black.copy(alpha = .45f))
            .border(1.dp, if (active) Lime else Color.White.copy(alpha = if (c.glass) .3f else .15f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, style = OndaType.small.copy(fontSize = 11.sp), color = Color.White)
    }
}
