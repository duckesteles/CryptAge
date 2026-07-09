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
import java.io.InputStream
import java.io.OutputStream
import org.cryptage.core.crypto.AgeCryptoException
import org.cryptage.core.crypto.AgeFileKind
import org.cryptage.core.crypto.AgeHeaderProbe
import org.cryptage.core.crypto.AgeStreamCipher
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.files.SafStorage

class DecryptExecutor(
    private val storage: SafStorage,
) {

    fun execute(
        source: PickedDocument,
        request: DecryptRequest,
        onProgress: (Long, Long?) -> Unit,
    ): String {
        val outputUri = request.outputs[source.uri.toString()] ?: throw MissingDestinationException()
        val kind = AgeHeaderProbe.probe(storage.openRead(source.uri))
        when (kind) {
            AgeFileKind.NOT_AGE -> throw AgeCryptoException.NotAgeFile()
            AgeFileKind.PASSPHRASE ->
                if (request.passphrase == null) throw PassphraseRequiredException()
            AgeFileKind.KEY_BASED ->
                if (request.identities.isEmpty()) throw AgeCryptoException.WrongKey()
        }
        try {
            val input = storage.openRead(source.uri)
            val output = storage.openWrite(outputUri)
            runDecrypt(request, kind, input, output) { bytes ->
                onProgress(bytes, source.sizeBytes)
            }
            return storage.fileInfo(outputUri).name
        } catch (error: Exception) {
            storage.deleteQuietly(outputUri)
            throw error
        }
    }

    private fun runDecrypt(
        request: DecryptRequest,
        kind: AgeFileKind,
        input: InputStream,
        output: OutputStream,
        onBytesProcessed: (Long) -> Unit,
    ) {
        if (kind == AgeFileKind.PASSPHRASE) {
            val passphrase = request.passphrase ?: throw PassphraseRequiredException()
            AgeStreamCipher.decryptWithPassphrase(passphrase, input, output, onBytesProcessed)
        } else {
            AgeStreamCipher.decryptWithIdentities(request.identities, input, output, onBytesProcessed)
        }
    }
}
