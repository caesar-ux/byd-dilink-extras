package com.byd.dilink.extras.fuelcost.di

import android.content.Context
import androidx.room.Room
import com.byd.dilink.extras.data.dao.ChargeDao
import com.byd.dilink.extras.data.dao.FuelDao
import com.byd.dilink.extras.data.dao.OdometerDao
import com.byd.dilink.extras.data.db.DiLinkExtrasDatabase
import com.byd.dilink.extras.data.repository.FuelCostRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FuelCostModule {

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
    fun provideFuelDao(db: DiLinkExtrasDatabase): FuelDao = db.fuelDao()

    @Provides
    @Singleton
    fun provideChargeDao(db: DiLinkExtrasDatabase): ChargeDao = db.chargeDao()

    @Provides
    @Singleton
    fun provideOdometerDao(db: DiLinkExtrasDatabase): OdometerDao = db.odometerDao()

    @Provides
    @Singleton
    fun provideFuelCostRepository(
        fuelDao: FuelDao,
        chargeDao: ChargeDao,
        odometerDao: OdometerDao
    ): FuelCostRepository {
        return FuelCostRepository(fuelDao, chargeDao, odometerDao)
    }
}
