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
import java.io.InputStream
import java.io.OutputStream

class SafStorage(private val context: Context) {

    fun openRead(uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri)
            ?: throw IOException("cannot open document for reading")

    fun openWrite(uri: Uri): OutputStream =
        context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("cannot open document for writing")

    fun createFileInTree(treeUri: Uri, displayName: String): Uri {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("cannot access destination directory")
        val file = tree.createFile(MimeTypes.forName(displayName), displayName)
            ?: throw IOException("cannot create file in destination directory")
        return file.uri
    }

    fun createDirectoryInTree(treeUri: Uri, displayName: String): DocumentFile {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("cannot access destination directory")
        return tree.findFile(displayName)?.takeIf(DocumentFile::isDirectory)
            ?: tree.createDirectory(displayName)
            ?: throw IOException("cannot create directory in destination")
    }

    fun fileInfo(uri: Uri): PickedDocument {
        val document = DocumentFile.fromSingleUri(context, uri)
        val name = document?.name ?: uri.lastPathSegment ?: "file"
        val size = document?.length()?.takeIf { it > 0 }
        return PickedDocument(uri, name, size, isDirectory = false)
    }

    fun treeInfo(uri: Uri): PickedDocument {
        val document = DocumentFile.fromTreeUri(context, uri)
        val name = document?.name ?: uri.lastPathSegment ?: "folder"
        return PickedDocument(uri, name, sizeBytes = null, isDirectory = true)
    }

    fun persistTreePermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }

    fun deleteQuietly(uri: Uri) {
        runCatching { DocumentFile.fromSingleUri(context, uri)?.delete() }
    }

    fun deleteQuietly(document: DocumentFile) {
        runCatching { document.delete() }
    }

    fun extractionTargetFor(directory: DocumentFile): TreeExtractionTarget =
        TreeExtractionTarget(context, directory)
}
