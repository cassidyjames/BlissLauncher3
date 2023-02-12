/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.database.sqlite.SQLiteDatabase
import android.os.UserHandle
import com.android.launcher3.Launcher
import com.android.launcher3.model.data.AppInfo
import java.io.FileDescriptor
import java.io.PrintWriter

@JvmDefaultWithCompatibility
interface LauncherAppMonitorCallback {
    // Launcher activity Callbacks
    fun onLauncherPreCreate(launcher: Launcher?) {}
    fun onLauncherCreated() {}
    fun onLauncherPreResume() {}
    fun onLauncherResumed() {}
    fun onLauncherStart() {}
    fun onLauncherStop() {}
    fun onLauncherPrePause() {}
    fun onLauncherPaused() {}
    fun onLauncherDestroy(launcher: Launcher) {}
    fun onLauncherStyleChanged(style: String) {}
    fun onLauncherRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>?,
        grantResults: IntArray?
    ) {}

    fun onLauncherFocusChanged(hasFocus: Boolean) {}

    // Launcher app Callbacks
    fun onAppCreated(context: Context?) {}
    fun onReceive(intent: Intent?) {}
    fun onUIConfigChanged() {}
    fun onThemeChanged() {}
    fun onAppSharedPreferenceChanged(key: String?) {}
    fun onPackageRemoved(packageName: String?, user: UserHandle?) {}
    fun onPackageAdded(packageName: String?, user: UserHandle?) {}
    fun onPackageChanged(packageName: String?, user: UserHandle?) {}
    fun onPackagesAvailable(packageNames: Array<String?>?, user: UserHandle?, replacing: Boolean) {}
    fun onPackagesUnavailable(
        packageNames: Array<String?>?,
        user: UserHandle?,
        replacing: Boolean
    ) {}

    fun onPackagesSuspended(packageNames: Array<String?>?, user: UserHandle?) {}
    fun onPackagesUnsuspended(packageNames: Array<String?>?, user: UserHandle?) {}
    fun onShortcutsChanged(
        packageName: String?,
        shortcuts: List<ShortcutInfo?>?,
        user: UserHandle?
    ) {}

    fun onAllAppsListUpdated(apps: List<AppInfo?>?) {}
    fun onLauncherLocaleChanged() {}
    fun onLauncherOrientationChanged() {}
    fun onLauncherScreensizeChanged() {}
    fun onLoadAllAppsEnd(apps: ArrayList<AppInfo?>?) {}

    // Launcher database callbacks
    fun dump(prefix: String?, fd: FileDescriptor?, w: PrintWriter?, dumpAll: Boolean) {}
    fun dump(prefix: String, fd: FileDescriptor, w: PrintWriter, args: Array<String>) {}
    fun onLauncherDbUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun onReceiveHomeIntent() {}
    fun onLauncherWorkspaceBindingFinish() {}
    fun onLauncherAllAppBindingFinish(apps: Array<AppInfo>) {}
}
