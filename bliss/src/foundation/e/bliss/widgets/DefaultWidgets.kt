/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import foundation.e.bliss.utils.BlissDbUtils

object DefaultWidgets {
    private val ecloudWidget =
        ComponentName("foundation.e.drive", "foundation.e.drive.widgets.EDriveWidget")
    private val privacyWidget =
        ComponentName("foundation.e.advancedprivacy", "foundation.e.advancedprivacy.Widget")
    val oldWeatherWidget =
        ComponentName(
            "foundation.e.blisslauncher",
            "foundation.e.blisslauncher.features.weather.WeatherAppWidgetProvider"
        )
    val weatherWidget =
        ComponentName(
            "foundation.e.blissweather",
            "foundation.e.blissweather.widget.WeatherAppWidgetProvider"
        )

    private val widgets = listOf(ecloudWidget, privacyWidget, weatherWidget)

    @JvmStatic
    fun getWidgetsList(context: Context): List<ComponentName> {
        val providerList: MutableList<ComponentName> = mutableListOf()

        // Get widget details from old database
        val widgetItemsList: MutableList<BlissDbUtils.WidgetItems> =
            BlissDbUtils.getWidgetDetails(context)

        for (widgetItem in widgetItemsList) {
            val provider = widgetItem.componentName
            provider.let(providerList::add)
        }

        // Disable dummy widget provider component after widgets listed.
        val packageManager = context.packageManager
        packageManager.setComponentEnabledSetting(
            oldWeatherWidget,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // Return default widgets if the providerList is empty
        return providerList.ifEmpty { widgets }
    }
}
