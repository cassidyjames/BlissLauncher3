/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.multimode

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.R
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.util.Executors.MODEL_EXECUTOR
import foundation.e.bliss.BaseController
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.LauncherAppMonitorCallback
import foundation.e.bliss.blur.BlurWallpaperProvider
import foundation.e.bliss.preferences.BlissPrefs
import java.io.FileDescriptor
import java.io.PrintWriter

class MultiModeController(val context: Context, val monitor: LauncherAppMonitor) : BaseController {

    private val idp by lazy { InvariantDeviceProfile.INSTANCE.get(context) }
    private val mAppMonitorCallback: LauncherAppMonitorCallback =
        object : LauncherAppMonitorCallback {
            override fun onLoadAllAppsEnd(apps: ArrayList<AppInfo?>?) {
                MODEL_EXECUTOR.submit(
                    VerifyIdleAppTask(
                        context,
                        apps,
                        null,
                        null,
                        false,
                        monitor.launcher.model.mBgDataModel
                    )
                )
            }

            override fun onAppSharedPreferenceChanged(key: String?) {
                when (key) {
                    BlissPrefs.PREF_SINGLE_LAYER_MODE -> {
                        monitor.launcher.model.forceReload()
                    }
                    else -> Unit
                }
            }

            override fun onLauncherOrientationChanged() {
                BlurWallpaperProvider.getInstanceNoCreate().orientationChanged()
            }

            override fun dump(
                prefix: String?,
                fd: FileDescriptor?,
                w: PrintWriter?,
                dumpAll: Boolean
            ) {
                w?.let {
                    println()
                    println("$prefix $TAG: ${this@MultiModeController}")
                }
            }
        }

    init {
        sharedPreferences = LauncherPrefs.getPrefs(context)
        resources = context.resources
        monitor.registerCallback(mAppMonitorCallback)
        Log.d(TAG, this.toString())
    }

    override fun dumpState(
        prefix: String?,
        fd: FileDescriptor?,
        writer: PrintWriter?,
        dumpAll: Boolean
    ) {
        writer?.let {
            println()
            println("$prefix $TAG: ${this@MultiModeController}")
        }
    }

    companion object {
        private const val TAG = "MultiModeController"
        @JvmField var sharedPreferences: SharedPreferences? = null
        @JvmField var resources: Resources? = null

        private fun throwIfControllerNotInit() {
            val launcherAppMonitor = LauncherAppMonitor.getInstanceNoCreate()
            if (launcherAppMonitor == null || launcherAppMonitor.multiModeController == null) {
                throw RuntimeException("MultiModeController is not init.")
            }
        }

        @JvmStatic
        val isSingleLayerMode: Boolean
            get() {
                throwIfControllerNotInit()
                return sharedPreferences!!
                    .getBoolean(
                        BlissPrefs.PREF_SINGLE_LAYER_MODE,
                        resources!!.getBoolean(R.bool.default_single_mode)
                    )
                    .apply {
                        Log.d(TAG, "Single Layer Mode (getter): $this")
                        return@apply
                    }
            }
    }
}
