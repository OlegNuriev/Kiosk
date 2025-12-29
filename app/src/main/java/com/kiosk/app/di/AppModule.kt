package com.kiosk.app.di

import com.kiosk.app.data.repository.AppPreferencesRepositoryImpl
import com.kiosk.app.domain.repository.AppPreferencesRepository
import com.kiosk.app.presentation.KioskController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(
        impl: AppPreferencesRepositoryImpl
    ): AppPreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object KioskModule {
    @Provides
    @Singleton
    fun provideKioskController(): KioskController = KioskController()
}
