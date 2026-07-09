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
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import org.cryptage.core.ui.PassphraseField
import org.cryptage.core.ui.primaryStorageInitialUri

@Composable
fun EncryptScreen(viewModel: EncryptViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val batch by viewModel.batch.collectAsStateWithLifecycle()
    val recipients by viewModel.recipientEntries.collectAsStateWithLifecycle()

    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.addFiles(uris) }
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> viewModel.addFolder(uri) }
    val destinationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> viewModel.setDestination(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle(stringResource(R.string.encrypt_section_input))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { filesLauncher.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.NoteAdd, contentDescription = null)
                Text(
                    text = stringResource(R.string.encrypt_add_files),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = { folderLauncher.launch(primaryStorageInitialUri) }) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Text(
                    text = stringResource(R.string.encrypt_add_folder),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        for (source in state.sources) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (source.isDirectory) Icons.Filled.Folder else Icons.Filled.InsertDriveFile,
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
                        contentDescription = stringResource(R.string.encrypt_remove),
                    )
                }
            }
        }

        SectionTitle(stringResource(R.string.encrypt_section_mode))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.keyBased,
                onClick = { viewModel.setKeyBased(true) },
                label = { Text(stringResource(R.string.encrypt_mode_keys)) },
            )
            FilterChip(
                selected = !state.keyBased,
                onClick = { viewModel.setKeyBased(false) },
                label = { Text(stringResource(R.string.encrypt_mode_passphrase)) },
            )
        }
        if (state.keyBased) {
            if (recipients.isEmpty()) {
                Text(
                    text = stringResource(R.string.encrypt_no_recipients),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            for (entry in recipients) {
                RecipientRow(
                    name = entry.name,
                    selected = entry.id in state.selectedRecipientIds,
                    onToggle = { viewModel.toggleRecipient(entry.id) },
                )
            }
        } else {
            PassphraseField(
                value = state.passphrase,
                onValueChange = viewModel::setPassphrase,
                label = stringResource(R.string.encrypt_passphrase_label),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionTitle(stringResource(R.string.encrypt_section_destination))
        OutlinedButton(onClick = { destinationLauncher.launch(primaryStorageInitialUri) }) {
            Icon(Icons.Filled.Folder, contentDescription = null)
            Text(
                text = state.destinationName
                    ?: stringResource(R.string.encrypt_choose_destination),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Button(
            onClick = viewModel::start,
            enabled = viewModel.canStart(state, batch),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.encrypt_start))
        }

        if (batch.operation == JobOperation.ENCRYPT && batch.items.isNotEmpty()) {
            SectionTitle(stringResource(R.string.encrypt_section_progress))
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

@Composable
private fun RecipientRow(name: String, selected: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
    }
}
