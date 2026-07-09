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

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

object BiometricUnlock {

    fun show(
        context: Context,
        title: String,
        cancelLabel: String,
        onSuccess: () -> Unit,
    ) {
        val activity = context.findFragmentActivity() ?: return
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(cancelLabel)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        BiometricPrompt(activity, activity.mainExecutor, callback).authenticate(promptInfo)
    }

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var current: Context = this
        while (current is ContextWrapper) {
            if (current is FragmentActivity) return current
            current = current.baseContext
        }
        return null
    }
}
