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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.cryptage.core.model.KeyEntry

interface KeyEntryRepository {
    val entries: Flow<List<KeyEntry>>
    suspend fun save(entry: KeyEntry)
    suspend fun delete(id: String)
}

class SecureKeyEntryRepository(
    private val store: EncryptedPreferenceStore,
) : KeyEntryRepository {

    private val mutex = Mutex()

    override val entries: Flow<List<KeyEntry>> =
        store.watch(ENTRIES_KEY).map { raw -> raw?.let(KeyEntriesJson::decode).orEmpty() }

    override suspend fun save(entry: KeyEntry) {
        mutate { current -> current.filterNot { it.id == entry.id } + entry }
    }

    override suspend fun delete(id: String) {
        mutate { current -> current.filterNot { it.id == id } }
    }

    private suspend fun mutate(transform: (List<KeyEntry>) -> List<KeyEntry>) {
        mutex.withLock {
            val current = entries.first()
            store.write(ENTRIES_KEY, KeyEntriesJson.encode(transform(current)))
        }
    }

    private companion object {
        const val ENTRIES_KEY = "key_entries"
    }
}
