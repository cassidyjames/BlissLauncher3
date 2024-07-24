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

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

@Suppress("Unused")
class GitHooksPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register<Copy>("copyGitHooks") {
            description = "Copies the git hooks from /hooks to the .git/hooks folder."
            from("${project.rootDir}/hooks/") {
                include("**/*.sh")
                rename("(.*).sh", "$1")
            }
            into("${project.rootDir}/.git/hooks")
        }

        project.tasks.register<Exec>("installGitHooks") {
            description = "Installs the pre-commit hooks with permissions"
            commandLine("chmod", "-R", "+x", ".git/hooks/")
            onlyIf { !Os.isFamily(Os.FAMILY_WINDOWS) }
        }
    }
}
