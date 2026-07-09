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

package org.cryptage.core.archive

import com.github.luben.zstd.ZstdOutputStream
import java.io.OutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream

object TarZstdArchiver {

    const val TAR_SUFFIX: String = ".tar"
    const val TAR_ZSTD_SUFFIX: String = ".tar.zst"

    private const val BUFFER_SIZE = 64 * 1024

    fun archive(
        entries: Sequence<ArchiveEntrySource>,
        destination: OutputStream,
        zstdLevel: Int?,
        onBytesProcessed: (Long) -> Unit,
    ) {
        val sink = if (zstdLevel != null) ZstdOutputStream(destination, zstdLevel) else destination
        TarArchiveOutputStream(sink).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            var processed = 0L
            for (entry in entries) {
                processed = writeEntry(tar, entry, processed, onBytesProcessed)
            }
        }
    }

    private fun writeEntry(
        tar: TarArchiveOutputStream,
        entry: ArchiveEntrySource,
        processedSoFar: Long,
        onBytesProcessed: (Long) -> Unit,
    ): Long {
        var processed = processedSoFar
        val name = if (entry.isDirectory) {
            entry.relativePath.trimEnd('/') + "/"
        } else {
            entry.relativePath
        }
        val tarEntry = TarArchiveEntry(name)
        if (!entry.isDirectory) {
            tarEntry.size = entry.sizeBytes
        }
        tar.putArchiveEntry(tarEntry)
        val open = entry.open
        if (!entry.isDirectory && open != null) {
            open().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    tar.write(buffer, 0, read)
                    processed += read
                    onBytesProcessed(processed)
                }
            }
        }
        tar.closeArchiveEntry()
        return processed
    }
}
