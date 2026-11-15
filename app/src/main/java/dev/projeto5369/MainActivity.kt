package dev.projeto5369

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import dev.projeto5369.core.*

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(vm: AppViewModel = viewModel()) {
    val nav=rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val library by vm.library.collectAsStateWithLifecycle()
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val searchState by vm.searchState.collectAsStateWithLifecycle()
    val channelState by vm.channelState.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val detailError by vm.detailError.collectAsStateWithLifecycle()
    fun open(video: Video) { vm.select(video); nav.navigate("video/${video.id}") }
    Scaffold(
        topBar={ TopAppBar(title={Text("Projeto 5369")},navigationIcon={if(nav.previousBackStackEntry != null) TextButton(onClick={nav.popBackStack()}) {Text("Voltar")}}) },
        bottomBar={ NavigationBar {
            NavigationBarItem(selected=current?.destination?.route == "home",onClick={ vm.home(); nav.navigate("home") {popUpTo("home") {inclusive=true}} },icon={Text("⌂")},label={Text("Home")})
            NavigationBarItem(selected=current?.destination?.route == "search",onClick={nav.navigate("search") {launchSingleTop=true}},icon={Text("⌕")},label={Text("Pesquisa")})
            NavigationBarItem(selected=current?.destination?.route == "library",onClick={nav.navigate("library") {launchSingleTop=true}},icon={Text("☆")},label={Text("Biblioteca")})
            NavigationBarItem(selected=current?.destination?.route == "settings",onClick={nav.navigate("settings") {launchSingleTop=true}},icon={Text("⚙")},label={Text("Ajustes")})
        } }
    ) { padding ->
        NavHost(navController=nav,startDestination="home",modifier=Modifier.padding(padding).fillMaxSize()) {
            composable("home") { CatalogScreen(homeState,library,{vm.more("home")},{vm.retry("home")},::open,header={
                Text("Seleção por tema, sem personalização oficial do YouTube.",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={nav.navigate("search")}) { Text("Pesquisar ou abrir link de vídeo") }
            }) }
            composable("search") { SearchScreen(library.recentSearches,onSearch={q -> vm.search(q); nav.navigate("results")},onOpen={id -> open(vm.selected(id))}) }
            composable("results") { CatalogScreen(searchState,library,{vm.more("search")},{vm.retry("search")},::open) }
            composable("channel/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) { entry ->
                val id=entry.arguments!!.getString("id")!!
                LaunchedEffect(id) { vm.channel(id) }
                CatalogScreen(channelState,library,{vm.more("channel")},{vm.retry("channel")},::open,header={ channelState.channel?.description?.takeIf { it.isNotBlank() }?.let { Text(it.take(700)) } })
            }
            composable("video/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) { entry ->
                val id=entry.arguments!!.getString("id")!!
                val video=detail?.takeIf { it.id==id } ?: vm.selected(id)
                LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item { AsyncImage(model=video.thumbnail,contentDescription=null,modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f),contentScale=ContentScale.Crop) }
                    item { Text(video.title,style=MaterialTheme.typography.headlineSmall); Text(video.channel); Text(durationLabel(video)) }
                    item { Button(onClick={nav.navigate("player/$id")}) { Text("Reproduzir") } }
                    item { OutlinedButton(onClick={vm.favorite(video)}) {Text(if(library.favorites.any {it.id==id}) "Remover dos favoritos" else "Favoritar no aparelho")} }
                    video.channelId?.takeIf {Regex("UC[A-Za-z0-9_-]{22}").matches(it)}?.let { channelId -> item { TextButton(onClick={nav.navigate("channel/$channelId")}) {Text("Ver canal")} } }
                    item { detailError?.let {Text(it,color=MaterialTheme.colorScheme.error)}; Text(video.description.ifBlank {"Carregando informações…"}) }
                }
            }
            composable("player/{id}",arguments=listOf(navArgument("id"){type=NavType.StringType})) { entry ->
                val video=vm.selected(entry.arguments!!.getString("id")!!)
                LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item { Text(video.title,style=MaterialTheme.typography.titleLarge) }
                    item { YouTubePlayer(video,onPlaying={vm.watched(video)}) }
                }
            }
            composable("library") {
                LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item { Text("Favoritos locais",style=MaterialTheme.typography.titleLarge) }
                    if(library.favorites.isEmpty()) item {Text("Nenhum favorito salvo.")}
                    items(library.favorites,key={"f${it.id}"}) {VideoRow(it) {open(it)}}
                    item {Text("Histórico local",style=MaterialTheme.typography.titleLarge); Text("Registra vídeos quando o player informa reprodução iniciada.",style=MaterialTheme.typography.bodySmall)}
                    if(library.history.isEmpty()) item {Text("Nenhum vídeo assistido neste aparelho.")}
                    items(library.history,key={"h${it.id}"}) {VideoRow(it) {open(it)}}
                }
            }
            composable("settings") { SettingsScreen(library,vm::preferences,vm::clearHistory,onDemo={nav.navigate("media-demo")}) }
            composable("media-demo") { Column(Modifier.padding(16.dp)) {
                Text("Teste do Media3",style=MaterialTheme.typography.titleLarge)
                AuthorizedMediaPlayer(AuthorizedMediaSource("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4","https://www.bigbuckbunny.org/ • CC BY 3.0"))
            } }
        }
    }
}
@Composable
private fun SearchScreen(recent: List<String>,onSearch:(String)->Unit,onOpen:(String)->Unit) {
    var query by rememberSaveable {mutableStateOf("")}
    var link by rememberSaveable {mutableStateOf("")}
    var invalid by rememberSaveable {mutableStateOf(false)}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text("Pesquisa",style=MaterialTheme.typography.headlineSmall)}
        item {OutlinedTextField(value=query,onValueChange={query=it},label={Text("Buscar vídeos longos")},singleLine=true,modifier=Modifier.fillMaxWidth())}
        item {Button(enabled=query.isNotBlank(),onClick={onSearch(query.trim())}) {Text("Pesquisar")}}
        item {Text("Abrir vídeo por link ou ID",style=MaterialTheme.typography.titleMedium)}
        item {OutlinedTextField(value=link,onValueChange={link=it;invalid=false},label={Text("URL do YouTube ou ID")},singleLine=true,isError=invalid,modifier=Modifier.fillMaxWidth())}
        item {Button(onClick={val id=videoId(link); if(id != null) onOpen(id) else invalid=true}) {Text("Abrir vídeo")}; if(invalid) Text("Informe um link do YouTube ou ID válido.",color=MaterialTheme.colorScheme.error)}
        if(recent.isNotEmpty()) item {Text("Pesquisas recentes")}
        items(recent) { q -> TextButton(onClick={query=q;onSearch(q)}) {Text(q)} }
    }
}
@Composable
private fun CatalogScreen(state: CatalogState,library: Library,more:()->Unit,retry:()->Unit,open:(Video)->Unit,header:@Composable ()->Unit = {}) {
    val visible=remember(state.items,library) {library.visible(state.items)}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text(state.heading,style=MaterialTheme.typography.headlineSmall); header(); Text("Filtro local: ${library.minimumMinutes}+ min${if(library.hideWatched) " • ocultar assistidos" else ""}",style=MaterialTheme.typography.bodySmall)}
        items(visible,key={it.id}) {video -> VideoRow(video) {open(video)}}
        if(state.loading) item {CircularProgressIndicator(Modifier.size(28.dp)); Text("Carregando…")}
        if(state.error != null) item {Text(state.error,color=MaterialTheme.colorScheme.error); Button(onClick=retry) {Text("Tentar novamente")}}
        if(!state.loading && state.error==null && visible.isEmpty()) item {Text("Nenhum vídeo corresponde aos filtros nesta página. Ajuste a duração ou carregue mais.")}
        if(state.cursor != null && !state.loading) item {Button(onClick=more) {Text("Carregar mais")}}
    }
}
@Composable
private fun VideoRow(video: Video,onClick:()->Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick=onClick)) {
        Row(Modifier.padding(10.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            AsyncImage(model=video.thumbnail,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.width(128.dp).aspectRatio(16f/9f))
            Column(Modifier.weight(1f)) {Text(video.title,maxLines=3,style=MaterialTheme.typography.titleSmall); Text(video.channel,maxLines=1,style=MaterialTheme.typography.bodySmall); Text(durationLabel(video),style=MaterialTheme.typography.labelSmall)}
        }
    }
}
private fun durationLabel(video: Video): String = video.durationSeconds?.let { seconds -> "${seconds/60} min ${seconds%60} s" } ?: "Duração não informada"
@Composable
private fun SettingsScreen(library: Library,preferences:(Int,Boolean)->Unit,clear:()->Unit,onDemo:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text("Configurações locais",style=MaterialTheme.typography.headlineSmall)}
        item {Text("Duração mínima"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf(0,20,40,60).forEach {m -> FilterChip(selected=library.minimumMinutes==m,onClick={preferences(m,library.hideWatched)},label={Text(if(m==0) "Todas" else "$m min")})}}}
        item {Row {Checkbox(checked=library.hideWatched,onCheckedChange={preferences(library.minimumMinutes,it)}); Text("Ocultar vídeos já assistidos",Modifier.padding(top=12.dp))}}
        item {OutlinedButton(onClick=clear) {Text("Limpar histórico e pesquisas")}}
        item {Text("Conta Google",style=MaterialTheme.typography.titleMedium); Text("Modo visitante ativo. Login oficial pendente de configuração OAuth própria; favoritos e histórico ficam neste aparelho.")}
        item {Text("Reprodução",style=MaterialTheme.typography.titleMedium); Text("YouTube: player incorporado oficial. Incorporação, disponibilidade e autorização dependem do YouTube e do proprietário."); TextButton(onClick=onDemo) {Text("Testar Media3 com amostra autorizada")}}
        item {Text("Projeto 5369 • 0.1.0 • interface provisória\nSem recomendações personalizadas oficiais. Shorts são omitidos do catálogo.",style=MaterialTheme.typography.bodySmall)}
        item {Text("Licenças de dependências",style=MaterialTheme.typography.titleMedium); Text("AndroidX/Compose/Media3/DataStore: Apache 2.0\nKotlin e kotlinx: Apache 2.0\nOkHttp: Apache 2.0\nCoil: Apache 2.0\nProtobuf transitivo do DataStore: BSD 3-Clause\nNenhum código GPL dos clientes de referência incorporado.",style=MaterialTheme.typography.bodySmall)}
    }
}
