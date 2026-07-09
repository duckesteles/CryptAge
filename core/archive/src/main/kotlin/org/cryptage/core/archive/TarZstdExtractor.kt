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

import com.github.luben.zstd.ZstdInputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

object TarZstdExtractor {

    private const val BUFFER_SIZE = 64 * 1024
    private val ZSTD_MAGIC = byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte())

    fun extract(
        source: InputStream,
        target: ExtractionTarget,
        onBytesProcessed: (Long) -> Unit,
    ) {
        val buffered = BufferedInputStream(source, BUFFER_SIZE)
        val decompressed = if (startsWithZstdMagic(buffered)) ZstdInputStream(buffered) else buffered
        TarArchiveInputStream(decompressed).use { tar ->
            var processed = 0L
            while (true) {
                val entry = tar.nextEntry ?: break
                val path = sanitizedPath(entry.name)
                if (entry.isDirectory) {
                    target.createDirectory(path)
                } else {
                    target.openFile(path).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = tar.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            processed += read
                            onBytesProcessed(processed)
                        }
                    }
                }
            }
        }
    }

    private fun startsWithZstdMagic(source: BufferedInputStream): Boolean {
        source.mark(ZSTD_MAGIC.size)
        val head = ByteArray(ZSTD_MAGIC.size)
        var total = 0
        while (total < head.size) {
            val read = source.read(head, total, head.size - total)
            if (read == -1) break
            total += read
        }
        source.reset()
        return total == ZSTD_MAGIC.size && head.contentEquals(ZSTD_MAGIC)
    }

    private fun sanitizedPath(rawName: String): String {
        val segments = rawName.replace('\\', '/').split('/').filter { it.isNotEmpty() }
        if (segments.isEmpty() || segments.any { it == "." || it == ".." }) {
            throw IOException("unsafe archive entry path: $rawName")
        }
        return segments.joinToString(separator = "/")
    }
}
