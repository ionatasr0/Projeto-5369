package dev.projeto5369

/** Account access is independent of the anonymous catalog and local library. */
sealed interface AuthState {
    data object Guest: AuthState
    data class ConfigurationRequired(val message: String): AuthState
    data class SignedIn(val displayName: String): AuthState
}
interface AccountAuth {
    val state: AuthState
    suspend fun signIn(): AuthState
    suspend fun signOut()
}
class OfficialOAuthPending: AccountAuth {
    override val state: AuthState = AuthState.Guest
    override suspend fun signIn(): AuthState = AuthState.ConfigurationRequired(
        "OAuth Google depende de projeto Cloud próprio, consentimento e cliente Android associado ao pacote e certificado."
    )
    override suspend fun signOut() = Unit
}
