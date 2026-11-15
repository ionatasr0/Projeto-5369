package dev.projeto5369

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Network/device test: an actual decoded video frame and advancing playback, never a mock player. */
@RunWith(AndroidJUnit4::class)
class Media3PlaybackTest {
    @Test fun authorizedSampleRendersAndAdvances() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val rendered=CountDownLatch(1)
            val failure=AtomicReference<PlaybackException?>(null)
            lateinit var player: ExoPlayer
            scenario.onActivity { activity ->
                player=ExoPlayer.Builder(activity).build()
                player.addListener(object: Player.Listener {
                    override fun onRenderedFirstFrame() {rendered.countDown()}
                    override fun onPlayerError(error: PlaybackException) {failure.set(error);rendered.countDown()}
                })
                activity.setContentView(PlayerView(activity).apply {this.player=player})
                player.setMediaItem(MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"))
                player.prepare();player.playWhenReady=true
            }
            try {
                assertTrue("Nenhum frame decodificado em 45 segundos",rendered.await(45,TimeUnit.SECONDS))
                assertNull("Falha na fonte autorizada: ${failure.get()}",failure.get())
                // Emulators can be slow to start the clock: poll up to 10 s instead of a fixed 1.5 s.
                val deadline = SystemClock.uptimeMillis() + 10_000
                var position = 0L
                while (SystemClock.uptimeMillis() < deadline) { scenario.onActivity { position = player.currentPosition }; if (position >= 1_000) break; SystemClock.sleep(250) }
                scenario.onActivity {
                    assertTrue("A posição não avançou",player.currentPosition >= 1_000)
                    assertTrue("O player não está reproduzindo",player.isPlaying)
                    player.pause()
                    assertFalse(player.isPlaying)
                }
            } finally {scenario.onActivity {player.release()}}
        }
    }
}
