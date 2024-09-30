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

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import foundation.e.bliss.widgets.AddedWidgetsAdapter.OnActionClickListener
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetsActivity : Activity(), OnActionClickListener {
    private lateinit var mAddedWidgetsAdapter: AddedWidgetsAdapter
    private lateinit var mAppWidgetManager: AppWidgetManager
    private lateinit var mAppWidgetHost: BlissAppWidgetHost
    private lateinit var widgetsDbHelper: WidgetsDbHelper

    private val mCompositeDisposable = CompositeDisposable()

    private fun refreshRecyclerView() {
        val widgetIds = mAppWidgetHost.appWidgetIds.sorted()
        val widgets = mutableListOf<BlissWidget>()

        for (id in widgetIds) {
            val appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(id)
            if (appWidgetInfo != null) {
                val widget = BlissWidget(id, appWidgetInfo)
                widgets.add(widget)
            }
        }

        mAddedWidgetsAdapter.setAppWidgetProviderInfos(widgets)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widgets)
        setActionBar(findViewById(R.id.action_bar))

        mAppWidgetManager = AppWidgetManager.getInstance(this)
        mAppWidgetHost = BlissAppWidgetHost(this)
        widgetsDbHelper = WidgetsDbHelper.getInstance(this)

        val addedWidgets = findViewById<RecyclerView>(R.id.added_widgets_recycler_view)
        addedWidgets!!.apply {
            layoutManager = LinearLayoutManager(this@WidgetsActivity)
            setHasFixedSize(false)
            isNestedScrollingEnabled = true
            addItemDecoration(
                DividerItemDecoration(this@WidgetsActivity, DividerItemDecoration.VERTICAL)
            )
        }

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        mAddedWidgetsAdapter = AddedWidgetsAdapter(this, metrics.densityDpi)
        addedWidgets!!.adapter = mAddedWidgetsAdapter
        refreshRecyclerView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.dispose()
    }

    override fun removeWidget(id: Int) {
        mAppWidgetHost.onAppWidgetRemoved(id)
        widgetsDbHelper.delete(id)
        CoroutineScope(Dispatchers.Main).launch {
            WidgetContainer.WidgetFragment.eventFlow.emit(Unit)
        }

        if (mAddedWidgetsAdapter.itemCount == 0) {
            finish()
        }
    }
}
