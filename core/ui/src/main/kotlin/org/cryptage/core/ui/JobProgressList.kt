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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cryptage.core.model.JobItem
import org.cryptage.core.model.JobState

@Composable
fun JobProgressList(items: List<JobItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        for (item in items) {
            JobItemRow(item)
        }
    }
}

@Composable
private fun JobItemRow(item: JobItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            JobStateBadge(item.state)
        }
        JobStateDetail(item.state)
    }
}

@Composable
private fun JobStateBadge(state: JobState) {
    when (state) {
        is JobState.Succeeded -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.core_ui_state_done),
            tint = MaterialTheme.colorScheme.primary,
        )
        is JobState.Failed -> Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = stringResource(R.string.core_ui_state_failed),
            tint = MaterialTheme.colorScheme.error,
        )
        else -> Unit
    }
}

@Composable
private fun JobStateDetail(state: JobState) {
    when (state) {
        is JobState.Pending -> Text(
            text = stringResource(R.string.core_ui_state_waiting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is JobState.Running -> RunningIndicator(state)
        is JobState.Succeeded -> Text(
            text = stringResource(R.string.core_ui_state_saved_as, state.outputName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is JobState.Failed -> Text(
            text = jobFailureMessage(state.kind),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun RunningIndicator(state: JobState.Running) {
    val total = state.totalBytes
    if (total != null && total > 0) {
        LinearProgressIndicator(
            progress = { (state.bytesProcessed.toFloat() / total).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    } else {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}
