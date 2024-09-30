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
package foundation.e.bliss.folder

import android.content.Context
import com.android.launcher3.folder.Folder
import foundation.e.bliss.BaseController
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.LauncherAppMonitorCallback
import java.io.FileDescriptor
import java.io.PrintWriter

class GridFolderController(context: Context, val monitor: LauncherAppMonitor) : BaseController {

    private val mAppMonitorCallback: LauncherAppMonitorCallback =
        object : LauncherAppMonitorCallback {
            override fun onReceiveHomeIntent() {
                val folder = Folder.getOpen(monitor.launcher)
                if (folder is GridFolder) {
                    folder.setNeedResetState(false)
                }
            }
        }

    val gridFolderIconLayoutRule = GridFolderIconLayoutRule(context)

    init {
        monitor.registerCallback(mAppMonitorCallback)
    }

    override fun dumpState(
        prefix: String?,
        fd: FileDescriptor?,
        writer: PrintWriter?,
        dumpAll: Boolean
    ) {
        writer?.let {
            println()
            println("$prefix ${TAG}: ${this@GridFolderController}")
        }
    }

    companion object {
        private const val TAG = "GridFolderController"
    }
}
