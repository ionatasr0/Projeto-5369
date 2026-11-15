package dev.projeto5369.core

import kotlin.test.*

class EmbeddedStateTest {
    @Test fun errorSurvivesLateUnstartedAndReadyCallbacks() {
        assertEquals(PlaybackState.ERROR, nextEmbeddedState(PlaybackState.ERROR, -1))
        assertEquals(PlaybackState.ERROR, nextEmbeddedState(PlaybackState.ERROR, 5))
        assertEquals(PlaybackState.PLAYING, nextEmbeddedState(PlaybackState.ERROR, 1))
    }
    @Test fun waitingForGestureDoesNotStartLoadingTimeoutAgain() {
        assertEquals(PlaybackState.READY, nextEmbeddedState(PlaybackState.READY, -1))
        assertEquals(PlaybackState.LOADING, nextEmbeddedState(PlaybackState.READY, 3))
        assertEquals(PlaybackState.PLAYING, nextEmbeddedState(PlaybackState.LOADING, 1))
        assertEquals(PlaybackState.PAUSED, nextEmbeddedState(PlaybackState.PLAYING, 2))
        assertEquals(PlaybackState.ENDED, nextEmbeddedState(PlaybackState.PLAYING, 0))
    }
    @Test fun unknownCallbacksDoNotBreakPlayback() {
        assertEquals(PlaybackState.PLAYING, nextEmbeddedState(PlaybackState.PLAYING, 999))
    }
}
