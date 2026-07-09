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

package org.cryptage.core.jobs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.cryptage.core.common.DispatcherProvider
import org.cryptage.core.files.PickedDocument
import org.cryptage.core.model.BatchState
import org.cryptage.core.model.JobItem
import org.cryptage.core.model.JobOperation
import org.cryptage.core.model.JobState

class BatchController(
    private val scope: CoroutineScope,
    private val dispatchers: DispatcherProvider,
    private val encryptExecutor: EncryptExecutor,
    private val decryptExecutor: DecryptExecutor,
) {

    private val stateFlow = MutableStateFlow(BatchState.Idle)
    val state: StateFlow<BatchState> = stateFlow.asStateFlow()

    fun startEncrypt(request: EncryptRequest): Boolean =
        start(JobOperation.ENCRYPT, request.sources) { source, onProgress ->
            encryptExecutor.execute(source, request, onProgress)
        }

    fun startDecrypt(request: DecryptRequest): Boolean =
        start(JobOperation.DECRYPT, request.sources) { source, onProgress ->
            decryptExecutor.execute(source, request, onProgress)
        }

    fun clear() {
        stateFlow.update { current ->
            if (current.running) current else BatchState.Idle
        }
    }

    private fun start(
        operation: JobOperation,
        sources: List<PickedDocument>,
        execute: suspend (PickedDocument, (Long, Long?) -> Unit) -> String,
    ): Boolean {
        if (sources.isEmpty() || stateFlow.value.running) return false
        val items = sources.map { JobItem(itemId(it), it.name, JobState.Pending) }
        stateFlow.value = BatchState(operation, items, running = true)
        scope.launch(dispatchers.io) {
            for (source in sources) {
                runItem(source, execute)
            }
            stateFlow.update { it.copy(running = false) }
        }
        return true
    }

    private suspend fun runItem(
        source: PickedDocument,
        execute: suspend (PickedDocument, (Long, Long?) -> Unit) -> String,
    ) {
        val id = itemId(source)
        updateItem(id) { JobState.Running(0, source.sizeBytes) }
        var lastReported = 0L
        try {
            val outputName = execute(source) { bytes, total ->
                if (bytes - lastReported >= PROGRESS_STEP_BYTES) {
                    lastReported = bytes
                    updateItem(id) { JobState.Running(bytes, total) }
                }
            }
            updateItem(id) { JobState.Succeeded(outputName) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            updateItem(id) { JobState.Failed(FailureMapping.map(error)) }
        }
    }

    private fun updateItem(id: String, transform: () -> JobState) {
        stateFlow.update { current ->
            current.copy(
                items = current.items.map { item ->
                    if (item.id == id) item.copy(state = transform()) else item
                },
            )
        }
    }

    private fun itemId(source: PickedDocument): String = source.uri.toString()

    private companion object {
        const val PROGRESS_STEP_BYTES = 512L * 1024L
    }
}
