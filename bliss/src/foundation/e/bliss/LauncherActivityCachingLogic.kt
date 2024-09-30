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
