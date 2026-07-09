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

package org.cryptage.feature.decrypt

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
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.files.SafStorage
import org.cryptage.core.jobs.BatchController
import org.cryptage.core.jobs.DecryptPreflight
import org.cryptage.core.jobs.DecryptRequest
import org.cryptage.core.model.BatchState
import org.cryptage.core.model.KeyEntry
import org.cryptage.core.securestore.KeyEntryRepository

class DecryptViewModel(
    private val batchController: BatchController,
    keyEntries: KeyEntryRepository,
    private val storage: SafStorage,
    private val preflight: DecryptPreflight,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    data class UiState(
        val sources: List<PickedDocument> = emptyList(),
        val destination: Uri? = null,
        val destinationName: String? = null,
        val askPassphrase: Boolean = false,
    )

    private val mutableState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableState.asStateFlow()

    private val identities: StateFlow<List<String>> = keyEntries.entries
        .map { entries -> entries.mapNotNull(KeyEntry::identity) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val batch: StateFlow<BatchState> = batchController.state

    fun addFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            val added = uris.map(storage::fileInfo)
            mutableState.update { state ->
                state.copy(sources = (state.sources + added).distinctBy { it.uri })
            }
        }
    }

    fun removeSource(uri: Uri) {
        mutableState.update { state ->
            state.copy(sources = state.sources.filterNot { it.uri == uri })
        }
    }

    fun setDestination(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(dispatchers.io) {
            storage.persistTreePermission(uri)
            val name = storage.treeInfo(uri).name
            mutableState.update { it.copy(destination = uri, destinationName = name) }
        }
    }

    fun canStart(state: UiState, batch: BatchState): Boolean =
        !batch.running && state.sources.isNotEmpty() && state.destination != null

    fun start() {
        viewModelScope.launch {
            val state = mutableState.value
            if (state.destination == null || state.sources.isEmpty()) return@launch
            if (preflight.anyNeedsPassphrase(state.sources)) {
                mutableState.update { it.copy(askPassphrase = true) }
            } else {
                launchBatch(passphrase = null)
            }
        }
    }

    fun startWithPassphrase(passphrase: String) {
        mutableState.update { it.copy(askPassphrase = false) }
        viewModelScope.launch { launchBatch(passphrase) }
    }

    fun dismissPassphrase() {
        mutableState.update { it.copy(askPassphrase = false) }
    }

    private suspend fun launchBatch(passphrase: String?) {
        val state = mutableState.value
        val destination = state.destination ?: return
        batchController.startDecrypt(
            DecryptRequest(
                sources = state.sources,
                identities = identities.first(),
                passphrase = passphrase,
                destinationTree = destination,
            ),
        )
    }
}
