package dev.projeto5369

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.projeto5369.core.*
import dev.projeto5369.ui.*

private val BottomSpace = PaddingValues(bottom = 120.dp)

/** Loads the next page when the user nears the end, only while something is visible (no runaway paging on empty filters). */
@Composable
private fun AutoMore(list: LazyListState, state: CatalogState, visibleCount: Int, more: () -> Unit) {
    val near by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { it >= list.layoutInfo.totalItemsCount - 4 } ?: false } }
    LaunchedEffect(near, state.cursor, state.loading, state.error, visibleCount) {
        if (near && visibleCount > 0 && state.cursor != null && !state.loading && state.error == null) more()
    }
}

/** Shared tail of every catalog list: loading, error with retry, empty after filters, manual "load more". */
private fun androidx.compose.foundation.lazy.LazyListScope.catalogStates(state: CatalogState, visibleCount: Int, more: () -> Unit, retry: () -> Unit, extra: @Composable RowScope.() -> Unit = {}) {
    if (state.loading) item("loading") { LoadingRow(if (state.items.isEmpty()) "Buscando vídeos…" else "Carregando mais…") }
    if (state.error != null) item("error") {
        StateCard(state.error, true, Modifier.padding(top = 12.dp)) { Pill("Tentar novamente", retry, primary = true); extra() }
    }
    if (!state.loading && state.error == null && visibleCount == 0) item("empty") {
        StateCard(if (state.items.isEmpty()) "Nenhum vídeo encontrado." else "Nenhum vídeo corresponde aos filtros nesta página. Ajuste a duração em Ajustes ou carregue mais.", false, Modifier.padding(top = 12.dp)) {
            if (state.cursor != null) Pill("Carregar mais", more, primary = true)
        }
    }
    if (state.cursor != null && !state.loading && state.error == null && visibleCount > 0) item("more") {
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Pill("Carregar mais", more) }
    }
}

@Composable
fun HomeScreen(state: CatalogState, library: Library, topic: Topic, onTopic: (Topic) -> Unit, more: () -> Unit, retry: () -> Unit, open: (Video) -> Unit,
               onSearch: () -> Unit, onSettings: () -> Unit, onRoute: (String) -> Unit) {
    val c = LocalOnda.current
    val visible = remember(state.items, library) { library.visible(state.items) }
    val list = rememberLazyListState()
    AutoMore(list, state, visible.size, more)
    LazyColumn(Modifier.fillMaxSize().testTag("home-list"), state = list, contentPadding = BottomSpace) {
        item("title") {
            ScreenTitle("ONDA TUBE · ${if (c.glass) "GLASS" else "SELEÇÃO"}", "Início") {
                CircleIcon(OndaIcons.Search, "Pesquisar", onSearch)
                CircleIcon(OndaIcons.Sliders, "Ajustes", onSettings)
            }
        }
        item("wave") { WaveLine(Modifier.padding(top = 6.dp)) }
        item("chips") {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(HOME_TOPICS, key = { it.label }) { t -> Pill(t.label, { onTopic(t) }, selected = t == topic) }
            }
            Text("Seleção por tema a partir de buscas públicas · não é recomendação personalizada do YouTube · filtro local ${library.minimumMinutes}+ min",
                style = OndaType.small, color = c.muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        }
        visible.firstOrNull()?.let { first -> item("featured-${first.id}") { FeaturedVideo(first, "EM DESTAQUE · ${topic.label.uppercase()}") { open(first) } } }
        if (library.history.isNotEmpty()) item("continue") {
            SectionHeader("Continue assistindo", "Histórico") { onRoute("list/history") }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(library.history.take(12), key = { "h${it.id}" }) { v -> VideoTile(v, { open(v) }) }
            }
        }
        item("navigate") {
            SectionHeader("Navegar")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ColorCard("VERTICAIS", "Shorts", "Feed por tema", listOf(Color(0xFFE2506E), Color(0xFF5A1430)), { onRoute("shorts") }) }
                item { ColorCard("NO APARELHO", "Favoritos", "${library.favorites.size} vídeos", listOf(Color(0xFFD9A23C), Color(0xFF6E4A12)), { onRoute("list/favorites") }) }
                item { ColorCard("SEGUINDO", "Canais", "${library.channels.size} canais", listOf(Color(0xFF4C5BD4), Color(0xFF1E1F5A)), { onRoute("list/channels") }) }
                item { ColorCard("LOCAL", "Seu Tube", "Estatísticas", listOf(Color(0xFF6BBF3A), Color(0xFF1F4A12)), { onRoute("stats") }) }
            }
        }
        if (visible.size > 1) item("feed-title") { SectionHeader(topic.label) }
        items(visible.drop(1), key = { it.id }) { v -> VideoRowCard(v) { open(v) } }
        catalogStates(state, visible.size, more, retry) { Pill("Pesquisar", onSearch) }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, onSubmit: () -> Unit, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector = OndaIcons.Search, tag: String? = null) {
    val c = LocalOnda.current
    Row(modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(c.surface).border(1.dp, c.border, RoundedCornerShape(26.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = c.muted, modifier = Modifier.size(18.dp))
        BasicTextField(value, onChange, singleLine = true, textStyle = OndaType.body.copy(color = c.text, fontSize = 16.sp), cursorBrush = SolidColor(Lime),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSubmit() }, onGo = { onSubmit() }),
            modifier = Modifier.weight(1f).semantics { contentDescription = placeholder }.then(if (tag != null) Modifier.testTag(tag) else Modifier),
            decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, style = OndaType.body.copy(fontSize = 16.sp), color = c.muted); inner() } })
    }
}

