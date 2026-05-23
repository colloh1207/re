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
            // Major global languages
            LanguageOption("en", "English", "English", "🇬🇧"),
            LanguageOption("ar", "Arabic", "العربية", "🇸🇦"),
            LanguageOption("zh", "Chinese (Simplified)", "中文(简体)", "🇨🇳"),
            LanguageOption("zh-TW", "Chinese (Traditional)", "中文(繁體)", "🇹🇼"),
            LanguageOption("fr", "French", "Français", "🇫🇷"),
            LanguageOption("de", "German", "Deutsch", "🇩🇪"),
            LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳"),
            LanguageOption("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
            LanguageOption("it", "Italian", "Italiano", "🇮🇹"),
            LanguageOption("ja", "Japanese", "日本語", "🇯🇵"),
            LanguageOption("ko", "Korean", "한국어", "🇰🇷"),
            LanguageOption("ms", "Malay", "Bahasa Melayu", "🇲🇾"),
            LanguageOption("pt", "Portuguese", "Português", "🇧🇷"),
            LanguageOption("pt-PT", "Portuguese (Portugal)", "Português (Portugal)", "🇵🇹"),
            LanguageOption("ru", "Russian", "Русский", "🇷🇺"),
            LanguageOption("es", "Spanish", "Español", "🇪🇸"),
            LanguageOption("sw", "Swahili", "Kiswahili", "🇰🇪"),
            LanguageOption("tr", "Turkish", "Türkçe", "🇹🇷"),
            LanguageOption("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
            // South Asian
            LanguageOption("bn", "Bengali", "বাংলা", "🇧🇩"),
            LanguageOption("gu", "Gujarati", "ગુજરાતી", "🇮🇳"),
            LanguageOption("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳"),
            LanguageOption("ml", "Malayalam", "മലയാളം", "🇮🇳"),
            LanguageOption("mr", "Marathi", "मराठी", "🇮🇳"),
            LanguageOption("ne", "Nepali", "नेपाली", "🇳🇵"),
            LanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳"),
            LanguageOption("si", "Sinhala", "සිංහල", "🇱🇰"),
            LanguageOption("ta", "Tamil", "தமிழ்", "🇮🇳"),
            LanguageOption("te", "Telugu", "తెలుగు", "🇮🇳"),
            LanguageOption("ur", "Urdu", "اردو", "🇵🇰"),
            // African languages
            LanguageOption("am", "Amharic", "አማርኛ", "🇪🇹"),
            LanguageOption("ha", "Hausa", "Hausa", "🇳🇬"),
            LanguageOption("ig", "Igbo", "Igbo", "🇳🇬"),
            LanguageOption("ln", "Lingala", "Lingála", "🇨🇩"),
            LanguageOption("mg", "Malagasy", "Malagasy", "🇲🇬"),
            LanguageOption("om", "Oromo", "Afaan Oromoo", "🇪🇹"),
            LanguageOption("rw", "Kinyarwanda", "Ikinyarwanda", "🇷🇼"),
            LanguageOption("sn", "Shona", "chiShona", "🇿🇼"),
            LanguageOption("so", "Somali", "Soomaali", "🇸🇴"),
            LanguageOption("st", "Sesotho", "Sesotho", "🇱🇸"),
            LanguageOption("ti", "Tigrinya", "ትግርኛ", "🇪🇷"),
            LanguageOption("tn", "Tswana", "Setswana", "🇧🇼"),
            LanguageOption("xh", "Xhosa", "isiXhosa", "🇿🇦"),
            LanguageOption("yo", "Yoruba", "Yorùbá", "🇳🇬"),
            LanguageOption("zu", "Zulu", "isiZulu", "🇿🇦"),
            // European languages
            LanguageOption("af", "Afrikaans", "Afrikaans", "🇿🇦"),
            LanguageOption("be", "Belarusian", "Беларуская", "🇧🇾"),
            LanguageOption("bg", "Bulgarian", "Български", "🇧🇬"),
            LanguageOption("bs", "Bosnian", "Bosanski", "🇧🇦"),
            LanguageOption("ca", "Catalan", "Català", "🇪🇸"),
            LanguageOption("cs", "Czech", "Čeština", "🇨🇿"),
            LanguageOption("cy", "Welsh", "Cymraeg", "🏴󠁧󠁢󠁷󠁬󠁳󠁿"),
            LanguageOption("da", "Danish", "Dansk", "🇩🇰"),
            LanguageOption("el", "Greek", "Ελληνικά", "🇬🇷"),
            LanguageOption("et", "Estonian", "Eesti", "🇪🇪"),
            LanguageOption("eu", "Basque", "Euskara", "🇪🇸"),
            LanguageOption("fi", "Finnish", "Suomi", "🇫🇮"),
            LanguageOption("ga", "Irish", "Gaeilge", "🇮🇪"),
            LanguageOption("gl", "Galician", "Galego", "🇪🇸"),
            LanguageOption("hr", "Croatian", "Hrvatski", "🇭🇷"),
            LanguageOption("hu", "Hungarian", "Magyar", "🇭🇺"),
            LanguageOption("hy", "Armenian", "Հայերեն", "🇦🇲"),
            LanguageOption("is", "Icelandic", "Íslenska", "🇮🇸"),
            LanguageOption("ka", "Georgian", "ქართული", "🇬🇪"),
            LanguageOption("lb", "Luxembourgish", "Lëtzebuergesch", "🇱🇺"),
            LanguageOption("lt", "Lithuanian", "Lietuvių", "🇱🇹"),
            LanguageOption("lv", "Latvian", "Latviešu", "🇱🇻"),
            LanguageOption("mk", "Macedonian", "Македонски", "🇲🇰"),
            LanguageOption("mt", "Maltese", "Malti", "🇲🇹"),
            LanguageOption("nl", "Dutch", "Nederlands", "🇳🇱"),
            LanguageOption("no", "Norwegian", "Norsk", "🇳🇴"),
            LanguageOption("pl", "Polish", "Polski", "🇵🇱"),
            LanguageOption("ro", "Romanian", "Română", "🇷🇴"),
            LanguageOption("sk", "Slovak", "Slovenčina", "🇸🇰"),
            LanguageOption("sl", "Slovenian", "Slovenščina", "🇸🇮"),
            LanguageOption("sq", "Albanian", "Shqip", "🇦🇱"),
            LanguageOption("sr", "Serbian", "Српски", "🇷🇸"),
            LanguageOption("sv", "Swedish", "Svenska", "🇸🇪"),
            LanguageOption("tl", "Filipino", "Filipino", "🇵🇭"),
            LanguageOption("uk", "Ukrainian", "Українська", "🇺🇦"),
            // Middle East & Central Asia
            LanguageOption("az", "Azerbaijani", "Azərbaycan", "🇦🇿"),
            LanguageOption("fa", "Persian", "فارسی", "🇮🇷"),
            LanguageOption("he", "Hebrew", "עברית", "🇮🇱"),
            LanguageOption("kk", "Kazakh", "Қазақша", "🇰🇿"),
            LanguageOption("ku", "Kurdish", "Kurdî", "🌍"),
            LanguageOption("ky", "Kyrgyz", "Кыргызча", "🇰🇬"),
            LanguageOption("mn", "Mongolian", "Монгол", "🇲🇳"),
            LanguageOption("tg", "Tajik", "Тоҷикӣ", "🇹🇯"),
            LanguageOption("tk", "Turkmen", "Türkmençe", "🇹🇲"),
            LanguageOption("uz", "Uzbek", "Oʻzbek", "🇺🇿"),
            // Southeast Asia & Pacific
            LanguageOption("ceb", "Cebuano", "Cebuano", "🇵🇭"),
            LanguageOption("haw", "Hawaiian", "ʻŌlelo Hawaiʻi", "🇺🇸"),
            LanguageOption("jv", "Javanese", "Basa Jawa", "🇮🇩"),
            LanguageOption("km", "Khmer", "ភាសាខ្មែរ", "🇰🇭"),
            LanguageOption("lo", "Lao", "ລາວ", "🇱🇦"),
            LanguageOption("mi", "Māori", "Te Reo Māori", "🇳🇿"),
            LanguageOption("my", "Burmese", "မြန်မာဘာသာ", "🇲🇲"),
            LanguageOption("su", "Sundanese", "Basa Sunda", "🇮🇩"),
            LanguageOption("th", "Thai", "ภาษาไทย", "🇹🇭"),
            // Americas
            LanguageOption("ay", "Aymara", "Aymar aru", "🇧🇴"),
            LanguageOption("gn", "Guaraní", "Avañe'ẽ", "🇵🇾"),
            LanguageOption("ht", "Haitian Creole", "Kreyòl ayisyen", "🇭🇹"),
            LanguageOption("qu", "Quechua", "Runasimi", "🇵🇪"),
        )
    }

    fun getSavedLanguage(): String = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun saveLanguage(code: String) {
        prefs.edit().putString(KEY_LANGUAGE, code).apply()
    }

    fun applyLanguage(context: Context, code: String): Context {
        val locale = when {
            code.contains("-") -> {
                val parts = code.split("-")
                Locale(parts[0], parts[1])
            }
            else -> Locale(code)
        }
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
