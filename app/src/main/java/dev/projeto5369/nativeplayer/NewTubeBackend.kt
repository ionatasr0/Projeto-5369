package dev.projeto5369.nativeplayer

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import com.liskovsoft.youtubeapi.service.YouTubeSignInService
import dev.projeto5369.DeviceAccountGateway
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** All native-core preference files are encrypted; the context is application-only. */
internal class EncryptedNativeContext(base: Context) : ContextWrapper(base.applicationContext) {
    private val stores=mutableMapOf<String,SharedPreferences>()
    private val master by lazy { MasterKey.Builder(baseContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build() }
    override fun getApplicationContext(): Context = this
    @Synchronized override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = stores.getOrPut(name) {
        EncryptedSharedPreferences.create(baseContext,"newtube_secure_$name",master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }
}
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object NewTubeBackend {
    @Volatile private var ready=false
    @Synchronized fun initialize(context: Context) {
        if(ready) return
        androidx.media3.common.util.Log.setLogLevel(androidx.media3.common.util.Log.LOG_LEVEL_OFF)
        val signIn=YouTubeSignInService.instance()
        GlobalPreferences.instance(EncryptedNativeContext(context))
        val deadline=SystemClock.elapsedRealtime()+10_000
        while(!signIn.isAccountStoreLoaded && SystemClock.elapsedRealtime()<deadline) Thread.sleep(20)
        check(signIn.isAccountStoreLoaded) { "Account storage initialization failed" }
        ready=true
    }
    fun service() = YouTubeServiceManager.instance()
}
internal class NewTubeAccountGateway(private val context: Context) : DeviceAccountGateway {
    override val verificationUrl: String get() = com.liskovsoft.youtubeapi.auth.V2.OndaDevicePolicy.getVerificationUrl()
        ?: throw IllegalStateException("Missing verification URL")
    override suspend fun restore(): String? = withContext(Dispatchers.IO) {
        NewTubeBackend.initialize(context)
        NewTubeBackend.service().signInService.selectedAccount?.name
    }
    override suspend fun signIn(onCode: (String) -> Unit): String {
        withContext(Dispatchers.IO) { NewTubeBackend.initialize(context) }
        return suspendCancellableCoroutine { cont ->
            val subscription=AtomicReference<Disposable?>()
            cont.invokeOnCancellation { subscription.get()?.dispose() }
            val disposable=NewTubeBackend.service().signInService.signInObserve().subscribe(
                { code -> if(cont.isActive) onCode(code) },
                { _ -> if(cont.isActive) cont.resumeWithException(IllegalStateException("Device authorization failed")) },
                {
                    val account=NewTubeBackend.service().signInService.selectedAccount
                    if(cont.isActive) {
                        if(account != null && !account.isEmpty) cont.resume(account.name?.takeIf { it.isNotBlank() } ?: "Conta YouTube")
                        else cont.resumeWithException(IllegalStateException("Account was not persisted"))
                    }
                })
            subscription.set(disposable)
            if(!cont.isActive) disposable.dispose()
        }
    }
    override suspend fun signOut() = withContext(Dispatchers.IO) {
        NewTubeBackend.initialize(context)
        val service=NewTubeBackend.service().signInService
        service.accounts?.toList()?.forEach(service::removeAccount)
        check(service.selectedAccount == null)
    }
}
