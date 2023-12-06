/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

class BlissAppWidgetHost(val context: Context) : AppWidgetHost(context, WIDGET_HOST_ID) {
    private val widgetsDbHelper = WidgetsDbHelper.getInstance(context)

    fun createView(widgetId: Int, widgetInfo: AppWidgetProviderInfo): AppWidgetHostView {
        return createView(context, widgetId, widgetInfo).apply { setPaddingRelative(8, 24, 8, 24) }
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
        widgetsDbHelper.delete(appWidgetId)
    }

    companion object {
        const val TAG = "BlissAppWidgetHost"
        const val WIDGET_HOST_ID = 0x7f090001
        const val REQUEST_CONFIGURE_APPWIDGET = 1041
    }
}
