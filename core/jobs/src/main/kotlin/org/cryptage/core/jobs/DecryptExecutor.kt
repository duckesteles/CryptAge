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
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread
import org.cryptage.core.archive.TarZstdExtractor
import org.cryptage.core.crypto.AgeCryptoException
import org.cryptage.core.crypto.AgeFileKind
import org.cryptage.core.crypto.AgeHeaderProbe
import org.cryptage.core.crypto.AgeStreamCipher
import org.cryptage.core.files.OutputNames
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.files.SafStorage

class DecryptExecutor(
    private val storage: SafStorage,
) {

    private companion object {
        const val PIPE_BUFFER_SIZE = 256 * 1024
    }

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
        val decryptedName = OutputNames.decrypted(source.name)
        return if (OutputNames.isFolderArchive(decryptedName)) {
            decryptToFolder(source, request, kind, outputUri, decryptedName, onProgress)
        } else {
            decryptToFile(source, request, kind, outputUri, onProgress)
        }
    }

    private fun decryptToFile(
        source: PickedDocument,
        request: DecryptRequest,
        kind: AgeFileKind,
        outputUri: Uri,
        onProgress: (Long, Long?) -> Unit,
    ): String {
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

    private fun decryptToFolder(
        source: PickedDocument,
        request: DecryptRequest,
        kind: AgeFileKind,
        treeUri: Uri,
        decryptedName: String,
        onProgress: (Long, Long?) -> Unit,
    ): String {
        val folderName = OutputNames.extractedFolderName(decryptedName)
        val directory = storage.createDirectoryInTree(treeUri, folderName)
        try {
            val input = storage.openRead(source.uri)
            val pipeIn = PipedInputStream(PIPE_BUFFER_SIZE)
            val pipeOut = PipedOutputStream(pipeIn)
            var decryptError: Exception? = null
            val writer = thread(name = "cryptage-decrypt") {
                try {
                    runDecrypt(request, kind, input, pipeOut) { bytes ->
                        onProgress(bytes, source.sizeBytes)
                    }
                } catch (error: Exception) {
                    decryptError = error
                } finally {
                    runCatching { pipeOut.close() }
                }
            }
            var extractError: Exception? = null
            try {
                TarZstdExtractor.extract(pipeIn, storage.extractionTargetFor(directory)) { }
            } catch (error: Exception) {
                extractError = error
            } finally {
                runCatching { pipeIn.close() }
                writer.join()
            }
            val error = when {
                decryptError is AgeCryptoException -> decryptError
                extractError != null -> extractError
                else -> decryptError
            }
            error?.let { throw it }
            return directory.name ?: folderName
        } catch (error: Exception) {
            storage.deleteQuietly(directory)
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
