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

package org.cryptage.feature.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.cryptage.core.crypto.AgeKeys
import org.cryptage.core.model.KeyEntry
import org.cryptage.core.securestore.KeyEntryRepository

class KeysViewModel(
    private val repository: KeyEntryRepository,
) : ViewModel() {

    enum class ImportError { NO_KEY_MATERIAL, INVALID_IDENTITY, INVALID_RECIPIENT }

    sealed interface Dialog {
        data object None : Dialog
        data object Generate : Dialog
        data class Import(val error: ImportError? = null) : Dialog
        data class Reveal(val entry: KeyEntry) : Dialog
        data class ConfirmDelete(val entry: KeyEntry) : Dialog
    }

    val entries: StateFlow<List<KeyEntry>> = repository.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val mutableDialog = MutableStateFlow<Dialog>(Dialog.None)
    val dialog: StateFlow<Dialog> = mutableDialog.asStateFlow()

    fun showGenerateDialog() {
        mutableDialog.value = Dialog.Generate
    }

    fun showImportDialog() {
        mutableDialog.value = Dialog.Import()
    }

    fun showRevealDialog(entry: KeyEntry) {
        mutableDialog.value = Dialog.Reveal(entry)
    }

    fun showDeleteDialog(entry: KeyEntry) {
        mutableDialog.value = Dialog.ConfirmDelete(entry)
    }

    fun dismissDialog() {
        mutableDialog.value = Dialog.None
    }

    fun generate(name: String) {
        viewModelScope.launch {
            val pair = AgeKeys.generate()
            repository.save(
                KeyEntry(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    recipient = pair.recipient,
                    identity = pair.identity,
                ),
            )
            mutableDialog.value = Dialog.None
        }
    }

    fun import(name: String, identityText: String, recipientText: String) {
        val identity = identityText.trim().ifEmpty { null }
        val recipient = recipientText.trim().ifEmpty { null }
        val error = validate(identity, recipient)
        if (error != null) {
            mutableDialog.value = Dialog.Import(error)
            return
        }
        viewModelScope.launch {
            val resolvedRecipient = recipient ?: identity?.let(AgeKeys::recipientOf)
            repository.save(
                KeyEntry(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    recipient = resolvedRecipient,
                    identity = identity,
                ),
            )
            mutableDialog.value = Dialog.None
        }
    }

    fun delete(entry: KeyEntry) {
        viewModelScope.launch {
            repository.delete(entry.id)
            mutableDialog.value = Dialog.None
        }
    }

    private fun validate(identity: String?, recipient: String?): ImportError? = when {
        identity == null && recipient == null -> ImportError.NO_KEY_MATERIAL
        identity != null && !AgeKeys.isValidIdentity(identity) -> ImportError.INVALID_IDENTITY
        recipient != null && !AgeKeys.isValidRecipient(recipient) -> ImportError.INVALID_RECIPIENT
        else -> null
    }
}
