/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.folder

import android.content.Context
import com.android.launcher3.folder.ClippedFolderIconLayoutRule
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

    val gridFolderIconLayoutRule: ClippedFolderIconLayoutRule

    init {
        monitor.registerCallback(mAppMonitorCallback)
        gridFolderIconLayoutRule = GridFolderIconLayoutRule(context)
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
