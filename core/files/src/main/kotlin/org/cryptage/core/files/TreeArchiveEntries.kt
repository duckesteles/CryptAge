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

package org.cryptage.core.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import org.cryptage.core.archive.ArchiveEntrySource

class TreeArchiveEntries(private val context: Context) {

    fun entries(treeUri: Uri): Sequence<ArchiveEntrySource> {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("cannot access folder")
        return sequence { yieldChildren(root, "") }
    }

    fun totalBytes(treeUri: Uri): Long {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("cannot access folder")
        return sumBytes(root)
    }

    private suspend fun SequenceScope<ArchiveEntrySource>.yieldChildren(
        directory: DocumentFile,
        prefix: String,
    ) {
        for (child in directory.listFiles()) {
            val name = child.name ?: continue
            val path = prefix + name
            if (child.isDirectory) {
                yield(ArchiveEntrySource(path, isDirectory = true, sizeBytes = 0))
                yieldChildren(child, "$path/")
            } else {
                val uri = child.uri
                yield(
                    ArchiveEntrySource(path, isDirectory = false, sizeBytes = child.length()) {
                        context.contentResolver.openInputStream(uri)
                            ?: throw IOException("cannot open $path for reading")
                    },
                )
            }
        }
    }

    private fun sumBytes(directory: DocumentFile): Long =
        directory.listFiles().sumOf { child ->
            if (child.isDirectory) sumBytes(child) else child.length()
        }
}
