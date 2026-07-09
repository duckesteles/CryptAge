/*
 * This file is part of CryptAge.
 *
 * CryptAge is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * CryptAge is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with CryptAge. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cryptage.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.cryptage.core.model.AppLockSettings
import org.cryptage.core.model.ZstdSettings

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val zstdSettings: Flow<ZstdSettings> = dataStore.data.map { preferences ->
        ZstdSettings(
            enabled = preferences[ZSTD_ENABLED] ?: true,
            level = (preferences[ZSTD_LEVEL] ?: ZstdSettings.DEFAULT_LEVEL)
                .coerceIn(ZstdSettings.MIN_LEVEL, ZstdSettings.MAX_LEVEL),
        )
    }

    val appLockSettings: Flow<AppLockSettings> = dataStore.data.map { preferences ->
        AppLockSettings(
            biometricEnabled = preferences[BIOMETRIC_ENABLED] ?: false,
            pinEnabled = preferences[PIN_ENABLED] ?: false,
        )
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setZstdEnabled(enabled: Boolean) {
        dataStore.edit { it[ZSTD_ENABLED] = enabled }
    }

    suspend fun setZstdLevel(level: Int) {
        dataStore.edit {
            it[ZSTD_LEVEL] = level.coerceIn(ZstdSettings.MIN_LEVEL, ZstdSettings.MAX_LEVEL)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        dataStore.edit { it[PIN_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    private companion object {
        val ZSTD_ENABLED = booleanPreferencesKey("zstd_enabled")
        val ZSTD_LEVEL = intPreferencesKey("zstd_level")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("app_lock_biometric_enabled")
        val PIN_ENABLED = booleanPreferencesKey("app_lock_pin_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
