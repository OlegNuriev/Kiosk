package com.kiosk.app.data.repository
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.kiosk.app.domain.repository.AppPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("kiosk_prefs")
class AppPreferencesRepositoryImpl @Inject constructor(
   @ApplicationContext context: Context
) : AppPreferencesRepository {

    private val dataStore = context.dataStore
    private val KEY_FIRST_LAUNCH = booleanPreferencesKey("first_launch")

    override suspend fun isFirstLaunch(): Boolean {
        return dataStore.data.first()[KEY_FIRST_LAUNCH] ?: true
    }

    override suspend fun setLaunched() {
        dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = false
        }
    }
}