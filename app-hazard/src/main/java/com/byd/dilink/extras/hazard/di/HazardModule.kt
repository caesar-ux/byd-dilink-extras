package com.byd.dilink.extras.hazard.di

import android.content.Context
import androidx.room.Room
import com.byd.dilink.extras.data.dao.HazardDao
import com.byd.dilink.extras.data.db.DiLinkExtrasDatabase
import com.byd.dilink.extras.data.repository.HazardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HazardModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiLinkExtrasDatabase {
        return Room.databaseBuilder(
            context,
            DiLinkExtrasDatabase::class.java,
            "dilink_extras.db"
        )
            .addMigrations(DiLinkExtrasDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideHazardDao(db: DiLinkExtrasDatabase): HazardDao = db.hazardDao()

    @Provides
    @Singleton
    fun provideHazardRepository(hazardDao: HazardDao): HazardRepository {
        return HazardRepository(hazardDao)
    }
}
