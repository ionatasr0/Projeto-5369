package dev.projeto5369

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.projeto5369.ui.*
import android.os.SystemClock
import android.net.Uri
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.projeto5369.core.*
import kotlinx.coroutines.delay

/** Test-only switches (instrumentation): muted autoplay and the last reported player time. Off in normal use. */
internal object PlayerDebug {
    @Volatile var autoplayMuted = false
    @Volatile var lastTime = -1.0
    @Volatile var host: String? = null
    /** Diagnostics only: "iframeapi" (default) or "embedurl"; software layer; visible probe colours. */
    @Volatile var mode = "iframeapi"
    @Volatile var software = false
    @Volatile var probe = false
    @Volatile var lastLog = ""
}
internal fun embeddedHtml(id: String, autoplayMuted: Boolean = false, host: String? = null, probe: Boolean = false): String {
    require(videoId(id) == id)
    require(host == null || host == "https://www.youtube-nocookie.com")
    // Base URL identifies this Android package per YouTube embedded-player guidance.
    return """<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="referrer" content="strict-origin-when-cross-origin"></head>
        <body style="margin:0;background:${if(probe) "#600" else "#000"}">${if(probe) "<div style='position:absolute;top:0;left:0;color:#fff;font:20px sans-serif;z-index:9'>WEBVIEW DESENHA</div>" else ""}<div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script><script>
        var player; function onYouTubeIframeAPIReady(){player=new YT.Player('player',{${host?.let { "host:'$it'," } ?: ""}
        width:'100%',height:'100%',videoId:'$id',playerVars:{playsinline:1,origin:'https://dev.projeto5369'},
        events:{onReady:function(){var f=document.querySelector('iframe'),r=f?f.getBoundingClientRect():null;P5369.log('ready iframe='+(r?Math.round(r.width)+'x'+Math.round(r.height):'none')+' viewport='+innerWidth+'x'+innerHeight+' body='+document.body.clientWidth+'x'+document.body.clientHeight);P5369.event(5);${if(autoplayMuted) "player.mute();player.playVideo();" else ""}},onStateChange:function(e){P5369.event(e.data);if(e.data==1&&!window.t5369){window.t5369=setInterval(function(){P5369.time(player.getCurrentTime());},1000);}},onError:function(e){P5369.failure(e.data);}}});}
        </script><style>
        html,body{margin:0;padding:0;width:100%;height:100vh;overflow:hidden;background:#000}
        #player,iframe{position:fixed;inset:0;display:block;width:100vw!important;height:100vh!important;border:0}
        </style></body></html>""".trimIndent()
}
private class PlayerBridge(val eventHandler: (Int) -> Unit, val errorHandler: (Int) -> Unit) {
    @JavascriptInterface fun event(value: Int) = eventHandler(value)
    @JavascriptInterface fun failure(value: Int) = errorHandler(value)
    @JavascriptInterface fun time(value: Double) { PlayerDebug.lastTime = value }
    @JavascriptInterface fun log(value: String) {
        if (Regex("ready iframe=[0-9]+x[0-9]+ viewport=[0-9]+x[0-9]+ body=[0-9]+x[0-9]+").matches(value)) {
            PlayerDebug.lastLog = value; Log.d("P5369Player", value)
        }
    }
}
private tailrec fun Context.activity(): Activity? = when(this) { is Activity -> this; is ContextWrapper -> baseContext.activity(); else -> null }

