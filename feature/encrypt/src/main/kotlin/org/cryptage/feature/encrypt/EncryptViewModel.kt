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

package org.cryptage.feature.encrypt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.cryptage.core.common.DispatcherProvider
import org.cryptage.core.files.OutputNames
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.files.SafStorage
import org.cryptage.core.jobs.BatchController
import org.cryptage.core.jobs.EncryptRequest
import org.cryptage.core.model.BatchState
import org.cryptage.core.model.EncryptMode
import org.cryptage.core.model.KeyEntry
import org.cryptage.core.model.ZstdSettings
import org.cryptage.core.securestore.KeyEntryRepository
import org.cryptage.core.settings.SettingsRepository

class EncryptViewModel(
    private val batchController: BatchController,
    keyEntries: KeyEntryRepository,
    private val settings: SettingsRepository,
    private val storage: SafStorage,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    data class UiState(
        val sources: List<PickedDocument> = emptyList(),
        val keyBased: Boolean = true,
        val selectedRecipientIds: Set<String> = emptySet(),
        val passphrase: String = "",
    )

    data class SavePrompt(val sourceUri: Uri, val suggestedName: String)

    private val mutableState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableState.asStateFlow()

    private val pendingSaveFlow = MutableStateFlow<SavePrompt?>(null)
    val pendingSave: StateFlow<SavePrompt?> = pendingSaveFlow.asStateFlow()

    val recipientEntries: StateFlow<List<KeyEntry>> = keyEntries.entries
        .map { entries -> entries.filter(KeyEntry::hasRecipient) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val batch: StateFlow<BatchState> = batchController.state

    private var queue: ArrayDeque<SavePrompt> = ArrayDeque()
    private val resolvedOutputs = mutableMapOf<String, Uri>()
    private var activeMode: EncryptMode? = null
    private var activeZstd: ZstdSettings = ZstdSettings()

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            val added = uris.map(storage::fileInfo)
            mutableState.update { state ->
                state.copy(sources = (state.sources + added).distinctBy { it.uri })
            }
        }
    }

    fun addFolder(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(dispatchers.io) {
            storage.persistTreePermission(uri)
            val folder = storage.treeInfo(uri)
            mutableState.update { state ->
                state.copy(sources = (state.sources + folder).distinctBy { it.uri })
            }
        }
    }

    fun removeSource(uri: Uri) {
        mutableState.update { state ->
            state.copy(sources = state.sources.filterNot { it.uri == uri })
        }
    }

    fun setKeyBased(keyBased: Boolean) {
        mutableState.update { it.copy(keyBased = keyBased) }
    }

    fun toggleRecipient(id: String) {
        mutableState.update { state ->
            val selected = if (id in state.selectedRecipientIds) {
                state.selectedRecipientIds - id
            } else {
                state.selectedRecipientIds + id
            }
            state.copy(selectedRecipientIds = selected)
        }
    }

    fun setPassphrase(passphrase: String) {
        mutableState.update { it.copy(passphrase = passphrase) }
    }

    fun canStart(state: UiState, batch: BatchState): Boolean {
        if (batch.running || state.sources.isEmpty()) return false
        return if (state.keyBased) {
            state.selectedRecipientIds.isNotEmpty()
        } else {
            state.passphrase.isNotEmpty()
        }
    }

    fun start() {
        val state = mutableState.value
        viewModelScope.launch {
            val mode = resolveMode(state) ?: return@launch
            val zstd = settings.zstdSettings.first()
            activeMode = mode
            activeZstd = zstd
            resolvedOutputs.clear()
            queue = ArrayDeque(
                state.sources.map { source ->
                    SavePrompt(source.uri, suggestedName(source, zstd))
                },
            )
            pendingSaveFlow.value = queue.firstOrNull()
        }
    }

    fun onDestinationChosen(uri: Uri?) {
        val current = pendingSaveFlow.value ?: return
        if (uri != null) {
            resolvedOutputs[current.sourceUri.toString()] = uri
        }
        queue.removeFirstOrNull()
        val next = queue.firstOrNull()
        if (next != null) {
            pendingSaveFlow.value = next
        } else {
            pendingSaveFlow.value = null
            launchBatch()
        }
    }

    private fun launchBatch() {
        val mode = activeMode ?: return
        val state = mutableState.value
        val sources = state.sources.filter { resolvedOutputs.containsKey(it.uri.toString()) }
        if (sources.isEmpty()) return
        batchController.startEncrypt(
            EncryptRequest(
                sources = sources,
                mode = mode,
                outputs = resolvedOutputs.toMap(),
                zstd = activeZstd,
            ),
        )
    }

    private fun resolveMode(state: UiState): EncryptMode? = if (state.keyBased) {
        val recipients = recipientEntries.value
            .filter { it.id in state.selectedRecipientIds }
            .mapNotNull(KeyEntry::recipient)
        if (recipients.isEmpty()) null else EncryptMode.Recipients(recipients)
    } else {
        if (state.passphrase.isEmpty()) null else EncryptMode.Passphrase(state.passphrase)
    }

    private fun suggestedName(source: PickedDocument, zstd: ZstdSettings): String =
        if (source.isDirectory) {
            OutputNames.encryptedFolder(source.name, zstd.enabled)
        } else {
            OutputNames.encryptedFile(source.name)
        }
}
