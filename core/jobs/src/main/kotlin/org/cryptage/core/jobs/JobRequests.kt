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

import android.net.Uri
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.model.EncryptMode
import org.cryptage.core.model.ZstdSettings

data class EncryptRequest(
    val sources: List<PickedDocument>,
    val mode: EncryptMode,
    val destinationTree: Uri,
    val zstd: ZstdSettings,
)

data class DecryptRequest(
    val sources: List<PickedDocument>,
    val identities: List<String>,
    val passphrase: String?,
    val destinationTree: Uri,
)

internal class PassphraseRequiredException : Exception("a passphrase is required for this file")
