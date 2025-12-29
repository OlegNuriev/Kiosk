package com.kiosk.app.domain.repository

interface AppPreferencesRepository {
    suspend fun isFirstLaunch(): Boolean
    suspend fun setLaunched()
}