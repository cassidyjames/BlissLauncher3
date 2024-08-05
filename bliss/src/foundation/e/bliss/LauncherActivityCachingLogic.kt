/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss

import android.content.Context
import android.content.pm.LauncherActivityInfo
import androidx.annotation.Keep
import com.android.launcher3.R
import com.android.launcher3.icons.LauncherActivityCachingLogic as BaseLogic
import foundation.e.bliss.utils.resourcesToMap

@Keep
@Suppress("Unused")
class LauncherActivityCachingLogic(context: Context) : BaseLogic() {
    private val aliasedApps by lazy {
        val list = context.resources.getStringArray(R.array.aliased_apps).toList()
        resourcesToMap(list)
    }

    override fun getLabel(info: LauncherActivityInfo): CharSequence {
        val customLabel = aliasedApps[info.componentName.packageName]
        return if (!customLabel.isNullOrEmpty()) {
            customLabel
        } else super.getLabel(info)
    }
}
