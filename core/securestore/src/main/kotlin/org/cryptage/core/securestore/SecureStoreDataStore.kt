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

package org.cryptage.core.securestore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.secureStoreDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "securestore",
)

internal fun secureStoreDataStoreOf(context: Context): DataStore<Preferences> =
    context.secureStoreDataStore

fun encryptedPreferenceStoreOf(context: Context): EncryptedPreferenceStore =
    EncryptedPreferenceStore(secureStoreDataStoreOf(context), SecureStoreAead.create(context))
