package dev.projeto5369

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dev.projeto5369.core.Library
import java.io.IOException
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val Context.libraryDataStore by preferencesDataStore(name="library")
class LocalStore(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context): this(context.applicationContext.libraryDataStore)
    private val key = stringPreferencesKey("library_v1")
    private val json = Json { ignoreUnknownKeys = true }
    val library: Flow<Library> = dataStore.data.catch { if(it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> prefs[key]?.let { raw -> try { json.decodeFromString<Library>(raw) } catch(_: SerializationException) { Library() } } ?: Library() }
    suspend fun update(transform: (Library) -> Library) {
        dataStore.edit { prefs ->
            val current = prefs[key]?.let { raw -> runCatching { json.decodeFromString<Library>(raw) }.getOrNull() } ?: Library()
            prefs[key] = json.encodeToString(transform(current))
        }
    }
}
