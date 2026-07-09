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

package org.cryptage.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.cryptage.core.crypto.AgeKeys
import org.cryptage.core.model.KeyEntry
import org.cryptage.core.securestore.AppLockStore
import org.cryptage.core.securestore.KeyEntryRepository
import org.cryptage.core.settings.SettingsRepository

class OnboardingViewModel(
    private val keyEntries: KeyEntryRepository,
    private val settings: SettingsRepository,
    private val appLockStore: AppLockStore,
) : ViewModel() {

    enum class Step { WELCOME, BACKUP, APP_LOCK }

    data class UiState(
        val step: Step = Step.WELCOME,
        val recipient: String? = null,
        val identity: String? = null,
        val biometricEnabled: Boolean = false,
        val pinEnabled: Boolean = false,
        val showSetPin: Boolean = false,
    )

    private val mutableState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableState.asStateFlow()

    fun generateKey(keyName: String) {
        viewModelScope.launch {
            val pair = AgeKeys.generate()
            keyEntries.save(
                KeyEntry(
                    id = UUID.randomUUID().toString(),
                    name = keyName,
                    recipient = pair.recipient,
                    identity = pair.identity,
                ),
            )
            mutableState.update {
                it.copy(step = Step.BACKUP, recipient = pair.recipient, identity = pair.identity)
            }
        }
    }

    fun continueToAppLock() {
        mutableState.update { it.copy(step = Step.APP_LOCK) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setBiometricEnabled(enabled)
            mutableState.update { it.copy(biometricEnabled = enabled) }
        }
    }

    fun requestEnablePin() {
        mutableState.update { it.copy(showSetPin = true) }
    }

    fun confirmPin(pin: String) {
        viewModelScope.launch {
            appLockStore.setPin(pin)
            settings.setPinEnabled(true)
            mutableState.update { it.copy(pinEnabled = true, showSetPin = false) }
        }
    }

    fun dismissSetPin() {
        mutableState.update { it.copy(showSetPin = false) }
    }

    fun disablePin() {
        viewModelScope.launch {
            appLockStore.clearPin()
            settings.setPinEnabled(false)
            mutableState.update { it.copy(pinEnabled = false) }
        }
    }

    fun finish() {
        viewModelScope.launch {
            settings.setOnboardingCompleted()
        }
    }
}
