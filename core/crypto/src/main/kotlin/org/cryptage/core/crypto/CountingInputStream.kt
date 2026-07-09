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

package org.cryptage.core.crypto

import java.io.FilterInputStream
import java.io.InputStream

internal class CountingInputStream(
    delegate: InputStream,
    private val onBytesRead: (Long) -> Unit,
) : FilterInputStream(delegate) {

    private var count: Long = 0

    override fun read(): Int {
        val value = super.read()
        if (value != -1) {
            count += 1
            onBytesRead(count)
        }
        return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = super.read(b, off, len)
        if (read > 0) {
            count += read
            onBytesRead(count)
        }
        return read
    }
}
