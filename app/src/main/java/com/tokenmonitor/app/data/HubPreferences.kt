package com.tokenmonitor.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hub_settings")

class HubPreferences(private val context: Context) {

    companion object {
        private val KEY_HUB_URL = stringPreferencesKey("hub_url")
        private val KEY_HUB_SECRET = stringPreferencesKey("hub_secret")
    }

    val hubUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_HUB_URL] ?: ""
    }

    val hubSecret: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_HUB_SECRET] ?: ""
    }

    suspend fun saveHubConfig(url: String, secret: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HUB_URL] = url.trimEnd('/')
            prefs[KEY_HUB_SECRET] = secret
        }
    }
}
