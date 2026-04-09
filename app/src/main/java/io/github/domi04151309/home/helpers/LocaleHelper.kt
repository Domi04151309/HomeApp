package io.github.domi04151309.home.helpers

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.util.Locale

object LocaleHelper {
    const val PREF_LANGUAGE = "language"
    const val PREF_LANGUAGE_DEFAULT = ""

    private val SUPPORTED_LANGUAGES =
        mapOf(
            "" to "System default",
            "en" to "English",
            "ar" to "العربية",
            "bg" to "Български",
            "cs" to "Čeština",
            "da" to "Dansk",
            "de" to "Deutsch",
            "el" to "Ελληνικά",
            "es" to "Español",
            "et" to "Eesti",
            "fi" to "Suomi",
            "fr" to "Français",
            "hu" to "Magyar",
            "it" to "Italiano",
            "ja" to "日本語",
            "ko" to "한국어",
            "lt" to "Lietuvių",
            "lv" to "Latviešu",
            "nl" to "Nederlands",
            "pl" to "Polski",
            "pt" to "Português",
            "ro" to "Română",
            "ru" to "Русский",
            "sv" to "Svenska",
            "tr" to "Türkçe",
            "vi" to "Tiếng Việt",
            "zh-rCN" to "简体中文",
            "zh-rTW" to "繁體中文",
        )

    fun getLanguageDisplayName(languageCode: String): String =
        SUPPORTED_LANGUAGES[languageCode] ?: SUPPORTED_LANGUAGES[PREF_LANGUAGE_DEFAULT]!!

    fun getSupportedLanguages(): Map<String, String> = SUPPORTED_LANGUAGES

    fun getCurrentLanguage(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_LANGUAGE, PREF_LANGUAGE_DEFAULT) ?: PREF_LANGUAGE_DEFAULT
    }

    /**
     * Wraps the context with the saved locale configuration.
     * This should be called from attachBaseContext() in Application and Activities.
     */
    fun wrapContext(context: Context): Context {
        val language = getCurrentLanguage(context)
        return if (language.isEmpty()) {
            // Use system default
            context
        } else {
            wrapContextWithLocale(context, language)
        }
    }

    /**
     * Sets a new locale and saves it to preferences.
     * Should be called when user changes language in settings.
     */
    fun setNewLocale(
        context: Context,
        language: String,
    ) {
        // Save preference with commit to ensure it's written before restart
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(PREF_LANGUAGE, language)
            commit()
        }
    }

    private fun localeForLanguageCode(language: String): Locale =
        when (language) {
            "zh-rCN" -> Locale("zh", "CN")
            "zh-rTW" -> Locale("zh", "TW")
            else -> Locale(language)
        }

    private fun wrapContextWithLocale(
        context: Context,
        language: String,
    ): Context {
        val locale = localeForLanguageCode(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            config.setLocales(localeList)
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Updates the configuration for an activity.
     * Call this in onCreate() of each activity before setContentView().
     */
    fun updateActivityConfiguration(activity: Activity) {
        val language = getCurrentLanguage(activity)
        if (language.isEmpty()) return

        val locale = localeForLanguageCode(language)
        Locale.setDefault(locale)
        updateConfiguration(activity, locale)
    }

    private fun updateConfiguration(
        activity: Activity,
        locale: Locale,
    ) {
        val resources: Resources = activity.resources
        val config: Configuration = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
            activity.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
