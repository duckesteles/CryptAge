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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.cryptage.core.model.KeyEntry

@Composable
internal fun GenerateKeyDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.keys_generate_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.keys_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.keys_generate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.keys_cancel))
            }
        },
    )
}

@Composable
internal fun ImportKeyDialog(
    error: KeysViewModel.ImportError?,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var identity by rememberSaveable { mutableStateOf("") }
    var recipient by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.keys_import_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.keys_name_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = identity,
                    onValueChange = { identity = it },
                    label = { Text(stringResource(R.string.keys_identity_label)) },
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text(stringResource(R.string.keys_recipient_label)) },
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (error != null) {
                    Text(
                        text = importErrorMessage(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, identity, recipient) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.keys_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.keys_cancel))
            }
        },
    )
}

@Composable
internal fun RevealIdentityDialog(
    entry: KeyEntry,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.keys_reveal_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = entry.identity.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { entry.identity?.let { clipboard.setText(AnnotatedString(it)) } },
            ) {
                Text(stringResource(R.string.keys_copy_secret))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.keys_close))
            }
        },
    )
}

@Composable
private fun importErrorMessage(error: KeysViewModel.ImportError): String = stringResource(
    when (error) {
        KeysViewModel.ImportError.NO_KEY_MATERIAL -> R.string.keys_error_no_material
        KeysViewModel.ImportError.INVALID_IDENTITY -> R.string.keys_error_invalid_identity
        KeysViewModel.ImportError.INVALID_RECIPIENT -> R.string.keys_error_invalid_recipient
    },
)
