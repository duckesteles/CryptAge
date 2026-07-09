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

package org.cryptage.core.jobs

import kotlinx.coroutines.withContext
import org.cryptage.core.common.DispatcherProvider
import org.cryptage.core.crypto.AgeFileKind
import org.cryptage.core.crypto.AgeHeaderProbe
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.files.SafStorage

class DecryptPreflight(
    private val storage: SafStorage,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun anyNeedsPassphrase(sources: List<PickedDocument>): Boolean =
        withContext(dispatchers.io) {
            sources.any { source ->
                runCatching {
                    AgeHeaderProbe.probe(storage.openRead(source.uri)) == AgeFileKind.PASSPHRASE
                }.getOrDefault(false)
            }
        }
}
