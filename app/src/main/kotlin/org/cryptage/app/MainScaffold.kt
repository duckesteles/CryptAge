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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import org.cryptage.feature.decrypt.DecryptScreen
import org.cryptage.feature.decrypt.DecryptViewModel
import org.cryptage.feature.encrypt.EncryptScreen
import org.cryptage.feature.encrypt.EncryptViewModel
import org.cryptage.feature.keys.KeysScreen
import org.cryptage.feature.keys.KeysViewModel
import org.cryptage.feature.settings.LicensesScreen
import org.cryptage.feature.settings.SettingsScreen
import org.cryptage.feature.settings.SettingsViewModel

private enum class MainTab { ENCRYPT, DECRYPT }

private enum class SubScreen { SETTINGS, KEYS, LICENSES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScaffold(container: AppContainer) {
    var tabName by rememberSaveable { mutableStateOf(MainTab.ENCRYPT.name) }
    var stackNames by rememberSaveable { mutableStateOf(listOf<String>()) }
    val tab = MainTab.valueOf(tabName)
    val current = stackNames.lastOrNull()?.let(SubScreen::valueOf)

    BackHandler(enabled = current != null) {
        stackNames = stackNames.dropLast(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle(current)) },
                navigationIcon = {
                    if (current != null) {
                        IconButton(onClick = { stackNames = stackNames.dropLast(1) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.app_action_back),
                            )
                        }
                    }
                },
                actions = {
                    if (current == null) {
                        IconButton(onClick = { stackNames = listOf(SubScreen.SETTINGS.name) }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.app_action_settings),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (current == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == MainTab.ENCRYPT,
                        onClick = { tabName = MainTab.ENCRYPT.name },
                        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        label = { Text(stringResource(R.string.app_tab_encrypt)) },
                    )
                    NavigationBarItem(
                        selected = tab == MainTab.DECRYPT,
                        onClick = { tabName = MainTab.DECRYPT.name },
                        icon = { Icon(Icons.Filled.LockOpen, contentDescription = null) },
                        label = { Text(stringResource(R.string.app_tab_decrypt)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (current) {
                SubScreen.SETTINGS -> SettingsScreen(
                    viewModel = settingsViewModel(container),
                    onOpenKeys = { stackNames = stackNames + SubScreen.KEYS.name },
                    onOpenLicenses = { stackNames = stackNames + SubScreen.LICENSES.name },
                )
                SubScreen.KEYS -> KeysScreen(viewModel = keysViewModel(container))
                SubScreen.LICENSES -> LicensesScreen()
                null -> when (tab) {
                    MainTab.ENCRYPT -> EncryptScreen(viewModel = encryptViewModel(container))
                    MainTab.DECRYPT -> DecryptScreen(viewModel = decryptViewModel(container))
                }
            }
        }
    }
}

@Composable
private fun screenTitle(current: SubScreen?): String = stringResource(
    when (current) {
        SubScreen.SETTINGS -> R.string.app_title_settings
        SubScreen.KEYS -> R.string.app_title_keys
        SubScreen.LICENSES -> R.string.app_title_licenses
        null -> R.string.app_name
    },
)

@Composable
private fun encryptViewModel(container: AppContainer): EncryptViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            EncryptViewModel(
                batchController = container.batchController,
                keyEntries = container.keyEntryRepository,
                settings = container.settingsRepository,
                storage = container.safStorage,
                dispatchers = container.dispatchers,
            )
        }
    },
)

@Composable
private fun decryptViewModel(container: AppContainer): DecryptViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            DecryptViewModel(
                batchController = container.batchController,
                keyEntries = container.keyEntryRepository,
                storage = container.safStorage,
                preflight = container.decryptPreflight,
                dispatchers = container.dispatchers,
            )
        }
    },
)

@Composable
private fun keysViewModel(container: AppContainer): KeysViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            KeysViewModel(repository = container.keyEntryRepository)
        }
    },
)

@Composable
private fun settingsViewModel(container: AppContainer): SettingsViewModel = viewModel(
    factory = viewModelFactory {
        initializer {
            SettingsViewModel(
                settings = container.settingsRepository,
                appLockStore = container.appLockStore,
            )
        }
    },
)
