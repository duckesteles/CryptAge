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
import java.io.OutputStream
import org.cryptage.core.archive.TarZstdArchiver
import org.cryptage.core.crypto.AgeStreamCipher
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.files.SafStorage
import org.cryptage.core.files.TreeArchiveEntries
import org.cryptage.core.model.EncryptMode

class EncryptExecutor(
    private val storage: SafStorage,
    private val treeEntries: TreeArchiveEntries,
) {

    fun execute(
        source: PickedDocument,
        request: EncryptRequest,
        onProgress: (Long, Long?) -> Unit,
    ): String {
        val outputUri = request.outputs[source.uri.toString()] ?: throw MissingDestinationException()
        return if (source.isDirectory) {
            encryptFolder(source, request, outputUri, onProgress)
        } else {
            encryptFile(source, request, outputUri, onProgress)
        }
    }

    private fun encryptFile(
        source: PickedDocument,
        request: EncryptRequest,
        outputUri: Uri,
        onProgress: (Long, Long?) -> Unit,
    ): String {
        try {
            val input = storage.openRead(source.uri)
            val encrypting = encryptingStream(request.mode, storage.openWrite(outputUri))
            AgeStreamCipher.pumpPlaintext(input, encrypting) { bytes ->
                onProgress(bytes, source.sizeBytes)
            }
            return storage.fileInfo(outputUri).name
        } catch (error: Exception) {
            storage.deleteQuietly(outputUri)
            throw error
        }
    }

    private fun encryptFolder(
        source: PickedDocument,
        request: EncryptRequest,
        outputUri: Uri,
        onProgress: (Long, Long?) -> Unit,
    ): String {
        val compressed = request.zstd.enabled
        val totalBytes = runCatching { treeEntries.totalBytes(source.uri) }.getOrNull()
        try {
            val encrypting = encryptingStream(request.mode, storage.openWrite(outputUri))
            TarZstdArchiver.archive(
                entries = treeEntries.entries(source.uri),
                destination = encrypting,
                zstdLevel = if (compressed) request.zstd.level else null,
            ) { bytes ->
                onProgress(bytes, totalBytes)
            }
            return storage.fileInfo(outputUri).name
        } catch (error: Exception) {
            storage.deleteQuietly(outputUri)
            throw error
        }
    }

    private fun encryptingStream(mode: EncryptMode, destination: OutputStream): OutputStream =
        when (mode) {
            is EncryptMode.Recipients ->
                AgeStreamCipher.encryptingStreamForRecipients(mode.recipients, destination)
            is EncryptMode.Passphrase ->
                AgeStreamCipher.encryptingStreamForPassphrase(mode.passphrase, destination)
        }
}
