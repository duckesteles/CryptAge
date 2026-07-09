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

import kage.crypto.x25519.X25519Identity
import kage.crypto.x25519.X25519Recipient

object AgeKeys {

    fun generate(): AgeKeyPair {
        val identity = X25519Identity.new()
        return AgeKeyPair(
            identity = identity.encodeToString(),
            recipient = identity.recipient().encodeToString(),
        )
    }

    fun isValidRecipient(encoded: String): Boolean =
        runCatching { X25519Recipient.decode(encoded.trim()) }.isSuccess

    fun isValidIdentity(encoded: String): Boolean =
        runCatching { X25519Identity.decode(encoded.trim()) }.isSuccess

    fun recipientOf(identity: String): String =
        X25519Identity.decode(identity.trim()).recipient().encodeToString()
}
