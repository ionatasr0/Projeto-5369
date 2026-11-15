package dev.projeto5369.core

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Identifies the app honestly. It must NOT contain "Android"/"Mobile": YouTube answers such agents with a
 * 302 to m.youtube.com (verified live in CI, 15/11/2026), which this client treats as blocked, so search failed.
 */
const val USER_AGENT = "OndaTube/0.2 (Projeto5369; anonymous public catalog)"

/** Anonymous public WEB session only. No embedded keys, cookies, tokens or impersonated TV clients. */
class YouTubeCatalog(
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(25,TimeUnit.SECONDS)
        .connectTimeout(10,TimeUnit.SECONDS).followRedirects(false).build(),
    private val base: HttpUrl = "https://www.youtube.com/".toHttpUrl(),
) : Catalog {
    private suspend fun request(path: String, query: Map<String,String> = emptyMap(), body: JsonObject? = null): String {
        val url = base.newBuilder().addPathSegments(path).apply { query.forEach { (k,v) -> addQueryParameter(k,v) } }.build()
        val req = Request.Builder().url(url).header("User-Agent",USER_AGENT)
            .header("Accept-Language","pt-BR,pt;q=0.9")
        if(body != null) req.post(body.toString().toRequestBody("application/json".toMediaType()))
        return suspendCancellableCoroutine { cont ->
            val call = client.newCall(req.build())
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object: Callback {
                override fun onFailure(call: Call,e: IOException) {
                    if(cont.isActive) cont.resumeWithException(CatalogException(CatalogException.Kind.NETWORK,"Sem conexão com o YouTube. Verifique sua rede e tente novamente."))
                }
                override fun onResponse(call: Call,response: Response) {
                    try {
                        val result = response.use {
                            when(it.code) {
                                429 -> throw CatalogException(CatalogException.Kind.RATE_LIMIT,"O YouTube limitou as consultas. Aguarde antes de tentar novamente.")
                                301,302,303,307,308,401,403 -> throw CatalogException(CatalogException.Kind.BLOCKED,"O YouTube exige consentimento ou autorização. Abra no YouTube para continuar.")
                            }
                            if(!it.isSuccessful) throw CatalogException(CatalogException.Kind.UNAVAILABLE,"YouTube indisponível (HTTP ${it.code}).")
                            val source = it.body?.source() ?: throw YouTubeParser.schema()
                            // Bound allocation on mobile; no unlimited HTML/JSON response buffering.
                            val limit = 8L * 1024 * 1024
                            source.request(limit + 1)
                            if(source.buffer.size > limit) throw CatalogException(CatalogException.Kind.SCHEMA,"Resposta do catálogo excedeu o limite de memória.")
                            source.readUtf8()
                        }
                        if(cont.isActive) cont.resume(result)
                    } catch(e: Exception) { if(cont.isActive) cont.resumeWithException(e) }
                }
            })
        }
    }
    override suspend fun search(query: String): Page<Video> = withContext(Dispatchers.Default) {
        require(query.isNotBlank())
        val html = request("results",mapOf("search_query" to query,"hl" to "pt","gl" to "BR"))
        YouTubeParser.page(YouTubeParser.initial(html),"search",YouTubeParser.context(html))
    }
    override suspend fun shorts(query: String): Page<Video> = withContext(Dispatchers.Default) {
        require(query.isNotBlank())
        val q = if(query.contains("#shorts",ignoreCase=true)) query else "$query #shorts"
        val html = request("results",mapOf("search_query" to q,"hl" to "pt","gl" to "BR"))
        YouTubeParser.shortsPage(YouTubeParser.initial(html),"shorts",YouTubeParser.context(html))
    }
    /** Test diagnostics only (schema report). */
    internal suspend fun rawForDiagnostics(path: String, query: Map<String,String>): String = request(path,query)
    override suspend fun next(cursor: Cursor): Page<Video> = withContext(Dispatchers.Default) {
        require(cursor.endpoint in setOf("search","browse","shorts"))
        val context = YouTubeParser.parse(cursor.context)
        // Only a context received from the current public WEB page can be continued.
        val clientContext = context["client"] as? JsonObject
        if(clientContext?.let { YouTubeParser.string(it,"clientName") } != "WEB") throw CatalogException(CatalogException.Kind.BLOCKED,"Contexto público WEB indisponível para paginação.")
        val api = if(cursor.endpoint == "shorts") "search" else cursor.endpoint
        val response = request("youtubei/v1/$api",body=buildJsonObject {
            put("context",context); put("continuation",cursor.token)
        })
        val root = YouTubeParser.parse(response)
        val page = if(cursor.endpoint == "shorts") YouTubeParser.shortsPage(root,"shorts",context) else YouTubeParser.page(root,cursor.endpoint,context)
        if(page.next?.token == cursor.token) throw YouTubeParser.schema()
        page
    }
    override suspend fun video(id: String): Video = withContext(Dispatchers.Default) {
        require(videoId(id) == id)
        YouTubeParser.details(request("watch",mapOf("v" to id,"hl" to "pt")),id)
    }
    override suspend fun channel(id: String): ChannelPage = withContext(Dispatchers.Default) {
        require(Regex("UC[A-Za-z0-9_-]{22}").matches(id))
        val html = request("channel/$id/videos",mapOf("hl" to "pt","gl" to "BR"))
        YouTubeParser.channel(YouTubeParser.initial(html),id,YouTubeParser.context(html))
    }
}
