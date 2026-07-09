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

import java.io.IOException
import org.cryptage.core.crypto.AgeCryptoException
import org.cryptage.core.model.JobFailureKind

internal object FailureMapping {

    fun map(error: Throwable): JobFailureKind = when (error) {
        is PassphraseRequiredException -> JobFailureKind.PASSPHRASE_REQUIRED
        is AgeCryptoException.WrongKey -> JobFailureKind.WRONG_KEY
        is AgeCryptoException.WrongPassphrase -> JobFailureKind.WRONG_PASSPHRASE
        is AgeCryptoException.NotAgeFile -> JobFailureKind.NOT_AGE_FILE
        is AgeCryptoException.CorruptFile -> JobFailureKind.CORRUPT_FILE
        is AgeCryptoException.InvalidKey -> JobFailureKind.WRONG_KEY
        is IOException -> mapIo(error)
        else -> JobFailureKind.UNKNOWN
    }

    private fun mapIo(error: IOException): JobFailureKind {
        val message = generateSequence<Throwable>(error) { it.cause }
            .mapNotNull { it.message }
            .joinToString(separator = " ")
        return when {
            message.contains("ENOSPC", ignoreCase = true) -> JobFailureKind.OUT_OF_SPACE
            message.contains("no space", ignoreCase = true) -> JobFailureKind.OUT_OF_SPACE
            message.contains("reading", ignoreCase = true) -> JobFailureKind.UNREADABLE_INPUT
            message.contains("writing", ignoreCase = true) -> JobFailureKind.WRITE_FAILED
            message.contains("create", ignoreCase = true) -> JobFailureKind.WRITE_FAILED
            else -> JobFailureKind.UNKNOWN
        }
    }
}
