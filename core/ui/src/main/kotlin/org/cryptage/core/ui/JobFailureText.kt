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

package org.cryptage.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cryptage.core.model.JobFailureKind

@Composable
fun jobFailureMessage(kind: JobFailureKind): String = stringResource(
    when (kind) {
        JobFailureKind.WRONG_KEY -> R.string.core_ui_error_wrong_key
        JobFailureKind.WRONG_PASSPHRASE -> R.string.core_ui_error_wrong_passphrase
        JobFailureKind.PASSPHRASE_REQUIRED -> R.string.core_ui_error_passphrase_required
        JobFailureKind.NOT_AGE_FILE -> R.string.core_ui_error_not_age_file
        JobFailureKind.CORRUPT_FILE -> R.string.core_ui_error_corrupt_file
        JobFailureKind.UNREADABLE_INPUT -> R.string.core_ui_error_unreadable_input
        JobFailureKind.WRITE_FAILED -> R.string.core_ui_error_write_failed
        JobFailureKind.OUT_OF_SPACE -> R.string.core_ui_error_out_of_space
        JobFailureKind.UNKNOWN -> R.string.core_ui_error_unknown
    },
)
