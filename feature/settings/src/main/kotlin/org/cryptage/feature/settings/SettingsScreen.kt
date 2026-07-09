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

package org.cryptage.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import org.cryptage.core.model.ZstdSettings
import org.cryptage.feature.applock.BiometricAvailability
import org.cryptage.feature.applock.SetPinDialog

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenKeys: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zstd by viewModel.zstd.collectAsStateWithLifecycle()
    val appLock by viewModel.appLock.collectAsStateWithLifecycle()
    val showSetPin by viewModel.showSetPin.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricAvailable = remember { BiometricAvailability.canAuthenticate(context) }

    if (showSetPin) {
        SetPinDialog(
            onConfirm = viewModel::confirmPin,
            onDismiss = viewModel::dismissSetPin,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        SectionTitle(stringResource(R.string.settings_section_app_lock))
        ToggleRow(
            title = stringResource(R.string.settings_biometric),
            subtitle = if (biometricAvailable) {
                stringResource(R.string.settings_biometric_subtitle)
            } else {
                stringResource(R.string.settings_biometric_unavailable)
            },
            checked = appLock.biometricEnabled && biometricAvailable,
            enabled = biometricAvailable,
            onCheckedChange = viewModel::setBiometricEnabled,
        )
        ToggleRow(
            title = stringResource(R.string.settings_pin),
            subtitle = stringResource(R.string.settings_pin_subtitle),
            checked = appLock.pinEnabled,
            enabled = true,
            onCheckedChange = { enabled ->
                if (enabled) viewModel.requestEnablePin() else viewModel.disablePin()
            },
        )

        SectionTitle(
            text = stringResource(R.string.settings_section_compression),
            modifier = Modifier.padding(top = 24.dp),
        )
        ToggleRow(
            title = stringResource(R.string.settings_zstd),
            subtitle = stringResource(R.string.settings_zstd_subtitle),
            checked = zstd.enabled,
            enabled = true,
            onCheckedChange = viewModel::setZstdEnabled,
        )
        if (zstd.enabled) {
            ZstdLevelSlider(zstd = zstd, onLevelChosen = viewModel::setZstdLevel)
        }

        SectionTitle(
            text = stringResource(R.string.settings_section_more),
            modifier = Modifier.padding(top = 24.dp),
        )
        NavigationRow(
            title = stringResource(R.string.settings_keys),
            subtitle = stringResource(R.string.settings_keys_subtitle),
            onClick = onOpenKeys,
        )
        NavigationRow(
            title = stringResource(R.string.settings_licenses),
            subtitle = stringResource(R.string.settings_licenses_subtitle),
            onClick = onOpenLicenses,
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun NavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun ZstdLevelSlider(zstd: ZstdSettings, onLevelChosen: (Int) -> Unit) {
    var sliderValue by remember(zstd.level) { mutableFloatStateOf(zstd.level.toFloat()) }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.settings_zstd_level, sliderValue.roundToInt()),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onLevelChosen(sliderValue.roundToInt()) },
            valueRange = ZstdSettings.MIN_LEVEL.toFloat()..ZstdSettings.MAX_LEVEL.toFloat(),
            steps = ZstdSettings.MAX_LEVEL - ZstdSettings.MIN_LEVEL - 1,
        )
    }
}
