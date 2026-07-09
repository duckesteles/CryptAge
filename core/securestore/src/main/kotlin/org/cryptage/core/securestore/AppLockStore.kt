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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppLockStore(private val store: EncryptedPreferenceStore) {

    val pinConfigured: Flow<Boolean> = store.watch(PIN_HASH_KEY).map { it != null }

    suspend fun setPin(pin: String) {
        store.write(PIN_HASH_KEY, PinHasher.hash(pin))
    }

    suspend fun clearPin() {
        store.write(PIN_HASH_KEY, null)
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = store.watch(PIN_HASH_KEY).first() ?: return false
        return PinHasher.verify(pin, stored)
    }

    private companion object {
        const val PIN_HASH_KEY = "app_lock_pin_hash"
    }
}
