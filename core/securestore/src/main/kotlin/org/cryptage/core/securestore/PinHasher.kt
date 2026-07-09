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
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PinHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val SEPARATOR = ":"

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        val derived = derive(pin, salt)
        return encode(salt) + SEPARATOR + encode(derived)
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) return false
        val salt = decode(parts[0]) ?: return false
        val expected = decode(parts[1]) ?: return false
        return MessageDigest.isEqual(derive(pin, salt), expected)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(encoded: String): ByteArray? =
        runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
}
