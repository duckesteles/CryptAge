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
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import java.io.OutputStream
import org.cryptage.core.archive.ExtractionTarget

class TreeExtractionTarget(
    private val context: Context,
    rootDirectory: DocumentFile,
) : ExtractionTarget {

    private val directories = mutableMapOf("" to rootDirectory)

    override fun createDirectory(relativePath: String) {
        ensureDirectory(relativePath.trimEnd('/'))
    }

    override fun openFile(relativePath: String): OutputStream {
        val parent = ensureDirectory(relativePath.substringBeforeLast('/', ""))
        val fileName = relativePath.substringAfterLast('/')
        val file = parent.createFile(MimeTypes.forName(fileName), fileName)
            ?: throw IOException("cannot create $relativePath")
        return context.contentResolver.openOutputStream(file.uri, "w")
            ?: throw IOException("cannot open $relativePath for writing")
    }

    private fun ensureDirectory(path: String): DocumentFile {
        directories[path]?.let { return it }
        val parent = ensureDirectory(path.substringBeforeLast('/', ""))
        val name = path.substringAfterLast('/')
        val directory = parent.findFile(name)?.takeIf(DocumentFile::isDirectory)
            ?: parent.createDirectory(name)
            ?: throw IOException("cannot create directory $path")
        directories[path] = directory
        return directory
    }
}
