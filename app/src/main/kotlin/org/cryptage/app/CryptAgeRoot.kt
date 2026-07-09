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

package org.cryptage.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.cryptage.core.model.AppLockSettings
import org.cryptage.feature.applock.AppLockScreen
import org.cryptage.feature.onboarding.OnboardingScreen
import org.cryptage.feature.onboarding.OnboardingViewModel

@Composable
fun CryptAgeRoot(container: AppContainer) {
    val onboardingCompleted by container.settingsRepository.onboardingCompleted
        .collectAsStateWithLifecycle(initialValue = null)
    val appLock by container.settingsRepository.appLockSettings
        .collectAsStateWithLifecycle(initialValue = null)
    var unlocked by rememberSaveable { mutableStateOf(false) }

    val completed = onboardingCompleted
    val lock: AppLockSettings? = appLock

    when {
        completed == null || lock == null -> LoadingScreen()
        !completed -> OnboardingScreen(
            viewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        OnboardingViewModel(
                            keyEntries = container.keyEntryRepository,
                            settings = container.settingsRepository,
                            appLockStore = container.appLockStore,
                        )
                    }
                },
            ),
        )
        lock.enabled && !unlocked -> AppLockScreen(
            biometricEnabled = lock.biometricEnabled,
            pinEnabled = lock.pinEnabled,
            verifyPin = container.appLockStore::verifyPin,
            onUnlocked = { unlocked = true },
        )
        else -> MainScaffold(container)
    }
}

@Composable
private fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}
