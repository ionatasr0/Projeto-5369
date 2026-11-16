package dev.projeto5369

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE,sdk=[35])
@OptIn(ExperimentalCoroutinesApi::class)
class AccountSessionTest {
    private class Gateway : DeviceAccountGateway {
        var saved: String?=null
        var calls=0
        var cleared=false
        var result: suspend ((String)->Unit)->String = { code -> code("TEST-CODE"); "Jooj" }
        override suspend fun restore(): String? = saved
        override suspend fun signIn(onCode: (String)->Unit): String { calls++; return result(onCode) }
        override suspend fun signOut() { saved=null; cleared=true }
    }
    @Test fun nativeRestoreUsesAccountStore() = runTest {
        val gateway=Gateway(); gateway.saved="Jooj"
        val session=DeviceAccountSession(this,gateway)
        session.restore(); runCurrent()
        assertEquals(AuthState.SignedIn("Jooj"),session.state.value)
    }
    @Test fun guestRestoreDoesNotInventAnAccount() = runTest {
        val session=DeviceAccountSession(this,Gateway()); session.restore(); runCurrent()
        assertEquals(AuthState.Guest,session.state.value)
    }
    @Test fun deviceCodeIsPresentedBeforeAccountCompletes() = runTest {
        val gateway=Gateway(); val approved=CompletableDeferred<String>()
        gateway.result={ onCode -> onCode("TEST-CODE"); approved.await() }
        val session=DeviceAccountSession(this,gateway); session.signIn(); runCurrent()
        assertEquals(AuthState.DeviceCode("TEST-CODE"),session.state.value)
        approved.complete("Jooj"); runCurrent()
        assertEquals(AuthState.SignedIn("Jooj"),session.state.value)
    }
    @Test fun cancelStopsPendingAuthorizationAndRepeatedTaps() = runTest {
        val gateway=Gateway(); val pending=CompletableDeferred<String>()
        gateway.result={onCode -> onCode("TEST-CODE");pending.await()}
        val session=DeviceAccountSession(this,gateway); session.signIn(); runCurrent(); session.signIn()
        assertEquals(1,gateway.calls)
        session.cancel(); pending.complete("Late"); runCurrent()
        assertEquals(AuthState.Guest,session.state.value)
    }
    @Test fun authorizationExpiresWithoutInfiniteSpinner() = runTest {
        val gateway=Gateway(); gateway.result={ CompletableDeferred<String>().await() }
        val session=DeviceAccountSession(this,gateway); session.signIn(); advanceTimeBy(600_001); runCurrent()
        assertTrue(session.state.value is AuthState.Failure)
    }
    @Test fun errorsAllowFreshLogin() = runTest {
        val gateway=Gateway(); gateway.result={throw IllegalStateException("test")}
        val session=DeviceAccountSession(this,gateway); session.signIn(); runCurrent()
        assertTrue(session.state.value is AuthState.Failure)
        gateway.result={"Jooj"}; session.signIn(); runCurrent()
        assertEquals(AuthState.SignedIn("Jooj"),session.state.value)
    }
    @Test fun logoutClearsStoreAndAllowsNewLogin() = runTest {
        val gateway=Gateway(); gateway.saved="Jooj"
        val session=DeviceAccountSession(this,gateway); session.restore(); runCurrent(); session.signOut(); runCurrent()
        assertTrue(gateway.cleared); assertEquals(AuthState.Guest,session.state.value)
        session.signIn(); runCurrent(); assertTrue(session.state.value is AuthState.SignedIn)
    }
}
