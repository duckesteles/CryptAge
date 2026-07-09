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

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EncryptedPreferenceStore(
    private val dataStore: DataStore<Preferences>,
    private val aead: Aead,
) {

    fun watch(key: String): Flow<String?> {
        val preferenceKey = stringPreferencesKey(key)
        return dataStore.data.map { preferences ->
            preferences[preferenceKey]?.let { decrypt(it, key) }
        }
    }

    suspend fun write(key: String, value: String?) {
        val preferenceKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(preferenceKey)
            } else {
                preferences[preferenceKey] = encrypt(value, key)
            }
        }
    }

    private fun encrypt(plaintext: String, key: String): String {
        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String, key: String): String {
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        val plaintext = aead.decrypt(ciphertext, key.toByteArray(Charsets.UTF_8))
        return String(plaintext, Charsets.UTF_8)
    }
}
