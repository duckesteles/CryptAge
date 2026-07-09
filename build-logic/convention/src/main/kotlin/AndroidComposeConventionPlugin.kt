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

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.cryptage.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

abstract class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            val commonExtension: CommonExtension =
                extensions.findByType(ApplicationExtension::class.java)
                    ?: extensions.getByType(LibraryExtension::class.java)

            commonExtension.buildFeatures.compose = true

            dependencies {
                val bom = libs.findLibrary("compose-bom").get()
                "implementation"(platform(bom))
                "implementation"(libs.findLibrary("compose-material3").get())
                "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
                "implementation"(libs.findLibrary("lifecycle-runtime-compose").get())
                "implementation"(libs.findLibrary("lifecycle-viewmodel-compose").get())
                "debugImplementation"(libs.findLibrary("compose-ui-tooling").get())
            }
        }
    }
}
