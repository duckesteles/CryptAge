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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    biometricEnabled: Boolean,
    pinEnabled: Boolean,
    verifyPin: suspend (String) -> Boolean,
    onUnlocked: () -> Unit,
) {
    val context = LocalContext.current
    val biometricAvailable = remember(biometricEnabled) {
        biometricEnabled && BiometricAvailability.canAuthenticate(context)
    }
    val biometricTitle = stringResource(R.string.applock_biometric_title)
    val biometricCancel = stringResource(R.string.applock_cancel)
    var pin by rememberSaveable { mutableStateOf("") }
    var wrongPin by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(biometricAvailable, pinEnabled) {
        if (biometricAvailable) {
            BiometricUnlock.show(context, biometricTitle, biometricCancel, onUnlocked)
        } else if (!pinEnabled) {
            onUnlocked()
        }
    }

    fun submitPin() {
        scope.launch {
            if (verifyPin(pin)) {
                onUnlocked()
            } else {
                wrongPin = true
                pin = ""
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.applock_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (pinEnabled) {
                PinField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        wrongPin = false
                    },
                    label = stringResource(R.string.applock_pin_label),
                    modifier = Modifier.padding(top = 24.dp),
                )
                if (wrongPin) {
                    Text(
                        text = stringResource(R.string.applock_wrong_pin),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Button(
                    onClick = ::submitPin,
                    enabled = pin.isNotEmpty(),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.applock_unlock))
                }
            }
            if (biometricAvailable) {
                TextButton(
                    onClick = {
                        BiometricUnlock.show(context, biometricTitle, biometricCancel, onUnlocked)
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.applock_use_biometric))
                }
            }
        }
    }
}
