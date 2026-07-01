package com.tokenmonitor.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tokenmonitor.app.data.model.PlatformsData
import kotlinx.serialization.json.Json

class ApiKeyStorage(context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // Fallback to plain SharedPreferences if encryption is unavailable
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, "api_keys_store", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        Log.w("ApiKeyStorage", "EncryptedSharedPreferences unavailable, falling back to plain storage")
        context.getSharedPreferences("api_keys_store_fallback", Context.MODE_PRIVATE)
    }

    fun save(data: PlatformsData) {
        try {
            prefs.edit().putString(KEY_DATA, json.encodeToString(PlatformsData.serializer(), data)).apply()
        } catch (_: Exception) {}
    }

    fun load(): PlatformsData {
        return try {
            val raw = prefs.getString(KEY_DATA, null) ?: return PlatformsData()
            json.decodeFromString(PlatformsData.serializer(), raw)
        } catch (_: Exception) { PlatformsData() }
    }

    fun export(): String = json.encodeToString(PlatformsData.serializer(), load())

    fun import(jsonString: String): PlatformsData {
        val data = json.decodeFromString(PlatformsData.serializer(), jsonString)
        save(data)
        return data
    }

    companion object {
        private const val KEY_DATA = "platforms_data"
    }
}
