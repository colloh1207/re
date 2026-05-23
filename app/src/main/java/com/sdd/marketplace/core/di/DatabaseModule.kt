package com.sdd.marketplace.core.di

import android.content.Context
import androidx.room.Room
import com.sdd.marketplace.data.local.SddDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSddDatabase(@ApplicationContext context: Context): SddDatabase {
        val passphrase = SQLiteDatabase.getBytes("sdd_secure_pass".toCharArray())
        val factory = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context,
            SddDatabase::class.java,
            "sdd_marketplace.db"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideProductDao(db: SddDatabase) = db.productDao()
    @Provides fun provideUserDao(db: SddDatabase) = db.userDao()
    @Provides fun provideChatDao(db: SddDatabase) = db.chatDao()
    @Provides fun provideMessageDao(db: SddDatabase) = db.messageDao()
    @Provides fun provideNotificationDao(db: SddDatabase) = db.notificationDao()
    @Provides fun provideFavoriteDao(db: SddDatabase) = db.favoriteDao()
    @Provides fun provideAppPreferencesDao(db: SddDatabase) = db.appPreferencesDao()
    @Provides fun provideSavedAccountDao(db: SddDatabase) = db.savedAccountDao()
    @Provides fun provideReferralDao(db: SddDatabase) = db.referralDao()
    @Provides fun provideSyncQueueDao(db: SddDatabase) = db.syncQueueDao()
}
