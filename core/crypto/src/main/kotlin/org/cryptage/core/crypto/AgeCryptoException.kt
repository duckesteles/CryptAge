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

package org.cryptage.core.crypto

sealed class AgeCryptoException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class WrongKey(cause: Throwable? = null) :
        AgeCryptoException("no provided identity matches the file", cause)

    class WrongPassphrase(cause: Throwable? = null) :
        AgeCryptoException("the passphrase does not match the file", cause)

    class NotAgeFile(cause: Throwable? = null) :
        AgeCryptoException("the input is not an age encrypted file", cause)

    class CorruptFile(cause: Throwable? = null) :
        AgeCryptoException("the age file is damaged or truncated", cause)

    class InvalidKey(cause: Throwable? = null) :
        AgeCryptoException("the provided key material is invalid", cause)
}
