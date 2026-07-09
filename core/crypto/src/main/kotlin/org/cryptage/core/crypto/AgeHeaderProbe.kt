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

import java.io.InputStream
import java.util.Base64

enum class AgeFileKind {
    NOT_AGE,
    KEY_BASED,
    PASSPHRASE,
}

object AgeHeaderProbe {

    private const val VERSION_LINE = "age-encryption.org/v1"
    private const val ARMOR_BEGIN = "-----BEGIN AGE ENCRYPTED FILE-----"
    private const val SCRYPT_STANZA_PREFIX = "-> scrypt"
    private const val STANZA_PREFIX = "->"
    private const val FOOTER_PREFIX = "---"
    private const val HEAD_LIMIT = 8192

    fun probe(source: InputStream): AgeFileKind {
        val head = source.use { readHead(it) }
        val text = String(head, Charsets.ISO_8859_1)
        val headerText = if (text.trimStart().startsWith(ARMOR_BEGIN)) {
            decodeArmoredHead(text) ?: return AgeFileKind.NOT_AGE
        } else {
            text
        }
        return classify(headerText)
    }

    private fun classify(headerText: String): AgeFileKind {
        val lines = headerText.lineSequence().iterator()
        if (!lines.hasNext() || lines.next().trimEnd() != VERSION_LINE) {
            return AgeFileKind.NOT_AGE
        }
        var sawStanza = false
        while (lines.hasNext()) {
            val line = lines.next()
            when {
                line.startsWith(SCRYPT_STANZA_PREFIX) -> return AgeFileKind.PASSPHRASE
                line.startsWith(FOOTER_PREFIX) ->
                    return if (sawStanza) AgeFileKind.KEY_BASED else AgeFileKind.NOT_AGE
                line.startsWith(STANZA_PREFIX) -> sawStanza = true
            }
        }
        return if (sawStanza) AgeFileKind.KEY_BASED else AgeFileKind.NOT_AGE
    }

    private fun decodeArmoredHead(text: String): String? {
        val afterBegin = text.substringAfter(ARMOR_BEGIN)
        val payloadLines = afterBegin.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .takeWhile { !it.startsWith("-----") }
            .toMutableList()
        if (payloadLines.isEmpty()) return null
        val endMarkerSeen = afterBegin.contains("-----END")
        if (!endMarkerSeen) {
            payloadLines.removeAt(payloadLines.lastIndex)
        }
        val joined = payloadLines.joinToString(separator = "")
        val usableLength = joined.length - joined.length % 4
        if (usableLength == 0) return null
        val bytes = try {
            Base64.getDecoder().decode(joined.substring(0, usableLength))
        } catch (_: IllegalArgumentException) {
            return null
        }
        return String(bytes, Charsets.ISO_8859_1)
    }

    private fun readHead(source: InputStream): ByteArray {
        val buffer = ByteArray(HEAD_LIMIT)
        var total = 0
        while (total < HEAD_LIMIT) {
            val read = source.read(buffer, total, HEAD_LIMIT - total)
            if (read == -1) break
            total += read
        }
        return buffer.copyOf(total)
    }
}
