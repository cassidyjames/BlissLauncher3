/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
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

    private val mCompositeDisposable = CompositeDisposable()

    private fun refreshRecyclerView() {
        val widgetIds = mAppWidgetHost.appWidgetIds
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

        val addedWidgets = findViewById<RecyclerView>(R.id.added_widgets_recycler_view)
        addedWidgets.apply {
            addedWidgets!!.layoutManager = LinearLayoutManager(this@WidgetsActivity)
            addedWidgets.setHasFixedSize(false)
            addedWidgets.isNestedScrollingEnabled = false
            addedWidgets.addItemDecoration(
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
        CoroutineScope(Dispatchers.Main).launch {
            WidgetContainer.WidgetFragment.eventFlow.emit(Unit)
        }

        if (mAddedWidgetsAdapter.itemCount == 0) {
            finish()
        }
    }
}
