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

plugins {
    id("cryptage.android.application")
    id("cryptage.android.compose")
}

android {
    namespace = "org.cryptage.app"

    defaultConfig {
        applicationId = "org.cryptage"
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:files"))
    implementation(project(":core:jobs"))
    implementation(project(":core:securestore"))
    implementation(project(":core:settings"))
    implementation(project(":core:ui"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:applock"))
    implementation(project(":feature:encrypt"))
    implementation(project(":feature:decrypt"))
    implementation(project(":feature:keys"))
    implementation(project(":feature:settings"))
    implementation(libs.activity.compose)
    implementation(libs.biometric)
    implementation(libs.kotlinx.coroutines.android)
}
