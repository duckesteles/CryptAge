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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cryptage.core.model.JobOperation
import org.cryptage.core.ui.JobProgressList
import org.cryptage.core.ui.PassphraseDialog

@Composable
fun DecryptScreen(viewModel: DecryptViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val batch by viewModel.batch.collectAsStateWithLifecycle()

    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.addFiles(uris) }
    val destinationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> viewModel.setDestination(uri) }

    if (state.askPassphrase) {
        PassphraseDialog(
            title = stringResource(R.string.decrypt_passphrase_title),
            confirmLabel = stringResource(R.string.decrypt_start),
            onConfirm = viewModel::startWithPassphrase,
            onDismiss = viewModel::dismissPassphrase,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.decrypt_section_input))
        OutlinedButton(onClick = { filesLauncher.launch(arrayOf("*/*")) }) {
            Icon(Icons.Filled.NoteAdd, contentDescription = null)
            Text(
                text = stringResource(R.string.decrypt_add_files),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        for (source in state.sources) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                IconButton(onClick = { viewModel.removeSource(source.uri) }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.decrypt_remove),
                    )
                }
            }
        }

        SectionTitle(stringResource(R.string.decrypt_section_destination))
        OutlinedButton(onClick = { destinationLauncher.launch(null) }) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Text(
                text = state.destinationName
                    ?: stringResource(R.string.decrypt_choose_destination),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Button(
            onClick = viewModel::start,
            enabled = viewModel.canStart(state, batch),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.decrypt_start))
        }

        if (batch.operation == JobOperation.DECRYPT && batch.items.isNotEmpty()) {
            SectionTitle(stringResource(R.string.decrypt_section_progress))
            JobProgressList(items = batch.items)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
