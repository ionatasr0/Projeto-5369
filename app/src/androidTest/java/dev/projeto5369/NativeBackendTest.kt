package dev.projeto5369

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.projeto5369.nativeplayer.EncryptedNativeContext
import dev.projeto5369.nativeplayer.NewTubeAccountGateway
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeBackendTest {
    @Test fun nativeSessionPreferencesAreEncryptedAtRest() {
        val base=InstrumentationRegistry.getInstrumentation().targetContext
        val secure=EncryptedNativeContext(base).getSharedPreferences("test_probe",0)
        val value="local-test-value-not-a-credential"
        try {
            assertTrue(secure.edit().putString("test_key",value).commit())
            assertEquals(value,secure.getString("test_key",null))
            val raw=base.getSharedPreferences("newtube_secure_test_probe",0)
            assertFalse(raw.all.toString().contains(value))
            assertFalse(raw.all.containsKey("test_key"))
        } finally { secure.edit().clear().commit() }
    }
    /** Requests a real device code, never exports it, and cancels before account approval. */
    @Test fun realDeviceAuthorizationCanIssueCodeAndBeCancelled() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("ytLive") == "true")
        val gateway=NewTubeAccountGateway(InstrumentationRegistry.getInstrumentation().targetContext)
        val received=CompletableDeferred<Unit>()
        val job=launch { gateway.signIn { if(it.isNotBlank()) received.complete(Unit) } }
        try { withTimeout(45_000) { received.await() }; assertTrue(received.isCompleted) }
        finally { job.cancelAndJoin() }
    }
}
