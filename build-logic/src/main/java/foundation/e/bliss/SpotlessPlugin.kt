/*
 * Copyright (C) 2024 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package foundation.e.bliss

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

@Suppress("Unused")
class SpotlessPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val licenseFile = File("${project.rootDir}/bliss/HEADER")

        project.pluginManager.apply(SpotlessPlugin::class)
        project.extensions.getByType<SpotlessExtension>().run {
            java {
                target("bliss/**/*.java", "build-logic/**/*.java")
                removeUnusedImports()
                eclipse()
                indentWithTabs(2)
                indentWithSpaces(4)
                licenseHeaderFile(licenseFile)
            }

            kotlin {
                ktfmt().kotlinlangStyle()
                target("bliss/**/*.kt", "build-logic/**/*.kt")
                targetExclude("**/build/")
                trimTrailingWhitespace()
                licenseHeaderFile(licenseFile)
            }

            kotlinGradle {
                ktfmt().kotlinlangStyle()
                target("bliss/**/*.gradle.kts", "build-logic/**/*.gradle.kts")
                targetExclude("**/build/")
            }
        }
    }
}
