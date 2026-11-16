package dev.projeto5369

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dev.projeto5369.core.*
import dev.projeto5369.ui.*

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

fun share(context: android.content.Context, video: Video) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "${video.title}\nhttps://youtu.be/${video.id}")
    runCatching { context.startActivity(Intent.createChooser(send, "Compartilhar vídeo")) }
}

private fun NavHostController.tab(route: String) = navigate(route) {
    popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true
}

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val accountState by vm.account.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.account.restore() }
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val library by vm.library.collectAsStateWithLifecycle()
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val searchState by vm.searchState.collectAsStateWithLifecycle()
    val channelState by vm.channelState.collectAsStateWithLifecycle()
    val shortsState by vm.shortsState.collectAsStateWithLifecycle()
    val topic by vm.topic.collectAsStateWithLifecycle()
    val relatedState by vm.relatedState.collectAsStateWithLifecycle()
    val detail by vm.detail.collectAsStateWithLifecycle()
    val detailError by vm.detailError.collectAsStateWithLifecycle()
    fun open(video: Video) { vm.select(video); nav.navigate("video/${video.id}") }
    val route = current?.destination?.route
    val tab = when {
        route == "home" -> Tab.HOME; route == "shorts" -> Tab.SHORTS; route == "search" || route == "results" -> Tab.SEARCH
        route == "stats" -> Tab.STATS; route == "library" || route?.startsWith("list/") == true || route == "settings" -> Tab.YOU
        else -> null
    }
    val showHotbar = route != null && !route.startsWith("video/") && !route.startsWith("player/") && route != "media-demo"

    OndaTheme(glass = library.glass) {
        OndaBackground {
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    HomeScreen(homeState, library, topic, { vm.topic(it) }, { vm.more("home") }, { vm.retry("home") }, ::open,
                        onSearch = { nav.tab("search") }, onSettings = { nav.navigate("settings") }, onRoute = { nav.navigate(it) }, onRefresh = vm::refreshHome)
                }
                composable("search") {
                    SearchScreen(library.recentSearches, onSearch = { q -> vm.search(q); nav.navigate("results") },
                        onOpen = { id -> open(vm.selected(id)) }, onForget = vm::forgetSearch)
                }
                composable("results") {
                    ResultsScreen(searchState, library, { vm.more("search") }, { vm.retry("search") }, ::open, onBack = { nav.popBackStack() })
                }
                composable("channel/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments!!.getString("id")!!
                    LaunchedEffect(id) { vm.channel(id) }
                    ChannelScreen(id, channelState, library, { vm.more("channel") }, { vm.retry("channel") }, ::open, vm::follow, onBack = { nav.popBackStack() })
                }
                composable("video/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments!!.getString("id")!!
                    val video = detail?.takeIf { it.id == id } ?: vm.selected(id)
                    LaunchedEffect(id) { vm.ensureRelated(video) }
                    val upNext = relatedState.takeIf { it.videoId == id }?.items.orEmpty().filter { it.id != id }.take(20)
                    WatchScreen(video, detailError, library, upNext, onPlaying = { vm.watched(video) }, onFavorite = { vm.favorite(video) },
                        onFollow = vm::follow, onChannel = { nav.navigate("channel/$it") }, open = ::open, onBack = { nav.popBackStack() })
                }
                composable("player/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    val id = entry.arguments!!.getString("id")!!
                    val video = vm.selected(id)
                    LaunchedEffect(id) { vm.ensureRelated(video) }
                    WatchScreen(video, detailError, library, relatedState.takeIf { it.videoId == id }?.items.orEmpty().filter { it.id != id }.take(20), onPlaying = { vm.watched(video) }, onFavorite = { vm.favorite(video) },
                        onFollow = vm::follow, onChannel = { nav.navigate("channel/$it") }, open = ::open, onBack = { nav.popBackStack() })
                }
                composable("shorts") {
                    LaunchedEffect(Unit) { vm.shorts() }
                    ShortsScreen(shortsState, library, onTopic = { vm.shorts(it) }, more = { vm.more("shorts") }, retry = { vm.retry("shorts") },
                        onPlaying = vm::watched, onFavorite = vm::favorite)
                }
                composable("library") {
                    LibraryScreen(library, ::open, onList = { nav.navigate("list/$it") }, onSettings = { nav.navigate("settings") }, accountState = accountState)
                }
                composable("list/{kind}", arguments = listOf(navArgument("kind") { type = NavType.StringType })) { entry ->
                    ListScreen(entry.arguments!!.getString("kind")!!, library, ::open, onChannel = { nav.navigate("channel/$it") },
                        onSearch = { q -> vm.search(q); nav.navigate("results") }, onForget = vm::forgetSearch, onBack = { nav.popBackStack() })
                }
                composable("stats") { StatsScreen(library, ::open, onChannel = { nav.navigate("channel/$it") }) }
                composable("settings") {
                    SettingsScreen(library, vm::preferences, vm::glass, vm::clearHistory, onDemo = { nav.navigate("media-demo") }, onBack = { nav.popBackStack() }, accountState = accountState, onSignIn = { vm.account.signIn() }, onSignOut = vm.account::signOut, onCancelAuth = vm.account::cancel)
                }
                composable("media-demo") { MediaDemoScreen(onBack = { nav.popBackStack() }) }
            }
            if (showHotbar) Hotbar(tab, { selected -> if (selected == Tab.HOME) vm.home(); nav.tab(selected.route) }, Modifier.align(Alignment.BottomCenter))
        }
    }
}
