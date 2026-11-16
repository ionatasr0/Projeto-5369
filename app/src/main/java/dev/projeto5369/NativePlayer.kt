package dev.projeto5369

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.projeto5369.core.Video
import dev.projeto5369.nativeplayer.NativePlaybackFailure
import dev.projeto5369.nativeplayer.NativeSources
import dev.projeto5369.ui.*
import kotlinx.coroutines.*

private tailrec fun Context.hostActivity(): Activity? = when(this) { is Activity -> this; is ContextWrapper -> baseContext.hostActivity(); else -> null }

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun YouTubePlayer(video: Video,onPlaying: () -> Unit,modifier: Modifier=Modifier,aspect: Float=16f/9f,showOpenExternal: Boolean=true,onAccount: (() -> Unit)?=null) {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val onPlay by rememberUpdatedState(onPlaying)
    var attempt by remember(video.id) { mutableIntStateOf(0) }
    var full by remember(video.id) { mutableStateOf(false) }
    var phase by remember(video.id,attempt) { mutableStateOf("Preparando vídeo…") }
    var failure by remember(video.id,attempt) { mutableStateOf<String?>(null) }
    var failureCategory by remember(video.id,attempt) { mutableStateOf<String?>(null) }
    var loading by remember(video.id,attempt) { mutableStateOf(true) }
    var kind by remember(video.id,attempt) { mutableStateOf("native") }
    val began=remember(video.id,attempt) { SystemClock.elapsedRealtime() }
    val player=remember(video.id,attempt) { ExoPlayer.Builder(context.applicationContext).setAudioAttributes(androidx.media3.common.AudioAttributes.DEFAULT,true).setHandleAudioBecomingNoisy(true).build().apply {
        volume=if(PlayerDebug.autoplayMuted) 0f else 1f
        addListener(object: Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                loading=state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if(failure == null) phase=when(state) {
                    Player.STATE_READY -> "Pronto"; Player.STATE_ENDED -> "Reprodução encerrada"
                    Player.STATE_BUFFERING -> "Carregando vídeo…"; else -> "Preparando vídeo…"
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                if(playing) { phase="Reproduzindo"; loading=false; onPlay() }
                else if(playbackState == Player.STATE_READY) phase="Pausado"
            }
            override fun onRenderedFirstFrame() {
                PlayerDebug.firstFrame=true
                Log.i("P5369Player","video=${video.id} stage=first_frame source=$kind elapsed_ms=${SystemClock.elapsedRealtime()-began}")
            }
            override fun onPlayerError(error: PlaybackException) {
                loading=false
                failure="Não foi possível reproduzir esta fonte (${error.errorCodeName})."
                Log.w("P5369Player","video=${video.id} stage=playback category=${error.errorCodeName} state=ERROR")
            }
        })
    } }
    LaunchedEffect(player) {
        Log.i("P5369Player","video=${video.id} stage=resolve source=newtube_native")
        try {
            val resolved=NativeSources.resolve(context,video.id)
            if(failure != null) return@LaunchedEffect
            kind=resolved.kind
            player.setMediaSource(resolved.source); player.prepare(); player.playWhenReady=true
            Log.i("P5369Player","video=${video.id} stage=prepare source=$kind")
        } catch(e: CancellationException) {
            if(e !is TimeoutCancellationException) throw e
            loading=false; failure="O carregamento demorou demais. Tente novamente."
        } catch(e: NativePlaybackFailure) {
            loading=false; failure=e.displayMessage; failureCategory=e.category
            Log.w("P5369Player","video=${video.id} stage=resolve category=${e.category} state=ERROR")
        } catch(_: Exception) { loading=false; failure="Não foi possível preparar o vídeo. Verifique a conexão e tente novamente." }
    }
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(player,loading,lifecycleState) {
        if(loading && lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
            delay(40_000); player.stop(); loading=false; failure="O player não respondeu a tempo. Tente novamente."
        }
    }
    LaunchedEffect(player) {
        while(isActive) { PlayerDebug.lastTime=player.currentPosition/1000.0; delay(1_000) }
    }
    DisposableEffect(player,lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(event == Lifecycle.Event.ON_PAUSE) player.pause() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); player.release(); Log.i("P5369Player","video=${video.id} stage=release source=native") }
    }
    DisposableEffect(full) {
        val activity=context.hostActivity()
        val orientation=activity?.requestedOrientation
        if(full && activity != null) {
            activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.getInsetsController(activity.window,activity.window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { if(full && activity != null) {
            activity.requestedOrientation=orientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowCompat.getInsetsController(activity.window,activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
        } }
    }
    BackHandler(full) { full=false }
    @Composable fun Surface(mod: Modifier) {
        key(player,full) { AndroidView(modifier=mod.background(Color.Black).testTag("yt-player"),factory={
            PlayerView(it).apply { this.player=player; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); setShowNextButton(false); setShowPreviousButton(false)
                setFullscreenButtonClickListener { full=!full }
            }
        },onRelease={ it.player=null }) }
    }
    Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(!full) Surface(Modifier.fillMaxWidth().aspectRatio(aspect))
        Text(failure ?: phase,style=OndaType.small,color=if(failure == null) LocalOnda.current.muted else Danger,
            modifier=Modifier.padding(horizontal=16.dp).testTag("yt-status"))
        if(failure != null) Row(Modifier.padding(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(failureCategory == "authorization" && onAccount != null) Pill("Entrar na conta",onAccount,primary=true)
            Pill("Tentar novamente",{ failure=null; attempt++ },primary=failureCategory != "authorization" || onAccount == null)
            if(showOpenExternal && (failureCategory != "authorization" || onAccount == null)) Pill("Abrir no YouTube",{ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/watch?v=${video.id}"))) } })
        }
    }
    if(full) Dialog(onDismissRequest={full=false},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxSize())
    }
}
