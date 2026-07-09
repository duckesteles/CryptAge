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

package org.cryptage.feature.applock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 8

@Composable
fun SetPinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    val mismatch = confirmation.isNotEmpty() && pin != confirmation
    val valid = pin.length >= MIN_PIN_LENGTH && pin == confirmation
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.applock_set_pin_title)) },
        text = {
            Column {
                PinField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = stringResource(R.string.applock_new_pin),
                )
                PinField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = stringResource(R.string.applock_confirm_pin),
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (mismatch) {
                    Text(
                        text = stringResource(R.string.applock_pin_mismatch),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid) {
                Text(stringResource(R.string.applock_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.applock_cancel))
            }
        },
    )
}

@Composable
internal fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= MAX_PIN_LENGTH && input.all(Char::isDigit)) {
                onValueChange(input)
            }
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier,
    )
}
