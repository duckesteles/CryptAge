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

import java.io.InputStream
import java.io.OutputStream
import kage.Age
import kage.Identity
import kage.Recipient
import kage.crypto.scrypt.ScryptIdentity
import kage.crypto.scrypt.ScryptRecipient
import kage.crypto.x25519.X25519Identity
import kage.crypto.x25519.X25519Recipient
import kage.errors.CryptoException
import kage.errors.InvalidIdentityException
import kage.errors.ParseException

object AgeStreamCipher {

    private const val BUFFER_SIZE = 64 * 1024

    fun encryptingStreamForRecipients(
        recipients: List<String>,
        destination: OutputStream,
    ): OutputStream = Age.encryptStream(recipients.map(::decodeRecipient), destination)

    fun encryptingStreamForPassphrase(
        passphrase: String,
        destination: OutputStream,
    ): OutputStream =
        Age.encryptStream(listOf(ScryptRecipient(passphrase.toByteArray(Charsets.UTF_8))), destination)

    fun pumpPlaintext(
        source: InputStream,
        encryptingStream: OutputStream,
        onBytesProcessed: (Long) -> Unit,
    ) {
        encryptingStream.use { output ->
            source.use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    total += read
                    onBytesProcessed(total)
                }
            }
        }
    }

    fun decryptWithIdentities(
        identities: List<String>,
        source: InputStream,
        destination: OutputStream,
        onBytesProcessed: (Long) -> Unit,
    ) {
        decrypt(identities.map(::decodeIdentity), source, destination, onBytesProcessed, passphraseMode = false)
    }

    fun decryptWithPassphrase(
        passphrase: String,
        source: InputStream,
        destination: OutputStream,
        onBytesProcessed: (Long) -> Unit,
    ) {
        val identity = ScryptIdentity(passphrase.toByteArray(Charsets.UTF_8))
        decrypt(listOf(identity), source, destination, onBytesProcessed, passphraseMode = true)
    }

    private fun decrypt(
        identities: List<Identity>,
        source: InputStream,
        destination: OutputStream,
        onBytesProcessed: (Long) -> Unit,
        passphraseMode: Boolean,
    ) {
        val countingSource = CountingInputStream(source, onBytesProcessed)
        try {
            Age.decryptStream(identities, countingSource, destination)
        } catch (error: InvalidIdentityException) {
            throw if (passphraseMode) {
                AgeCryptoException.WrongPassphrase(error)
            } else {
                AgeCryptoException.WrongKey(error)
            }
        } catch (error: ParseException) {
            throw AgeCryptoException.NotAgeFile(error)
        } catch (error: CryptoException) {
            throw AgeCryptoException.CorruptFile(error)
        }
    }

    private fun decodeRecipient(encoded: String): Recipient = try {
        X25519Recipient.decode(encoded.trim())
    } catch (error: Exception) {
        throw AgeCryptoException.InvalidKey(error)
    }

    private fun decodeIdentity(encoded: String): Identity = try {
        X25519Identity.decode(encoded.trim())
    } catch (error: Exception) {
        throw AgeCryptoException.InvalidKey(error)
    }
}
