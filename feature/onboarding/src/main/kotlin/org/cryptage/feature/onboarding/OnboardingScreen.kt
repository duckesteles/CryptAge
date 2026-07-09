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

package org.cryptage.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cryptage.feature.applock.BiometricAvailability
import org.cryptage.feature.applock.SetPinDialog

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.showSetPin) {
        SetPinDialog(
            onConfirm = viewModel::confirmPin,
            onDismiss = viewModel::dismissSetPin,
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            when (state.step) {
                OnboardingViewModel.Step.WELCOME -> WelcomeStep(viewModel)
                OnboardingViewModel.Step.BACKUP -> BackupStep(viewModel, state)
                OnboardingViewModel.Step.APP_LOCK -> AppLockStep(viewModel, state)
            }
        }
    }
}

@Composable
private fun WelcomeStep(viewModel: OnboardingViewModel) {
    val keyName = stringResource(R.string.onboarding_default_key_name)
    Icon(
        imageVector = Icons.Filled.Lock,
        contentDescription = null,
        modifier = Modifier.size(56.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text = stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    Button(
        onClick = { viewModel.generateKey(keyName) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        Text(stringResource(R.string.onboarding_generate_key))
    }
}

@Composable
private fun BackupStep(viewModel: OnboardingViewModel, state: OnboardingViewModel.UiState) {
    val clipboard = LocalClipboardManager.current
    Text(
        text = stringResource(R.string.onboarding_backup_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = stringResource(R.string.onboarding_backup_warning),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 12.dp),
    )
    KeyMaterialCard(
        label = stringResource(R.string.onboarding_public_key),
        value = state.recipient.orEmpty(),
        onCopy = { clipboard.setText(AnnotatedString(state.recipient.orEmpty())) },
    )
    KeyMaterialCard(
        label = stringResource(R.string.onboarding_secret_key),
        value = state.identity.orEmpty(),
        onCopy = { clipboard.setText(AnnotatedString(state.identity.orEmpty())) },
    )
    Button(
        onClick = viewModel::continueToAppLock,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        Text(stringResource(R.string.onboarding_backup_done))
    }
}

@Composable
private fun KeyMaterialCard(label: String, value: String, onCopy: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.onboarding_copy),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLockStep(viewModel: OnboardingViewModel, state: OnboardingViewModel.UiState) {
    val context = LocalContext.current
    val biometricAvailable = remember { BiometricAvailability.canAuthenticate(context) }
    Text(
        text = stringResource(R.string.onboarding_lock_title),
        style = MaterialTheme.typography.headlineSmall,
    )
    Text(
        text = stringResource(R.string.onboarding_lock_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    if (biometricAvailable) {
        LockToggleRow(
            title = stringResource(R.string.onboarding_lock_biometric),
            checked = state.biometricEnabled,
            onCheckedChange = viewModel::setBiometricEnabled,
        )
    }
    LockToggleRow(
        title = stringResource(R.string.onboarding_lock_pin),
        checked = state.pinEnabled,
        onCheckedChange = { enabled ->
            if (enabled) viewModel.requestEnablePin() else viewModel.disablePin()
        },
    )
    Button(
        onClick = viewModel::finish,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    ) {
        Text(stringResource(R.string.onboarding_finish))
    }
}

@Composable
private fun LockToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
