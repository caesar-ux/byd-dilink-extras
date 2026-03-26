package com.byd.dilink.extras.prayer.di

import android.content.Context
import androidx.room.Room
import com.byd.dilink.extras.data.dao.TasbeehDao
import com.byd.dilink.extras.data.db.DiLinkExtrasDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrayerModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiLinkExtrasDatabase {
        return Room.databaseBuilder(
            context,
            DiLinkExtrasDatabase::class.java,
            "dilink_extras.db"
        ).build()
    }

    @Provides
    fun provideTasbeehDao(db: DiLinkExtrasDatabase): TasbeehDao {
        return db.tasbeehDao()
    }
}
