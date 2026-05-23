package com.sdd.marketplace

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class SddApplication : Application(), WorkConfig.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("sdd_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("selected_language", "en") ?: "en"
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        val localeContext = base.createConfigurationContext(config)
        super.attachBaseContext(localeContext)
    }

    override val workManagerConfiguration: WorkConfig
        get() = WorkConfig.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
