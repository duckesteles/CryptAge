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

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.cryptage.core.common.DefaultDispatcherProvider
import org.cryptage.core.common.DispatcherProvider
import org.cryptage.core.files.SafStorage
import org.cryptage.core.files.TreeArchiveEntries
import org.cryptage.core.jobs.BatchController
import org.cryptage.core.jobs.DecryptExecutor
import org.cryptage.core.jobs.DecryptPreflight
import org.cryptage.core.jobs.EncryptExecutor
import org.cryptage.core.securestore.AppLockStore
import org.cryptage.core.securestore.KeyEntryRepository
import org.cryptage.core.securestore.SecureKeyEntryRepository
import org.cryptage.core.securestore.encryptedPreferenceStoreOf
import org.cryptage.core.settings.SettingsRepository
import org.cryptage.core.settings.settingsRepositoryOf

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val dispatchers: DispatcherProvider = DefaultDispatcherProvider()

    private val applicationScope = CoroutineScope(SupervisorJob() + dispatchers.default)

    private val encryptedStore by lazy {
        encryptedPreferenceStoreOf(appContext)
    }

    val keyEntryRepository: KeyEntryRepository by lazy {
        SecureKeyEntryRepository(encryptedStore)
    }

    val appLockStore: AppLockStore by lazy {
        AppLockStore(encryptedStore)
    }

    val settingsRepository: SettingsRepository by lazy {
        settingsRepositoryOf(appContext)
    }

    val safStorage: SafStorage by lazy {
        SafStorage(appContext)
    }

    val decryptPreflight: DecryptPreflight by lazy {
        DecryptPreflight(safStorage, dispatchers)
    }

    val batchController: BatchController by lazy {
        BatchController(
            scope = applicationScope,
            dispatchers = dispatchers,
            encryptExecutor = EncryptExecutor(safStorage, TreeArchiveEntries(appContext)),
            decryptExecutor = DecryptExecutor(safStorage, dispatchers.io),
        )
    }
}
