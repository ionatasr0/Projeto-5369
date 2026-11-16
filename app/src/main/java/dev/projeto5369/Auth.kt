package dev.projeto5369

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Identity only. Does not grant YouTube API scopes or authenticate the embedded YouTube player. */
sealed interface AuthState {
    data object Guest : AuthState
    data object Busy : AuthState
    data class ConfigurationRequired(val message: String) : AuthState
    data class SignedIn(val displayName: String) : AuthState
    data class Failure(val message: String) : AuthState
}
internal interface AccountGateway {
    suspend fun signIn(activity: Context, restore: Boolean): String
    suspend fun signOut()
}
internal class GoogleAccountGateway(context: Context, private val clientId: String) : AccountGateway {
    private val manager = CredentialManager.create(context.applicationContext)
    override suspend fun signIn(activity: Context, restore: Boolean): String {
        val request = if (restore) {
            GetCredentialRequest.Builder().addCredentialOption(
                GetGoogleIdOption.Builder().setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(true).setAutoSelectEnabled(true).build()
            ).setPreferImmediatelyAvailableCredentials(true).build()
        } else {
            GetCredentialRequest.Builder().addCredentialOption(
                GetSignInWithGoogleOption.Builder(clientId).build()
            ).build()
        }
        val credential = manager.getCredential(activity, request).credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        // The provider owns credentials. Do not persist/log ID tokens, email or provider response bundles.
        return google.displayName?.takeIf { it.isNotBlank() } ?: "Conta Google"
    }
    override suspend fun signOut() { manager.clearCredentialState(ClearCredentialStateRequest()) }
}

/** Only a non-sensitive opt-in bit is persisted. Restore always asks Google's provider again. */
internal class AccountSession(
    private val scope: CoroutineScope,
    private val gateway: AccountGateway,
    private val configured: Boolean,
    private val restoreAllowed: () -> Boolean,
    private val allowRestore: (Boolean) -> Unit,
) {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.Guest)
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var restored = false
    private var generation = 0
    fun restore(activity: Context) {
        if (restored) return
        restored = true
        if (configured && restoreAllowed()) signIn(activity, true)
        else Log.i("P5369Auth", "stage=restore session=guest")
    }
    fun signIn(activity: Context, restore: Boolean = false) {
        if (job?.isActive == true) return
        if (!configured) {
            mutableState.value = AuthState.ConfigurationRequired("O login Google deste aplicativo ainda não foi configurado pelo desenvolvedor. Você pode continuar como visitante.")
            Log.i("P5369Auth", "stage=authorize error=configuration")
            return
        }
        val request = ++generation
        mutableState.value = AuthState.Busy
        job = scope.launch {
            try {
                Log.i("P5369Auth", "stage=${if (restore) "restore" else "authorize"} session=pending")
                val name = withTimeout(60_000) { gateway.signIn(activity, restore) }
                if (request == generation) {
                    allowRestore(true)
                    mutableState.value = AuthState.SignedIn(name)
                    Log.i("P5369Auth", "stage=complete session=signed_in")
                }
            } catch (_: GetCredentialCancellationException) { finish(request, AuthState.Guest, "cancelled") }
            catch (_: NoCredentialException) {
                finish(request, if (restore) AuthState.Guest else AuthState.Failure("Nenhuma conta disponível. Adicione uma conta Google ao aparelho e tente novamente."), "no_credential")
            } catch (_: GetCredentialProviderConfigurationException) {
                finish(request, AuthState.Failure("O provedor Google não está disponível. Verifique o Google Play Services e a configuração do aplicativo."), "provider_configuration")
            } catch (_: TimeoutCancellationException) {
                finish(request, AuthState.Failure("A autorização demorou demais. Tente novamente."), "timeout")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                finish(request, AuthState.Failure("Não foi possível entrar. Verifique a conexão, o cliente Google e o certificado deste APK."), "authorization")
            }
        }
    }
    private fun finish(request: Int, state: AuthState, category: String) {
        if (request != generation) return
        mutableState.value = state
        Log.i("P5369Auth", "stage=complete session=guest error=$category")
    }
    fun cancel() {
        if (mutableState.value != AuthState.Busy) return
        generation++; job?.cancel(); mutableState.value = AuthState.Guest
        Log.i("P5369Auth", "stage=authorize error=cancelled")
    }
    fun signOut() {
        generation++; job?.cancel(); allowRestore(false); mutableState.value = AuthState.Busy
        job = scope.launch {
            try {
                withTimeout(10_000) { gateway.signOut() }
                mutableState.value = AuthState.Guest
                Log.i("P5369Auth", "stage=logout session=guest")
            } catch (e: CancellationException) {
                if (e !is TimeoutCancellationException) throw e
                mutableState.value = AuthState.Failure("Sessão local encerrada. Não foi possível limpar o estado do provedor Google; tente sair novamente.")
            } catch (_: Exception) {
                mutableState.value = AuthState.Failure("Sessão local encerrada. Não foi possível limpar o estado do provedor Google; tente sair novamente.")
            }
        }
    }
}
