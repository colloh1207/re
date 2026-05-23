package com.sdd.marketplace.core.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sdd_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
        const val DEFAULT_LANGUAGE = "en"

        val SUPPORTED_LANGUAGES = listOf(
            LanguageOption("en", "English", "English", "🇬🇧"),
            LanguageOption("ar", "Arabic", "العربية", "🇸🇦"),
            LanguageOption("zh", "Chinese", "中文", "🇨🇳"),
            LanguageOption("fr", "French", "Français", "🇫🇷"),
            LanguageOption("de", "German", "Deutsch", "🇩🇪"),
            LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳"),
            LanguageOption("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
            LanguageOption("it", "Italian", "Italiano", "🇮🇹"),
            LanguageOption("ja", "Japanese", "日本語", "🇯🇵"),
            LanguageOption("ko", "Korean", "한국어", "🇰🇷"),
            LanguageOption("ms", "Malay", "Bahasa Melayu", "🇲🇾"),
            LanguageOption("pt", "Portuguese", "Português", "🇧🇷"),
            LanguageOption("ru", "Russian", "Русский", "🇷🇺"),
            LanguageOption("es", "Spanish", "Español", "🇪🇸"),
            LanguageOption("sw", "Swahili", "Kiswahili", "🇰🇪"),
            LanguageOption("ta", "Tamil", "தமிழ்", "🇮🇳"),
            LanguageOption("te", "Telugu", "తెలుగు", "🇮🇳"),
            LanguageOption("tr", "Turkish", "Türkçe", "🇹🇷"),
            LanguageOption("ur", "Urdu", "اردو", "🇵🇰"),
            LanguageOption("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
            LanguageOption("ha", "Hausa", "Hausa", "🇳🇬"),
            LanguageOption("yo", "Yoruba", "Yorùbá", "🇳🇬"),
            LanguageOption("ig", "Igbo", "Igbo", "🇳🇬"),
            LanguageOption("bn", "Bengali", "বাংলা", "🇧🇩"),
            LanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳"),
            LanguageOption("gu", "Gujarati", "ગુજરાતી", "🇮🇳"),
            LanguageOption("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳"),
            LanguageOption("ml", "Malayalam", "മലയാളം", "🇮🇳"),
            LanguageOption("mr", "Marathi", "मराठी", "🇮🇳"),
            LanguageOption("am", "Amharic", "አማርኛ", "🇪🇹"),
            LanguageOption("so", "Somali", "Soomaali", "🇸🇴"),
            LanguageOption("tl", "Filipino", "Filipino", "🇵🇭"),
            LanguageOption("nl", "Dutch", "Nederlands", "🇳🇱"),
            LanguageOption("pl", "Polish", "Polski", "🇵🇱"),
            LanguageOption("uk", "Ukrainian", "Українська", "🇺🇦"),
            LanguageOption("th", "Thai", "ภาษาไทย", "🇹🇭")
        )
    }

    fun getSavedLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun saveLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
    }

    fun applyLanguage(context: Context, code: String): Context {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)
