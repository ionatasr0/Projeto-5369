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
class AppViewModel(app: Application): AndroidViewModel(app) {
    private val catalog: Catalog = YouTubeCatalog()
    private val store = LocalStore(app)
    val library = store.library.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),Library())
    private val mutableHome = MutableStateFlow(CatalogState())
    private val mutableSearch = MutableStateFlow(CatalogState(heading="Resultados"))
    private val mutableChannel = MutableStateFlow(CatalogState(heading="Canal"))
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
    private fun feed(key: String) = when(key) { "home" -> mutableHome; "channel" -> mutableChannel; else -> mutableSearch }
    fun home() { if(mutableHome.value.items.isEmpty() && !mutableHome.value.loading) search("documentário completo ciência história",false) }
    fun search(query: String, save: Boolean = true) {
        if(query.isBlank()) return
        val key = if(save) "search" else "home"; val target=feed(key)
        reload[key] = { search(query,save) }; jobs[key]?.cancel()
        target.value = CatalogState(heading=if(save) "Resultados: $query" else "Seleção por tema • vídeos longos",loading=true)
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
            target.update { it.copy(items=(if(append) it.items + page.items else page.items).distinctBy(Video::id),cursor=page.next,loading=false,error=null) }
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
            try { mutableDetail.value=catalog.video(video.id) }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { mutableDetailError.value=if(e is CatalogException) e.message else "Metadados indisponíveis." }
        }
    }
    fun selected(id: String): Video = detail.value?.takeIf { it.id == id } ?: library.value.favorites.firstOrNull { it.id == id }
        ?: library.value.history.firstOrNull { it.id == id } ?: (homeState.value.items + searchState.value.items + channelState.value.items).firstOrNull { it.id == id }
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
