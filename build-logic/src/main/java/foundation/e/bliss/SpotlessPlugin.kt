/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
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
        val licenseFile = File("${project.rootDir}/bliss/LICENSE.txt")

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
