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
package foundation.e.bliss.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import com.android.launcher3.R

class BlissAppWidgetHost(val context: Context) : AppWidgetHost(context, WIDGET_HOST_ID) {
    fun createView(widgetId: Int, widgetInfo: AppWidgetProviderInfo): AppWidgetHostView {
        return createView(context, widgetId, widgetInfo).apply {
            val widgetTopBottom =
                context.resources.getDimensionPixelSize(R.dimen.widget_page_top_bottom_padding)
            val widgetLeftRight =
                context.resources.getDimensionPixelSize(R.dimen.widget_page_all_padding)
            setPaddingRelative(widgetLeftRight, widgetTopBottom, widgetLeftRight, widgetTopBottom)
        }
    }

    @SuppressLint("NewApi")
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        val blur = DefaultWidgets.defaultWidgets.contains(appWidget?.provider)
        return RoundedWidgetView(context, blur)
    }

    override fun onAppWidgetRemoved(appWidgetId: Int) {
        deleteAppWidgetId(appWidgetId)
    }

    companion object {
        const val TAG = "BlissAppWidgetHost"
        const val WIDGET_HOST_ID = 0x7f090001
        const val REQUEST_CONFIGURE_APPWIDGET = 1041
    }
}
