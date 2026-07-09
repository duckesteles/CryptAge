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

data class LibraryLicense(
    val name: String,
    val license: String,
    val url: String,
)

object LibraryLicenses {

    val entries: List<LibraryLicense> = listOf(
        LibraryLicense(
            name = "kage",
            license = "Apache License 2.0 / MIT",
            url = "https://github.com/android-password-store/kage",
        ),
        LibraryLicense(
            name = "Bouncy Castle",
            license = "MIT License",
            url = "https://www.bouncycastle.org",
        ),
        LibraryLicense(
            name = "hkdf",
            license = "Apache License 2.0",
            url = "https://github.com/patrickfav/hkdf",
        ),
        LibraryLicense(
            name = "kotlin-result",
            license = "ISC License",
            url = "https://github.com/michaelbull/kotlin-result",
        ),
        LibraryLicense(
            name = "zstd-jni",
            license = "BSD 2-Clause License",
            url = "https://github.com/luben/zstd-jni",
        ),
        LibraryLicense(
            name = "Zstandard",
            license = "BSD 3-Clause License",
            url = "https://facebook.github.io/zstd/",
        ),
        LibraryLicense(
            name = "Apache Commons Compress",
            license = "Apache License 2.0",
            url = "https://commons.apache.org/compress/",
        ),
        LibraryLicense(
            name = "Kotlin and KotlinX Coroutines",
            license = "Apache License 2.0",
            url = "https://kotlinlang.org",
        ),
        LibraryLicense(
            name = "Jetpack Compose and Material 3",
            license = "Apache License 2.0",
            url = "https://developer.android.com/compose",
        ),
        LibraryLicense(
            name = "AndroidX (Activity, Lifecycle, DataStore, Biometric, DocumentFile)",
            license = "Apache License 2.0",
            url = "https://developer.android.com/jetpack/androidx",
        ),
        LibraryLicense(
            name = "Tink",
            license = "Apache License 2.0",
            url = "https://developers.google.com/tink",
        ),
        LibraryLicense(
            name = "Material Symbols and Icons",
            license = "Apache License 2.0",
            url = "https://fonts.google.com/icons",
        ),
    )
}