/** Official player fullscreen (its own button): shows the WebView custom view over the activity in landscape. */
private class FullscreenChrome(private val context: Context, private val onChange: (Boolean) -> Unit): WebChromeClient() {
    private var view: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null
    val showing get() = view != null
    override fun onConsoleMessage(message: ConsoleMessage): Boolean {
        // Remote console text can contain URLs or credentials: do not log it.
        return true
    }
    override fun onShowCustomView(v: View, cb: WebChromeClient.CustomViewCallback) {
        val activity = context.activity() ?: return cb.onCustomViewHidden()
        if(view != null) { cb.onCustomViewHidden(); return }
        view = v; callback = cb
        (activity.window.decorView as FrameLayout).addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        v.setBackgroundColor(android.graphics.Color.BLACK)
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars()); systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onChange(true)
    }
    override fun onHideCustomView() {
        val activity = context.activity()
        val v = view ?: return
        view = null
        if(activity != null) {
            (activity.window.decorView as FrameLayout).removeView(v)
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        callback?.onCustomViewHidden(); callback = null
        onChange(false)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayer(video: Video,onPlaying: () -> Unit,modifier: Modifier = Modifier,aspect: Float = 16f/9f,showOpenExternal: Boolean = true) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onPlay by rememberUpdatedState(onPlaying)
    var playback by remember(video.id) { mutableStateOf(PlaybackState.LOADING) }
    var error by remember(video.id) { mutableStateOf<String?>(null) }
    var attempt by remember(video.id) { mutableIntStateOf(0) }
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    var fullscreen by remember { mutableStateOf(false) }
    val active = remember(video.id,attempt,context,lifecycle) { java.util.concurrent.atomic.AtomicBoolean(true) }
    val destroyed = remember(video.id,attempt,context,lifecycle) { java.util.concurrent.atomic.AtomicBoolean(false) }
    val chrome = remember(video.id,attempt,context,lifecycle) { FullscreenChrome(context) { fullscreen = it } }
    val webView = remember(video.id,attempt,context,lifecycle) {
        val started = SystemClock.elapsedRealtime()
        Log.i("P5369Player", "video=${video.id} stage=initialize source=official_iframe attempt=$attempt")
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled=true
            settings.domStorageEnabled=true
            settings.mediaPlaybackRequiresUserGesture=!PlayerDebug.autoplayMuted
            if(PlayerDebug.software) setLayerType(View.LAYER_TYPE_SOFTWARE,null)
            settings.allowFileAccess=false
            settings.allowContentAccess=false
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            // Keep the genuine system WebView user agent, referer and default player ads/controls.
            webChromeClient=chrome
            webViewClient=object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest): Boolean {
                    if(!request.isForMainFrame) return false
                    if(request.url.scheme == "https") runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,request.url)) }
                    return true
                }
                override fun onPageFinished(view: WebView,url: String?) { Log.i("P5369Player","video=${video.id} stage=document_loaded width=${view.width} height=${view.height}") }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if(!active.get()) return
                    Log.w("P5369Player", "video=${video.id} stage=transport http=${response.statusCode} category=http")
                    if(request.isForMainFrame || (request.url.host in setOf("www.youtube.com", "www.youtube-nocookie.com") &&
                            (request.url.path?.startsWith("/embed/") == true || request.url.path == "/iframe_api"))) {
                        playback=PlaybackState.ERROR; error="Falha HTTP ${response.statusCode} ao carregar o player oficial."
                    }
                }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    active.set(false)
                    (view.parent as? android.view.ViewGroup)?.removeView(view)
                    if(destroyed.compareAndSet(false,true)) view.destroy()
                    playback=PlaybackState.ERROR; error="O processo do player foi encerrado. Tente novamente."
                    Log.w("P5369Player", "video=${video.id} stage=renderer category=renderer state=ERROR")
                    return true
                }
                override fun onReceivedError(view: WebView,request: WebResourceRequest,e: WebResourceError) {
                    if(!active.get()) return
                    if(request.isForMainFrame) { Log.w("P5369Player", "video=${video.id} stage=transport category=network code=${e.errorCode}"); playback=PlaybackState.ERROR; error="Falha de rede do player oficial (${e.errorCode})." }
                }
            }
            addJavascriptInterface(PlayerBridge(
                // After onError YouTube still emits -1 (unstarted); keep the real error visible until the user retries.
                { code -> Log.d("P5369Player","state $code"); post { if(!active.get()) return@post; if(playback != PlaybackState.ERROR || code == 1) { playback=nextEmbeddedState(playback, code); if(code == 1) {
                        Log.i("P5369Player", "video=${video.id} stage=playing elapsed_ms=${SystemClock.elapsedRealtime()-started} state=PLAYING")
                        error=null; onPlay()
                    } } } },
                { code -> Log.w("P5369Player","video=${video.id} stage=playback category=platform code=$code"); post { if(!active.get()) return@post; playback=PlaybackState.ERROR; error=when(code) {
                    101,150 -> "O YouTube recusou a reprodução incorporada. Abra no YouTube para verificar a disponibilidade."
                    2 -> "Identificador ou parâmetro de vídeo inválido."
                    5 -> "O player oficial não conseguiu reproduzir o formato neste aparelho."
                    100 -> "Vídeo indisponível ou privado."
                    153 -> "O YouTube recusou a identificação do player incorporado (153)."
                    else -> "O player oficial retornou o erro $code."
                } } }
            ),"P5369")
            if(PlayerDebug.mode == "embedurl") loadUrl("https://www.youtube.com/embed/${video.id}?playsinline=1&origin=https://dev.projeto5369",mapOf("Referer" to "https://dev.projeto5369/"))
            else loadDataWithBaseURL("https://dev.projeto5369/",embeddedHtml(video.id,PlayerDebug.autoplayMuted,PlayerDebug.host,PlayerDebug.probe),"text/html","UTF-8",null)
        }
    }
    BackHandler(enabled = fullscreen) { chrome.onHideCustomView() }
    DisposableEffect(webView,lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(!destroyed.get()) when(event) {
            Lifecycle.Event.ON_PAUSE -> { webView.evaluateJavascript("if(window.player&&player.pauseVideo){player.pauseVideo();}",null); webView.onPause() }
            Lifecycle.Event.ON_RESUME -> webView.onResume()
            else -> Unit
        } }
        lifecycle.addObserver(observer)
        onDispose { active.set(false); Log.i("P5369Player", "video=${video.id} stage=release state=$playback"); if(chrome.showing) chrome.onHideCustomView(); lifecycle.removeObserver(observer); if(destroyed.compareAndSet(false,true)) { webView.stopLoading(); webView.removeJavascriptInterface("P5369"); webView.destroy() } }
    }
    LaunchedEffect(webView,playback,lifecycleState) {
        if(playback == PlaybackState.LOADING && lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
            delay(25_000)
            Log.w("P5369Player", "video=${video.id} stage=timeout category=timeout state=ERROR")
            error="O player não respondeu. Verifique a rede ou abra no YouTube."; playback=PlaybackState.ERROR
        }
    }
    val onda = LocalOnda.current
    Column(modifier,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        // Nothing is drawn over the official iframe: its controls, ads and restrictions stay intact.
        key(webView) {
            AndroidView(factory={webView},modifier=Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(22.dp)).background(Color.Black).testTag("yt-player"))
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(when(playback) { PlaybackState.PLAYING -> Lime; PlaybackState.ERROR -> Danger; else -> onda.muted }))
            Text(modifier=Modifier.testTag("yt-status").weight(1f),style=OndaType.small,color=if(playback == PlaybackState.ERROR) Danger else onda.muted,text=when(playback) {
                PlaybackState.LOADING -> "Carregando player oficial…"
                PlaybackState.READY -> "Toque em reproduzir no player."
                PlaybackState.PLAYING -> "Reproduzindo no YouTube"
                PlaybackState.PAUSED -> "Pausado"
                PlaybackState.ENDED -> "Reprodução encerrada"
                PlaybackState.ERROR -> error ?: "Não foi possível reproduzir."
            })
        }
        if(playback == PlaybackState.ERROR || showOpenExternal) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(playback == PlaybackState.ERROR) Pill("Tentar novamente",{ playback=PlaybackState.LOADING; error=null; attempt++ },primary=true)
            Pill("Abrir no YouTube",{ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/watch?v=${video.id}"))) } },icon=OndaIcons.External)
        }
    }
}

