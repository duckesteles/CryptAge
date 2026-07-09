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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cryptage.core.model.KeyEntry
import org.cryptage.core.ui.ConfirmDialog

@Composable
fun KeysScreen(viewModel: KeysViewModel, modifier: Modifier = Modifier) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    KeysDialogs(viewModel, dialog)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::showGenerateDialog) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(
                    text = stringResource(R.string.keys_generate),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(onClick = viewModel::showImportDialog) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Text(
                    text = stringResource(R.string.keys_import),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.keys_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            items(entries, key = KeyEntry::id) { entry ->
                KeyEntryCard(
                    entry = entry,
                    onCopyRecipient = { recipient ->
                        clipboard.setText(AnnotatedString(recipient))
                    },
                    onReveal = { viewModel.showRevealDialog(entry) },
                    onDelete = { viewModel.showDeleteDialog(entry) },
                )
            }
        }
    }
}

@Composable
private fun KeyEntryCard(
    entry: KeyEntry,
    onCopyRecipient: (String) -> Unit,
    onReveal: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.keys_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.hasIdentity) {
                    AssistChip(
                        onClick = onReveal,
                        label = { Text(stringResource(R.string.keys_badge_secret)) },
                    )
                }
                if (entry.hasRecipient) {
                    AssistChip(
                        onClick = { entry.recipient?.let(onCopyRecipient) },
                        label = { Text(stringResource(R.string.keys_badge_public)) },
                    )
                }
            }
            val recipient = entry.recipient
            if (recipient != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = recipient,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onCopyRecipient(recipient) }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.keys_copy_public),
                        )
                    }
                }
            }
            if (entry.hasIdentity) {
                TextButton(onClick = onReveal) {
                    Text(stringResource(R.string.keys_reveal_secret))
                }
            }
        }
    }
}

@Composable
private fun KeysDialogs(viewModel: KeysViewModel, dialog: KeysViewModel.Dialog) {
    when (dialog) {
        is KeysViewModel.Dialog.None -> Unit
        is KeysViewModel.Dialog.Generate -> GenerateKeyDialog(
            onConfirm = viewModel::generate,
            onDismiss = viewModel::dismissDialog,
        )
        is KeysViewModel.Dialog.Import -> ImportKeyDialog(
            error = dialog.error,
            onConfirm = viewModel::import,
            onDismiss = viewModel::dismissDialog,
        )
        is KeysViewModel.Dialog.Reveal -> RevealIdentityDialog(
            entry = dialog.entry,
            onDismiss = viewModel::dismissDialog,
        )
        is KeysViewModel.Dialog.ConfirmDelete -> ConfirmDialog(
            title = stringResource(R.string.keys_delete_title),
            message = stringResource(R.string.keys_delete_message, dialog.entry.name),
            confirmLabel = stringResource(R.string.keys_delete),
            onConfirm = { viewModel.delete(dialog.entry) },
            onDismiss = viewModel::dismissDialog,
        )
    }
}
