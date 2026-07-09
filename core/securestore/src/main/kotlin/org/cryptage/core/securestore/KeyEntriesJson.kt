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

package org.cryptage.core.securestore

import org.cryptage.core.model.KeyEntry
import org.json.JSONArray
import org.json.JSONObject

internal object KeyEntriesJson {

    private const val FIELD_ID = "id"
    private const val FIELD_NAME = "name"
    private const val FIELD_RECIPIENT = "recipient"
    private const val FIELD_IDENTITY = "identity"

    fun encode(entries: List<KeyEntry>): String {
        val array = JSONArray()
        for (entry in entries) {
            val json = JSONObject()
            json.put(FIELD_ID, entry.id)
            json.put(FIELD_NAME, entry.name)
            json.putOpt(FIELD_RECIPIENT, entry.recipient)
            json.putOpt(FIELD_IDENTITY, entry.identity)
            array.put(json)
        }
        return array.toString()
    }

    fun decode(raw: String): List<KeyEntry> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    KeyEntry(
                        id = json.getString(FIELD_ID),
                        name = json.getString(FIELD_NAME),
                        recipient = json.optString(FIELD_RECIPIENT).ifEmpty { null },
                        identity = json.optString(FIELD_IDENTITY).ifEmpty { null },
                    ),
                )
            }
        }
    }
}
