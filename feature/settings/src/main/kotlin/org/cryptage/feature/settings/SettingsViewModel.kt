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

package org.cryptage.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.cryptage.core.model.AppLockSettings
import org.cryptage.core.model.ZstdSettings
import org.cryptage.core.securestore.AppLockStore
import org.cryptage.core.settings.SettingsRepository

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val appLockStore: AppLockStore,
) : ViewModel() {

    val zstd: StateFlow<ZstdSettings> = settings.zstdSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ZstdSettings())

    val appLock: StateFlow<AppLockSettings> = settings.appLockSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLockSettings())

    private val mutableShowSetPin = MutableStateFlow(false)
    val showSetPin: StateFlow<Boolean> = mutableShowSetPin.asStateFlow()

    fun setZstdEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setZstdEnabled(enabled) }
    }

    fun setZstdLevel(level: Int) {
        viewModelScope.launch { settings.setZstdLevel(level) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setBiometricEnabled(enabled) }
    }

    fun requestEnablePin() {
        mutableShowSetPin.value = true
    }

    fun confirmPin(pin: String) {
        viewModelScope.launch {
            appLockStore.setPin(pin)
            settings.setPinEnabled(true)
            mutableShowSetPin.value = false
        }
    }

    fun dismissSetPin() {
        mutableShowSetPin.value = false
    }

    fun disablePin() {
        viewModelScope.launch {
            appLockStore.clearPin()
            settings.setPinEnabled(false)
        }
    }
}
