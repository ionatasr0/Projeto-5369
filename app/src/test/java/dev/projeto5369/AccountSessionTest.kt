package dev.projeto5369

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountSessionTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private class Gateway : AccountGateway {
        var calls = 0
        var restores = 0
        var cleared = false
        var result: suspend () -> String = { "Jooj" }
        override suspend fun signIn(activity: Context, restore: Boolean): String {
            calls++; if (restore) restores++; return result()
        }
        override suspend fun signOut() { cleared = true }
    }
    @Test fun missingConfigurationNeverContactsGoogle() = runTest {
        val gateway = Gateway()
        val session = AccountSession(this, gateway, false, { false }, {})
        session.signIn(context); runCurrent()
        assertTrue(session.state.value is AuthState.ConfigurationRequired)
        assertEquals(0, gateway.calls)
    }
    @Test fun loginRestoreAndLogoutUseProviderAndPersistOnlyOptIn() = runTest {
        val gateway = Gateway(); var allowed = false
        val session = AccountSession(this, gateway, true, { allowed }, { allowed = it })
        session.signIn(context); runCurrent()
        assertEquals(AuthState.SignedIn("Jooj"), session.state.value); assertTrue(allowed)
        val recreated = AccountSession(this, gateway, true, { allowed }, { allowed = it })
        recreated.restore(context); runCurrent(); recreated.restore(context); runCurrent()
        assertEquals(1, gateway.restores)
        assertEquals(AuthState.SignedIn("Jooj"), recreated.state.value)
        recreated.signOut(); runCurrent()
        assertTrue(gateway.cleared); assertFalse(allowed); assertEquals(AuthState.Guest, recreated.state.value)
        val afterLogout = AccountSession(this, gateway, true, { allowed }, { allowed = it })
        afterLogout.restore(context); runCurrent(); assertEquals(1, gateway.restores)
        afterLogout.signIn(context); runCurrent(); assertTrue(afterLogout.state.value is AuthState.SignedIn)
    }
    @Test fun cancellationAndRepeatedTapsDoNotLeaveBusyState() = runTest {
        val gateway = Gateway(); val response = CompletableDeferred<String>()
        gateway.result = { response.await() }
        val session = AccountSession(this, gateway, true, { false }, {})
        session.signIn(context); runCurrent(); session.signIn(context); runCurrent()
        assertEquals(1, gateway.calls)
        session.cancel(); response.complete("Late result"); runCurrent()
        assertEquals(AuthState.Guest, session.state.value)
    }
    @Test fun providerCancellationIsGuestAndCanRetry() = runTest {
        val gateway = Gateway(); gateway.result = { throw GetCredentialCancellationException() }
        val session = AccountSession(this, gateway, true, { false }, {})
        session.signIn(context); runCurrent(); assertEquals(AuthState.Guest, session.state.value)
        gateway.result = { "Jooj" }; session.signIn(context); runCurrent()
        assertTrue(session.state.value is AuthState.SignedIn)
    }
    @Test fun revokedOrMissingCredentialIsNotRestoredAsSignedIn() = runTest {
        val gateway = Gateway(); gateway.result = { throw NoCredentialException() }
        val session = AccountSession(this, gateway, true, { true }, {})
        session.restore(context); runCurrent(); assertEquals(AuthState.Guest, session.state.value)
        session.signIn(context); runCurrent(); assertTrue(session.state.value is AuthState.Failure)
    }
    @Test fun stalledAuthorizationHasBoundedTimeout() = runTest {
        val gateway = Gateway(); gateway.result = { CompletableDeferred<String>().await() }
        val session = AccountSession(this, gateway, true, { false }, {})
        session.signIn(context); advanceTimeBy(60_001); runCurrent()
        assertTrue(session.state.value is AuthState.Failure)
    }
    @Test fun logoutWinsOverPendingLogin() = runTest {
        val gateway = Gateway(); val response = CompletableDeferred<String>(); var allowed = false
        gateway.result = { response.await() }
        val session = AccountSession(this, gateway, true, { false }, { allowed = it })
        session.signIn(context); runCurrent(); session.signOut(); response.complete("Late result"); runCurrent()
        assertEquals(AuthState.Guest, session.state.value); assertFalse(allowed); assertTrue(gateway.cleared)
    }
}
