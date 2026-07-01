package com.tokenmonitor.app.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

class LocaleManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("locale", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOCALE = "app_locale"
        const val LOCALE_EN = "en"
        const val LOCALE_ZH = "zh"

        fun wrap(context: Context, localeTag: String): Context {
            val locale = Locale.forLanguageTag(localeTag)
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }

        fun getSavedLocale(context: Context): String {
            return context.getSharedPreferences("locale", Context.MODE_PRIVATE)
                .getString(KEY_LOCALE, "") ?: ""
        }
    }

    fun setLocale(localeTag: String) {
        prefs.edit { putString(KEY_LOCALE, localeTag) }
    }
}