/** Callers must provide a source whose playback they are authorized to request. No YouTube stream extraction. */
data class AuthorizedMediaSource(val httpsUri: String,val authorizationReference: String) {
    init { require(Uri.parse(httpsUri).scheme == "https"); require(authorizationReference.isNotBlank()) }
}
@Composable
fun AuthorizedMediaPlayer(source: AuthorizedMediaSource) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var state by remember(source) { mutableStateOf("Preparando Media3…") }
    var loading by remember(source) { mutableStateOf(true) }
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val player=remember(source) { ExoPlayer.Builder(context).build().apply {
        addListener(object: Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) { loading=playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE; state=when(playbackState) {
                Player.STATE_READY -> "Pronto para reproduzir"; Player.STATE_BUFFERING -> "Carregando…"
                Player.STATE_ENDED -> "Encerrado"; else -> "Preparando…"
            } }
            override fun onRenderedFirstFrame() { Log.i("P5369Player", "stage=first_frame source=authorized_media3") }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { loading=false; state="Falha Media3: ${error.errorCodeName}"; Log.w("P5369Player", "stage=playback source=authorized_media3 category=${error.errorCodeName}") }
        })
        setMediaItem(MediaItem.fromUri(source.httpsUri)); prepare()
    } }
    LaunchedEffect(player,loading,lifecycleState) {
        if(loading && lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
            delay(25_000)
            player.stop(); loading=false; state="A fonte demorou demais para responder. Saia e tente novamente."
        }
    }
    DisposableEffect(player,lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(event == Lifecycle.Event.ON_PAUSE) player.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); player.release() }
    }
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        key(source) { AndroidView(factory={PlayerView(it).apply { this.player=player }},modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(22.dp))) }
        Text(state,style=OndaType.body,color=LocalOnda.current.text)
        Text("Amostra de teste autorizada • Big Buck Bunny (Blender Foundation, CC BY 3.0).",style=OndaType.small,color=LocalOnda.current.muted)
    }
}
