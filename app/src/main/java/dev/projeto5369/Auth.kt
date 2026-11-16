package dev.projeto5369

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed interface AuthState {
    data object Guest : AuthState
    data object Busy : AuthState
    data class DeviceCode(val code: String,val verificationUrl: String="https://www.youtube.com/activate") : AuthState
    data class SignedIn(val displayName: String) : AuthState
    data class Failure(val message: String) : AuthState
}
internal interface DeviceAccountGateway {
    val verificationUrl: String get() = "https://www.youtube.com/activate"
    suspend fun restore(): String?
    suspend fun signIn(onCode: (String) -> Unit): String
    suspend fun signOut()
}
/** No OAuth/HTTP response or authorization code is ever included in diagnostics. */
internal class DeviceAccountSession(private val scope: CoroutineScope, private val gateway: DeviceAccountGateway) {
    private val mutable = MutableStateFlow<AuthState>(AuthState.Guest)
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private var restored = false
    fun restore() {
        if(restored) return
        restored=true
        if(job?.isActive == true) return
        val request=++generation
        mutable.value=AuthState.Busy
        job=scope.launch {
            try {
                val name=withTimeout(25_000) { gateway.restore() }
                if(request == generation) mutable.value=name?.let { AuthState.SignedIn(it) } ?: AuthState.Guest
                Log.i("P5369Auth","stage=restore complete=true")
            } catch(e: CancellationException) { if(e !is TimeoutCancellationException) throw e; finish(request,"A restauração demorou demais. Tente entrar novamente.") }
            catch(_: Exception) { finish(request,"Não foi possível restaurar a sessão. Tente entrar novamente.") }
        }
    }
    fun signIn() {
        if(job?.isActive == true) return
        val request=++generation
        mutable.value=AuthState.Busy
        job=scope.launch {
            try {
                val name=withTimeout(10*60_000) {
                    gateway.signIn { code -> if(request == generation) mutable.value=AuthState.DeviceCode(code,gateway.verificationUrl) }
                }
                if(request == generation) mutable.value=AuthState.SignedIn(name)
                Log.i("P5369Auth","stage=authorize result=success")
            } catch(e: CancellationException) { if(e !is TimeoutCancellationException) throw e; finish(request,"O código expirou. Gere outro código para entrar.") }
            catch(_: Exception) { finish(request,"Não foi possível concluir a autorização. Verifique a conexão e gere outro código.") }
        }
    }
    private fun finish(request: Int,message: String) {
        if(request == generation) mutable.value=AuthState.Failure(message)
        Log.w("P5369Auth","stage=authorize result=failed")
    }
    fun cancel() {
        generation++; job?.cancel(); mutable.value=AuthState.Guest
        Log.i("P5369Auth","stage=authorize result=cancelled")
    }
    fun signOut() {
        generation++; job?.cancel(); val request=generation
        mutable.value=AuthState.Busy
        job=scope.launch {
            try {
                withTimeout(25_000) { gateway.signOut() }
                if(request == generation) mutable.value=AuthState.Guest
                Log.i("P5369Auth","stage=logout result=success")
            } catch(e: CancellationException) { if(e !is TimeoutCancellationException) throw e; finish(request,"A limpeza da sessão demorou demais. Tente sair novamente.") }
            catch(_: Exception) { finish(request,"Não foi possível limpar a sessão. Tente sair novamente.") }
        }
    }
}
