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
import com.android.launcher3.widget.LauncherAppWidgetHostView

class BlissAppWidgetHost(val context: Context) : AppWidgetHost(context, WIDGET_HOST_ID) {
    fun createView(widgetId: Int, widgetInfo: AppWidgetProviderInfo): AppWidgetHostView {
        return createView(context, widgetId, widgetInfo).apply { setPaddingRelative(8, 24, 8, 24) }
    }

    @SuppressLint("NewApi")
    override fun onCreateView(
        context: Context?,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ) = LauncherAppWidgetHostView(context)

    companion object {
        const val TAG = "BlissAppWidgetHost"
        const val WIDGET_HOST_ID = 1040
        const val REQUEST_CONFIGURE_APPWIDGET = 1041
    }
}
