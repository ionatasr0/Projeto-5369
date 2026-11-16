package dev.projeto5369.nativeplayer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import com.newtube.sabr.SabrMediaSource
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import io.reactivex.rxjava3.disposables.Disposable
import okhttp3.OkHttpClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NativePlaybackFailure(val category: String, val displayMessage: String) : Exception(displayMessage)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
data class NativeSource(val kind: String, val source: MediaSource)

/** Native NewTube source adapter. No account headers are copied into media requests. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object NativeSources {
    private const val MEDIA_UA="Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
    private val mediaClient=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS)
        .callTimeout(25,TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(true).followSslRedirects(false).build()
    private suspend fun formats(id: String): MediaItemFormatInfo = suspendCancellableCoroutine { cont ->
        val reference=AtomicReference<Disposable?>()
        cont.invokeOnCancellation { reference.get()?.dispose() }
        val disposable=NewTubeBackend.service().mediaItemService.getLatestFormatInfoObserve(id).firstOrError().subscribe(
            { info -> if(cont.isActive) cont.resume(info) },
            { _ -> if(cont.isActive) cont.resumeWithException(NativePlaybackFailure("metadata","O YouTube não disponibilizou uma fonte para este vídeo. Tente novamente.")) })
        reference.set(disposable)
        if(!cont.isActive) disposable.dispose()
    }
    suspend fun resolve(context: Context,id: String): NativeSource {
        require(dev.projeto5369.core.videoId(id) == id)
        withContext(Dispatchers.IO) { NewTubeBackend.initialize(context) }
        val info=withTimeout(35_000) { formats(id) }
        return withContext(Dispatchers.IO) {
            if(info.isBotCheckRequired || info.isUnplayable) throw NativePlaybackFailure("authorization","O YouTube não autorizou este vídeo. Confira sua conta e a disponibilidade do conteúdo.")
            if(!info.containsMedia()) throw NativePlaybackFailure("unavailable","Vídeo indisponível para reprodução nativa.")
            // The host has no native YouTube ad renderer. Never silently discard an announced ad.
            if(info.prerollWaitMs > 0) throw NativePlaybackFailure("ads","Este vídeo exige anúncios que o player nativo ainda não reproduz. Abra no YouTube para assistir.")
            val http=OkHttpDataSource.Factory(mediaClient).setUserAgent(MEDIA_UA)
            val data=DefaultDataSource.Factory(context.applicationContext,http)
            val item=MediaItem.Builder().setMediaId(id)
            when {
                SabrFormatAdapter.eligible(info) -> NativeSource("SABR",SabrMediaSource(SabrFormatAdapter.adapt(info),OkHttpDataSource.Factory(mediaClient.newBuilder().followRedirects(false).build()).setUserAgent(MEDIA_UA),DefaultBandwidthMeter.getSingletonInstance(context)))
                info.isLive && info.containsHlsUrl() -> NativeSource("HLS-live",DefaultMediaSourceFactory(data).createMediaSource(item.setUri(info.hlsManifestUrl).setMimeType(MimeTypes.APPLICATION_M3U8).build()))
                info.isLive && info.containsDashUrl() -> NativeSource("DASH-live",DefaultMediaSourceFactory(data).createMediaSource(item.setUri(info.dashManifestUrl).setMimeType(MimeTypes.APPLICATION_MPD).build()))
                info.containsDashFormats() -> {
                    if(info.isLive) throw NativePlaybackFailure("live","Esta transmissão não forneceu um manifesto remoto compatível.")
                    val manifest=info.createMpdStream()?.use { NativeDashParser().parse(Uri.parse("https://www.youtube.com/generated.mpd"),it) }
                        ?: throw NativePlaybackFailure("manifest","Manifesto de reprodução indisponível.")
                    NativeSource("DASH",DashMediaSource.Factory(data).createMediaSource(manifest,item.setUri("https://www.youtube.com/generated.mpd").setMimeType(MimeTypes.APPLICATION_MPD).build()))
                }
                info.containsHlsUrl() -> NativeSource("HLS",DefaultMediaSourceFactory(data).createMediaSource(item.setUri(info.hlsManifestUrl).setMimeType(MimeTypes.APPLICATION_M3U8).build()))
                info.containsDashUrl() -> NativeSource("DASH",DefaultMediaSourceFactory(data).createMediaSource(item.setUri(info.dashManifestUrl).setMimeType(MimeTypes.APPLICATION_MPD).build()))
                info.containsUrlFormats() -> {
                    val url=info.createUrlList()?.firstOrNull() ?: throw NativePlaybackFailure("format","Nenhuma faixa compatível foi encontrada.")
                    NativeSource("progressive",DefaultMediaSourceFactory(data).createMediaSource(item.setUri(url).build()))
                }
                else -> throw NativePlaybackFailure("format","Este vídeo não possui uma fonte compatível com o player nativo.")
            }
        }
    }
}
