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

package org.cryptage.core.model

enum class JobOperation { ENCRYPT, DECRYPT }

enum class JobFailureKind {
    WRONG_KEY,
    WRONG_PASSPHRASE,
    PASSPHRASE_REQUIRED,
    NOT_AGE_FILE,
    CORRUPT_FILE,
    UNREADABLE_INPUT,
    WRITE_FAILED,
    OUT_OF_SPACE,
    UNKNOWN,
}

sealed interface JobState {
    data object Pending : JobState
    data class Running(val bytesProcessed: Long, val totalBytes: Long?) : JobState
    data class Succeeded(val outputName: String) : JobState
    data class Failed(val kind: JobFailureKind) : JobState
}

data class JobItem(
    val id: String,
    val displayName: String,
    val state: JobState,
)

data class BatchState(
    val operation: JobOperation?,
    val items: List<JobItem>,
    val running: Boolean,
) {
    companion object {
        val Idle: BatchState = BatchState(null, emptyList(), false)
    }
}
