/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import android.content.ComponentName

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

    val defaultWidgets = listOf(ecloudWidget, privacyWidget, weatherWidget)
}
