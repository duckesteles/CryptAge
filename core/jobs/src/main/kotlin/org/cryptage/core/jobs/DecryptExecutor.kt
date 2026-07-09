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

import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.cryptage.core.crypto.AgeCryptoException
import org.cryptage.core.crypto.AgeFileKind
import org.cryptage.core.crypto.AgeHeaderProbe
import org.cryptage.core.crypto.AgeStreamCipher
import org.cryptage.core.archive.TarZstdExtractor
import org.cryptage.core.files.OutputNames
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.files.SafStorage

class DecryptExecutor(
    private val storage: SafStorage,
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun execute(
        source: PickedDocument,
        request: DecryptRequest,
        onProgress: (Long, Long?) -> Unit,
    ): String {
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
            decryptFolder(source, request, kind, decryptedName, onProgress)
        } else {
            decryptFile(source, request, kind, decryptedName, onProgress)
        }
    }

    private fun decryptFile(
        source: PickedDocument,
        request: DecryptRequest,
        kind: AgeFileKind,
        decryptedName: String,
        onProgress: (Long, Long?) -> Unit,
    ): String {
        val outputUri = storage.createFileInTree(request.destinationTree, decryptedName)
        try {
            val input = storage.openRead(source.uri)
            val output = storage.openWrite(outputUri)
            runDecrypt(request, kind, input, output) { bytes ->
                onProgress(bytes, source.sizeBytes)
            }
            return decryptedName
        } catch (error: Exception) {
            storage.deleteQuietly(outputUri)
            throw error
        }
    }

    private suspend fun decryptFolder(
        source: PickedDocument,
        request: DecryptRequest,
        kind: AgeFileKind,
        decryptedName: String,
        onProgress: (Long, Long?) -> Unit,
    ): String {
        val folderName = OutputNames.extractedFolderName(decryptedName)
        val directory = storage.createDirectoryInTree(request.destinationTree, folderName)
        try {
            streamDecryptedArchive(source, request, kind, directory, onProgress)
            return folderName
        } catch (error: Exception) {
            storage.deleteQuietly(directory)
            throw error
        }
    }

    private suspend fun streamDecryptedArchive(
        source: PickedDocument,
        request: DecryptRequest,
        kind: AgeFileKind,
        directory: DocumentFile,
        onProgress: (Long, Long?) -> Unit,
    ) {
        coroutineScope {
            val pipeIn = PipedInputStream(PIPE_BUFFER_SIZE)
            val pipeOut = PipedOutputStream(pipeIn)
            val decryptJob = async(ioDispatcher) {
                try {
                    val input = storage.openRead(source.uri)
                    runDecrypt(request, kind, input, pipeOut) { bytes ->
                        onProgress(bytes, source.sizeBytes)
                    }
                } finally {
                    runCatching { pipeOut.close() }
                }
            }
            try {
                TarZstdExtractor.extract(pipeIn, storage.extractionTargetFor(directory)) { }
            } catch (extractError: Exception) {
                runCatching { pipeIn.close() }
                val decryptError = runCatching { decryptJob.await() }.exceptionOrNull()
                throw if (decryptError is AgeCryptoException) decryptError else extractError
            }
            decryptJob.await()
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

    private companion object {
        const val PIPE_BUFFER_SIZE = 256 * 1024
    }
}
