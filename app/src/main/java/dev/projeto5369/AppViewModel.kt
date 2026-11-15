package dev.projeto5369

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.projeto5369.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class CatalogState(
    val heading: String = "Conteúdos longos", val items: List<Video> = emptyList(),
    val loading: Boolean = false, val error: String? = null, val cursor: Cursor? = null,
    val channel: Channel? = null,
)
data class Topic(val label: String, val query: String)
val HOME_TOPICS = listOf(
    Topic("Tudo","documentário completo ciência história"), Topic("Documentários","documentário completo"),
    Topic("Ciência","palestra ciência"), Topic("História","história documentário completo"), Topic("Música","show completo ao vivo"),
    Topic("Ônibus","viagem de ônibus"), Topic("Tecnologia","tecnologia análise completa"),
)
val SHORTS_TOPICS = listOf(Topic("Para você","curiosidades"), Topic("Ônibus","ônibus"), Topic("Tecnologia","tecnologia"), Topic("Humor","humor"), Topic("Música","música"))
class AppViewModel(app: Application): AndroidViewModel(app) {
    private val accountPreferences = app.getSharedPreferences("account_restore", android.content.Context.MODE_PRIVATE)
    internal val account = AccountSession(viewModelScope, GoogleAccountGateway(app, BuildConfig.GOOGLE_WEB_CLIENT_ID),
        BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank(), { accountPreferences.getBoolean("enabled", false) },
        { accountPreferences.edit().putBoolean("enabled", it).apply() })
    private val catalog: Catalog = YouTubeCatalog(onHttp = { endpoint, status -> android.util.Log.i("P5369Catalog", "stage=$endpoint http=$status") })
    private val store = LocalStore(app)
    val library = store.library.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),Library())
    private val mutableHome = MutableStateFlow(CatalogState())
    private val mutableSearch = MutableStateFlow(CatalogState(heading="Resultados"))
    private val mutableChannel = MutableStateFlow(CatalogState(heading="Canal"))
    private val mutableShorts = MutableStateFlow(CatalogState(heading="Shorts"))
    val shortsState = mutableShorts.asStateFlow()
    private val mutableTopic = MutableStateFlow(HOME_TOPICS.first())
    val topic = mutableTopic.asStateFlow()
    val homeState = mutableHome.asStateFlow()
    val searchState = mutableSearch.asStateFlow()
    val channelState = mutableChannel.asStateFlow()
    private val mutableDetail = MutableStateFlow<Video?>(null)
    val detail = mutableDetail.asStateFlow()
    private val mutableDetailError = MutableStateFlow<String?>(null)
    val detailError = mutableDetailError.asStateFlow()
    private val jobs = mutableMapOf<String,Job>()
    private val reload = mutableMapOf<String,() -> Unit>()
    private var detailRequest: Job? = null
    init { home() }
    private fun feed(key: String) = when(key) { "home" -> mutableHome; "channel" -> mutableChannel; "shorts" -> mutableShorts; else -> mutableSearch }
    fun home() { if(mutableHome.value.items.isEmpty() && !mutableHome.value.loading) search(mutableTopic.value.query,false) }
    /** Themed selection chips on Início: a public search per theme, never presented as personal recommendations. */
    fun topic(topic: Topic) { if(topic == mutableTopic.value && mutableHome.value.items.isNotEmpty()) return; mutableTopic.value=topic; search(topic.query,false) }
    fun shorts(query: String = SHORTS_TOPICS.first().query, force: Boolean = false) {
        if(!force && mutableShorts.value.heading == query && (mutableShorts.value.items.isNotEmpty() || mutableShorts.value.loading)) return
        reload["shorts"] = { shorts(query,true) }; jobs["shorts"]?.cancel()
        mutableShorts.value = CatalogState(heading=query,loading=true)
        jobs["shorts"] = viewModelScope.launch { load(mutableShorts,fetch={ catalog.shorts(query) }) }
    }
    fun glass(enabled: Boolean) { viewModelScope.launch { persist { it.copy(glass=enabled) } } }
    fun follow(channel: Channel) { viewModelScope.launch { persist { it.follow(channel) } } }
    fun forgetSearch(query: String) { viewModelScope.launch { persist { it.forget(query) } } }
    fun search(query: String, save: Boolean = true) {
        if(query.isBlank()) return
        val key = if(save) "search" else "home"; val target=feed(key)
        reload[key] = { search(query,save) }; jobs[key]?.cancel()
        target.value = CatalogState(heading=if(save) query else mutableTopic.value.label,loading=true)
        jobs[key] = viewModelScope.launch {
            if(save) persist { it.searched(query) }
            load(target,fetch={ catalog.search(query) })
        }
    }
    fun channel(id: String) {
        if(mutableChannel.value.channel?.id == id) return
        val target=mutableChannel
        reload["channel"] = { target.value=CatalogState(); channel(id) }; jobs["channel"]?.cancel()
        target.value = CatalogState(heading="Canal",loading=true)
        jobs["channel"] = viewModelScope.launch {
            try {
                val page = catalog.channel(id)
                target.value = CatalogState(page.channel.title,page.videos.items,cursor=page.videos.next,channel=page.channel)
            } catch(e: CancellationException) { throw e } catch(e: Exception) { fail(target,e) }
        }
    }
    private suspend fun load(target: MutableStateFlow<CatalogState>,fetch: suspend () -> Page<Video>,append: Boolean = false) {
        try {
            val page = fetch()
            target.update { current ->
                val items=page.items.map { video -> current.channel?.let { channel ->
                    video.copy(channel=video.channel.ifBlank { channel.title },channelId=video.channelId ?: channel.id)
                } ?: video }
                current.copy(items=(if(append) current.items + items else items).distinctBy(Video::id),cursor=page.next,loading=false,error=null)
            }
        } catch(e: CancellationException) { throw e } catch(e: Exception) { fail(target,e) }
    }
    fun more(key: String) {
        val target=feed(key); val cursor = target.value.cursor ?: return
        if(target.value.loading) return
        target.update { it.copy(loading=true,error=null) }
        jobs[key] = viewModelScope.launch { load(target,{ catalog.next(cursor) },true) }
    }
    fun retry(key: String) { if(feed(key).value.items.isNotEmpty() && feed(key).value.cursor != null) more(key) else reload[key]?.invoke() }
    private fun fail(target: MutableStateFlow<CatalogState>,e: Exception) { target.update { it.copy(loading=false,error=if(e is CatalogException) e.message else "Não foi possível carregar o catálogo. Tente novamente.") } }
    fun select(video: Video) {
        detailRequest?.cancel(); mutableDetail.value=video; mutableDetailError.value=null
        detailRequest=viewModelScope.launch {
            try {
                android.util.Log.i("P5369Player", "video=${video.id} stage=metadata")
                val d = catalog.video(video.id)
                // Keep what the list already knew when the details source is partial (oEmbed has no duration/channel id).
                mutableDetail.value = d.copy(channel = d.channel.ifBlank { video.channel }, channelId = d.channelId ?: video.channelId,
                    durationSeconds = d.durationSeconds ?: video.durationSeconds, description = d.description.ifBlank { video.description })
            }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { android.util.Log.w("P5369Player", "video=${video.id} stage=metadata category=${(e as? CatalogException)?.kind ?: "unknown"}"); mutableDetailError.value=if(e is CatalogException) e.message else "Metadados indisponíveis." }
        }
    }
    fun selected(id: String): Video = detail.value?.takeIf { it.id == id } ?: library.value.favorites.firstOrNull { it.id == id }
        ?: library.value.history.firstOrNull { it.id == id } ?: (homeState.value.items + searchState.value.items + channelState.value.items + shortsState.value.items).firstOrNull { it.id == id }
        ?: Video(id,"Vídeo $id",thumbnail="https://i.ytimg.com/vi/$id/hqdefault.jpg")
    fun favorite(video: Video) { viewModelScope.launch { persist { it.favorite(video) } } }
    fun watched(video: Video) { viewModelScope.launch { persist { it.watched(video) } } }
    fun preferences(minutes: Int,hide: Boolean) { viewModelScope.launch { persist { it.copy(minimumMinutes=minutes,hideWatched=hide) } } }
    fun clearHistory() { viewModelScope.launch { persist { it.copy(history=emptyList(),recentSearches=emptyList()) } } }
    private suspend fun persist(transform: (Library) -> Library) {
        try { store.update(transform) } catch(e: CancellationException) { throw e }
        catch(_: Exception) { mutableHome.update { it.copy(error="Não foi possível salvar os dados locais. Verifique o armazenamento.") } }
    }
}
