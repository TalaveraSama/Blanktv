package app.blanktv.tv.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "blanktv_settings")

/**
 * User preferences, persisted with DataStore. Kept deliberately tiny: anything that is really
 * library data belongs in Room, not here.
 */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    val darkTheme: Flow<Boolean> = store.data.map { it[KEY_DARK_THEME] ?: true }

    suspend fun setDarkTheme(dark: Boolean) {
        store.edit { prefs -> prefs[KEY_DARK_THEME] = dark }
    }

    private companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
