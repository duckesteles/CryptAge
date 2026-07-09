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

object OutputNames {

    const val AGE_SUFFIX: String = ".age"
    const val DECRYPTED_SUFFIX: String = ".decrypted"
    const val TAR_SUFFIX: String = ".tar"
    const val TAR_ZSTD_SUFFIX: String = ".tar.zst"

    fun encryptedFile(inputName: String): String = inputName + AGE_SUFFIX

    fun encryptedFolder(folderName: String, compressed: Boolean): String {
        val archiveSuffix = if (compressed) TAR_ZSTD_SUFFIX else TAR_SUFFIX
        return folderName + archiveSuffix + AGE_SUFFIX
    }

    fun decrypted(inputName: String): String =
        if (inputName.endsWith(AGE_SUFFIX)) {
            inputName.dropLast(AGE_SUFFIX.length)
        } else {
            inputName + DECRYPTED_SUFFIX
        }

    fun isFolderArchive(decryptedName: String): Boolean =
        decryptedName.endsWith(TAR_ZSTD_SUFFIX) || decryptedName.endsWith(TAR_SUFFIX)

    fun extractedFolderName(decryptedName: String): String = when {
        decryptedName.endsWith(TAR_ZSTD_SUFFIX) -> decryptedName.dropLast(TAR_ZSTD_SUFFIX.length)
        decryptedName.endsWith(TAR_SUFFIX) -> decryptedName.dropLast(TAR_SUFFIX.length)
        else -> decryptedName
    }
}