@Composable
fun SearchScreen(recent: List<String>, onSearch: (String) -> Unit, onOpen: (String) -> Unit, onForget: (String) -> Unit) {
    val c = LocalOnda.current
    var query by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf(false) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { query = it; onSearch(it.trim()) }
    }
    val submit = { if (query.isNotBlank()) onSearch(query.trim()) }
    LazyColumn(Modifier.fillMaxSize().testTag("search-list"), contentPadding = BottomSpace) {
        item("title") { ScreenTitle("BUSCAR NO YOUTUBE", "Pesquisa") }
        item("field") {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SearchField(query, { query = it }, "Vídeos, canais, assuntos…", submit, Modifier.weight(1f), tag = "search-field")
                CircleIcon(OndaIcons.Mic, "Pesquisar por voz", {
                    voiceError = false
                    try { voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")) }
                    catch (_: ActivityNotFoundException) { voiceError = true }
                }, accent = true, size = 52.dp)
            }
            Row(Modifier.padding(start = 16.dp, top = 10.dp)) { Pill("Pesquisar", submit, primary = true, enabled = query.isNotBlank()) }
            if (voiceError) Text("Este aparelho não tem reconhecimento de voz disponível.", style = OndaType.small, color = Danger, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        }
        item("topics") {
            SectionHeader("Assuntos")
            val topics = (recent.take(4).map { Topic(it, it) } + HOME_TOPICS.drop(1)).distinctBy { it.label.lowercase() }.take(4)
            val tints = listOf(listOf(Color(0xFF5A7A3A), Color(0xFF1A2610)), listOf(Color(0xFF4C5BD4), Color(0xFF1E1F5A)), listOf(Color(0xFF9A6BD4), Color(0xFF2E1A4A)), listOf(Color(0xFFD9A23C), Color(0xFF6E4A12)))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topics.chunked(2).forEachIndexed { row, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEachIndexed { i, t ->
                            val colors = tints[(row * 2 + i) % tints.size]
                            Column(Modifier.weight(1f).height(82.dp).clip(RoundedCornerShape(22.dp))
                                .background(if (c.glass) Brush.linearGradient(colors.map { it.copy(alpha = .4f) }) else Brush.linearGradient(colors))
                                .clickable(role = Role.Button) { query = t.query; onSearch(t.query) }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(if (recent.contains(t.label)) "RECENTE" else "TEMA", style = OndaType.label.copy(fontSize = 10.sp), color = Color.White.copy(alpha = .85f))
                                Text(t.label, style = OndaType.cardTitle.copy(fontSize = 17.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item("recent-title") { SectionHeader("Pesquisas recentes") }
            items(recent, key = { "r$it" }) { q ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) { Icon(OndaIcons.Clock, null, tint = c.muted, modifier = Modifier.size(16.dp)) }
                    Text(q, style = OndaType.body.copy(fontSize = 15.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { query = q; onSearch(q) }.padding(vertical = 12.dp))
                    CircleIcon(OndaIcons.Close, "Remover \"$q\" do histórico", { onForget(q) }, size = 40.dp)
                }
            }
        }
        item("link") {
            SectionHeader("Abrir vídeo por link")
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SearchField(link, { link = it; invalid = false }, "URL do YouTube ou ID", { val id = videoId(link); if (id != null) onOpen(id) else invalid = true }, icon = OndaIcons.Link, tag = "link-field")
                Pill("Abrir vídeo", { val id = videoId(link); if (id != null) onOpen(id) else invalid = true }, icon = OndaIcons.Play)
                if (invalid) Text("Informe um link do YouTube ou ID válido.", style = OndaType.small, color = Danger)
            }
        }
    }
}

@Composable
fun BackTitle(kicker: String, title: String, onBack: () -> Unit, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircleIcon(OndaIcons.Back, "Voltar", onBack)
        Column(Modifier.weight(1f)) {
            Text(kicker, style = OndaType.label, color = LocalOnda.current.muted)
            Text(title, style = OndaType.title, color = LocalOnda.current.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

@Composable
fun ResultsScreen(state: CatalogState, library: Library, more: () -> Unit, retry: () -> Unit, open: (Video) -> Unit, onBack: () -> Unit) {
    val visible = remember(state.items, library) { library.visible(state.items) }
    val list = rememberLazyListState()
    AutoMore(list, state, visible.size, more)
    LazyColumn(Modifier.fillMaxSize().testTag("results-list"), state = list, contentPadding = BottomSpace) {
        item("title") {
            BackTitle("RESULTADOS · FILTRO ${library.minimumMinutes}+ MIN", state.heading, onBack)
            Spacer(Modifier.height(12.dp))
        }
        items(visible, key = { it.id }) { v -> VideoRowCard(v) { open(v) } }
        catalogStates(state, visible.size, more, retry)
    }
}

@Composable
fun ChannelScreen(id: String, state: CatalogState, library: Library, more: () -> Unit, retry: () -> Unit, open: (Video) -> Unit, onFollow: (Channel) -> Unit, onBack: () -> Unit) {
    val c = LocalOnda.current
    val channel = state.channel?.takeIf { it.id == id }
    val items = if (channel != null) state.items else emptyList()
    val visible = remember(items, library) { library.visible(items) }
    val following = library.channels.any { it.id == id }
    var expanded by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    AutoMore(list, state, visible.size, more)
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = BottomSpace) {
        item("title") { BackTitle("CANAL", channel?.title ?: "Carregando…", onBack) }
        if (channel != null) item("header") {
            OndaSurface(Modifier.padding(16.dp).fillMaxWidth(), RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(tintFor(channel.title), Color.Black))).border(3.dp, Lime, CircleShape),
                            contentAlignment = Alignment.Center) { Text(channel.title.take(1).uppercase(), style = OndaType.title, color = Color.White) }
                        Column(Modifier.weight(1f)) {
                            Text(channel.title, style = OndaType.title, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${visible.size} vídeos longos nesta página", style = OndaType.small, color = c.muted)
                        }
                    }
                    if (channel.description.isNotBlank()) Text(channel.description, style = OndaType.small, color = c.muted, maxLines = if (expanded) 30 else 3,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { expanded = !expanded })
                    Pill(if (following) "Seguindo no aparelho" else "Seguir no aparelho", { onFollow(channel) }, icon = if (following) OndaIcons.Check else OndaIcons.Plus, primary = !following)
                    Text("Seguir aqui só guarda o canal neste aparelho; não é inscrição na sua conta do YouTube.", style = OndaType.small, color = c.muted)
                }
            }
        }
        items(visible, key = { it.id }) { v -> VideoRowCard(v) { open(v) } }
        catalogStates(state.takeIf { channel != null || it.loading || it.error != null } ?: state.copy(loading = true), visible.size, more, retry)
    }
}

@Composable
fun LibraryScreen(library: Library, open: (Video) -> Unit, onList: (String) -> Unit, onSettings: () -> Unit) {
    val c = LocalOnda.current
    LazyColumn(Modifier.fillMaxSize().testTag("library-list"), contentPadding = BottomSpace) {
        item("title") { ScreenTitle("VOCÊ", "Biblioteca") { CircleIcon(OndaIcons.Sliders, "Ajustes", onSettings) } }
        item("profile") {
            OndaSurface(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp).fillMaxWidth(), RoundedCornerShape(28.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFF2C48D), Color(0xFFC97A3A)))).border(3.dp, Lime, CircleShape),
                        contentAlignment = Alignment.Center) { Icon(OndaIcons.User, null, tint = Color.Black, modifier = Modifier.size(28.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Visitante", style = OndaType.title, color = c.text)
                        Text("Login Google ainda não configurado. Tudo aqui fica salvo só neste aparelho.", style = OndaType.small, color = c.muted)
                    }
                }
            }
        }
        item("stats") {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatBox("ASSISTIDOS", "${library.history.size}", "vídeos iniciados", Modifier.weight(1f))
                StatBox("FAVORITOS", "${library.favorites.size}", "no aparelho", Modifier.weight(1f))
            }
        }
        if (library.history.isNotEmpty()) item("history") {
            SectionHeader("Histórico", "Ver tudo") { onList("history") }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(library.history.take(12), key = { "h${it.id}" }) { v -> VideoTile(v, { open(v) }, 176.dp) }
            }
        }
        item("folders") {
            SectionHeader("Pastas")
            Column(Modifier.padding(horizontal = 16.dp).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FolderCard("Favoritos locais", "${library.favorites.size}", "salvos no aparelho", Color(0xFFE9B44C), Color(0xFF1A1206), { onList("favorites") }, Modifier.weight(1f))
                    FolderCard("Histórico", "${library.history.size}", "vídeos iniciados", Color(0xFFF2708A), Color(0xFF2A0716), { onList("history") }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FolderCard("Canais", "${library.channels.size}", "seguidos aqui", Color(0xFF8FB0FF), Color(0xFF0B1530), { onList("channels") }, Modifier.weight(1f))
                    FolderCard("Pesquisas", "${library.recentSearches.size}", "recentes", Lime, Color(0xFF101B16), { onList("searches") }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun StatBox(kicker: String, value: String, sub: String, modifier: Modifier = Modifier) {
    val c = LocalOnda.current
    OndaSurface(modifier, RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(kicker, style = OndaType.label.copy(fontSize = 10.sp), color = c.muted)
            Text(value, style = OndaType.title.copy(fontSize = 24.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = OndaType.small, color = c.muted)
        }
    }
}

@Composable
fun ListScreen(kind: String, library: Library, open: (Video) -> Unit, onChannel: (String) -> Unit, onSearch: (String) -> Unit, onForget: (String) -> Unit, onBack: () -> Unit) {
    val c = LocalOnda.current
    val (kicker, title) = when (kind) {
        "favorites" -> "NO APARELHO" to "Favoritos locais"; "history" -> "NO APARELHO" to "Histórico local"
        "channels" -> "SEGUIDOS AQUI" to "Canais"; else -> "NO APARELHO" to "Pesquisas recentes"
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = BottomSpace) {
        item("title") { BackTitle(kicker, title, onBack); Spacer(Modifier.height(12.dp)) }
        when (kind) {
            "favorites" -> {
                if (library.favorites.isEmpty()) item { StateCard("Nenhum favorito salvo.", false) }
                items(library.favorites, key = { "f${it.id}" }) { v -> VideoRowCard(v) { open(v) } }
            }
            "history" -> {
                item { Text("Registra vídeos quando o player oficial informa reprodução iniciada.", style = OndaType.small, color = c.muted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) }
                if (library.history.isEmpty()) item { StateCard("Nenhum vídeo assistido neste aparelho.", false) }
                items(library.history, key = { "h${it.id}" }) { v -> VideoRowCard(v) { open(v) } }
            }
            "channels" -> {
                if (library.channels.isEmpty()) item { StateCard("Nenhum canal seguido. Abra um canal e toque em \"Seguir no aparelho\".", false) }
                items(library.channels, key = { "c${it.id}" }) { ch ->
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onChannel(ch.id) }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(tintFor(ch.title), Color.Black))), contentAlignment = Alignment.Center) {
                            Text(ch.title.take(1).uppercase(), style = OndaType.button, color = Color.White)
                        }
                        Text(ch.title, style = OndaType.cardTitle, color = c.text, modifier = Modifier.weight(1f))
                    }
                }
            }
            else -> {
                if (library.recentSearches.isEmpty()) item { StateCard("Nenhuma pesquisa recente.", false) }
                items(library.recentSearches, key = { "s$it" }) { q ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(q, style = OndaType.body.copy(fontSize = 15.sp), color = c.text, modifier = Modifier.weight(1f).clickable { onSearch(q) }.padding(12.dp))
                        CircleIcon(OndaIcons.Close, "Remover \"$q\"", { onForget(q) }, size = 40.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(library: Library, open: (Video) -> Unit, onChannel: (String) -> Unit) {
    val c = LocalOnda.current
    val seconds = library.history.sumOf { it.durationSeconds ?: 0L }
    val top = library.history.filter { it.channel.isNotBlank() }.groupBy { it.channel }.map { (name, vids) -> Triple(name, vids.size, vids.firstNotNullOfOrNull { it.channelId }) }
        .sortedByDescending { it.second }.take(5)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = BottomSpace) {
        item("title") { ScreenTitle("SÓ NESTE APARELHO", "Seu Tube") }
        item("wave") { WaveLine(Modifier.padding(top = 6.dp)) }
        item("boxes") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("INICIADOS", "${library.history.size}", "vídeos", Modifier.weight(1f))
                    StatBox("DURAÇÃO SOMADA", if (seconds >= 3600) "${seconds / 3600} h ${(seconds % 3600) / 60}" else "${seconds / 60} min", "dos vídeos iniciados", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("FAVORITOS", "${library.favorites.size}", "salvos", Modifier.weight(1f))
                    StatBox("CANAIS", "${library.channels.size}", "seguidos aqui", Modifier.weight(1f))
                }
                Text("Calculado a partir do histórico local. A duração somada é a dos vídeos iniciados, não o tempo realmente assistido.", style = OndaType.small, color = c.muted)
            }
        }
        if (top.isNotEmpty()) {
            item("top-title") { SectionHeader("Canais mais vistos") }
            items(top, key = { "t${it.first}" }) { (name, count, id) ->
                Row(Modifier.fillMaxWidth().then(if (id != null) Modifier.clickable { onChannel(id) } else Modifier).padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(listOf(tintFor(name), Color.Black))), contentAlignment = Alignment.Center) {
                        Text(name.take(1).uppercase(), style = OndaType.button, color = Color.White)
                    }
                    Text(name, style = OndaType.cardTitle, color = c.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$count", style = OndaType.title, color = Lime)
                }
            }
        } else item("empty") { StateCard("Assista alguns vídeos para ver suas estatísticas aqui.", false) }
        library.history.firstOrNull()?.let { last -> item("last") { SectionHeader("Último iniciado"); VideoRowCard(last) { open(last) } } }
    }
}

@Composable
fun SettingsScreen(library: Library, preferences: (Int, Boolean) -> Unit, glass: (Boolean) -> Unit, clear: () -> Unit, onDemo: () -> Unit, onBack: () -> Unit) {
    val c = LocalOnda.current
    @Composable fun Card(title: String, content: @Composable ColumnScope.() -> Unit) {
        OndaSurface(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = OndaType.cardTitle.copy(fontSize = 17.sp), color = c.text); content() }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = BottomSpace) {
        item("title") { BackTitle("AJUSTES", "Configurações", onBack); Spacer(Modifier.height(10.dp)) }
        item("theme") { Card("Tema") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Preto AMOLED", { glass(false) }, selected = !library.glass)
                Pill("Onda Glass", { glass(true) }, selected = library.glass)
            }
            Text("O Glass usa luzes em degradê, sem desfoque em tempo real, para não pesar na bateria.", style = OndaType.small, color = c.muted)
        } }
        item("duration") { Card("Duração mínima") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 20, 40, 60).forEach { m -> Pill(if (m == 0) "Todas" else "$m min", { preferences(m, library.hideWatched) }, selected = library.minimumMinutes == m) }
            }
            Text("Filtro local do Início, pesquisa e canais. Shorts ficam na aba própria.", style = OndaType.small, color = c.muted)
        } }
        item("watched") { Card("Assistidos") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ocultar vídeos já assistidos", style = OndaType.body, color = c.text, modifier = Modifier.weight(1f))
                Switch(library.hideWatched, { preferences(library.minimumMinutes, it) }, colors = SwitchDefaults.colors(checkedTrackColor = Lime, checkedThumbColor = Color.Black))
            }
            Pill("Limpar histórico e pesquisas", clear)
        } }
        item("account") { Card("Conta Google") {
            Text("Modo visitante ativo. Login oficial pendente de configuração OAuth própria; favoritos, histórico e canais ficam neste aparelho.", style = OndaType.body, color = c.muted)
        } }
        item("playback") { Card("Reprodução") {
            Text("YouTube: player incorporado oficial, com os controles e anúncios dele. Incorporação, disponibilidade e autorização dependem do YouTube e do proprietário. O botão de tela cheia do player gira para horizontal.", style = OndaType.body, color = c.muted)
            Pill("Testar Media3 com amostra autorizada", onDemo)
        } }
        item("about") { Card("Sobre") {
            Text("OndaTube 0.2.0 · ecossistema Onda · JoojDevs\nBase técnica: Projeto 5369. Sem recomendações personalizadas oficiais; Shorts ficam fora do catálogo de vídeos longos.", style = OndaType.small, color = c.muted)
        } }
        item("licenses") { Card("Licenças de dependências") {
            Text("AndroidX/Compose/Media3/DataStore: Apache 2.0\nKotlin e kotlinx: Apache 2.0\nOkHttp: Apache 2.0\nCoil: Apache 2.0\nProtobuf transitivo do DataStore: BSD 3-Clause\nFonte Inter: SIL Open Font License 1.1\nNenhum código GPL dos clientes de referência incorporado.", style = OndaType.small, color = c.muted)
        } }
    }
}

@Composable
fun MediaDemoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BackTitle("TESTE", "Media3", onBack)
        Box(Modifier.padding(16.dp)) {
            AuthorizedMediaPlayer(AuthorizedMediaSource("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4", "https://www.bigbuckbunny.org/ • CC BY 3.0"))
        }
    }
}
