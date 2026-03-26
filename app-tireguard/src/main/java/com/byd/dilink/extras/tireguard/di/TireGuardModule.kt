package com.byd.dilink.extras.tireguard.di

import android.content.Context
import androidx.room.Room
import com.byd.dilink.extras.data.dao.*
import com.byd.dilink.extras.data.db.DiLinkExtrasDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TireGuardModule {

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
    fun provideTirePressureDao(db: DiLinkExtrasDatabase): TirePressureDao {
        return db.tirePressureDao()
    }

    @Provides
    fun provideBatteryDao(db: DiLinkExtrasDatabase): BatteryDao {
        return db.batteryDao()
    }

    @Provides
    fun provideTireRotationDao(db: DiLinkExtrasDatabase): TireRotationDao {
        return db.tireRotationDao()
    }

    @Provides
    fun provideTasbeehDao(db: DiLinkExtrasDatabase): TasbeehDao {
        return db.tasbeehDao()
    }
}
