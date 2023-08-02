/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import android.animation.LayoutTransition
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ServiceManager
import android.os.UserHandle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.android.app.animation.R.id.launcher
import com.android.internal.appwidget.IAppWidgetService
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.PendingAddItemInfo
import com.android.launcher3.R
import com.android.launcher3.config.FeatureFlags
import com.android.launcher3.graphics.FragmentWithPreview
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo
import com.android.launcher3.widget.PendingAddShortcutInfo
import com.android.launcher3.widget.WidgetCell
import com.android.launcher3.widget.picker.WidgetsFullSheet
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.LauncherAppMonitorCallback
import foundation.e.bliss.utils.Logger
import foundation.e.bliss.utils.ObservableList
import foundation.e.bliss.widgets.BlissAppWidgetHost.Companion.REQUEST_CONFIGURE_APPWIDGET
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Suppress("Deprecation", "NewApi")
class WidgetContainer(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val mLauncher by lazy { Launcher.getLauncher(context) }

    private lateinit var mRemoveWidgetLayout: FrameLayout
    private lateinit var mWrapper: LinearLayout
    private var mWrapperChildCount = 0

    private val layoutListener = OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
        val childCount = (view as LinearLayout).childCount
        if (mWrapperChildCount == childCount) return@OnLayoutChangeListener
        handleRemoveButtonVisibility(childCount)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, 0, 0, 0)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findViewById<Button>(R.id.manage_widgets)!!.setOnClickListener {
            WidgetsFullSheet.show(mLauncher, true, true)
        }

        findViewById<Button>(R.id.remove_widgets)!!.setOnClickListener {
            val intent =
                Intent(context, WidgetsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        mRemoveWidgetLayout = findViewById(R.id.remove_widget_parent)!!
        mWrapper =
            findViewWithTag<LinearLayout?>("wrapper_children").apply {
                addOnLayoutChangeListener(layoutListener)
                handleRemoveButtonVisibility(childCount)
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mWrapper.removeOnLayoutChangeListener(layoutListener)
    }

    private fun handleRemoveButtonVisibility(childCount: Int) {
        mWrapperChildCount = childCount
        CoroutineScope(Dispatchers.Main).launch {
            if (childCount == 0) {
                mRemoveWidgetLayout.visibility = View.GONE
            } else if (mRemoveWidgetLayout.visibility == View.GONE) {
                mRemoveWidgetLayout.visibility = View.VISIBLE
            }
        }
    }

    /** A fragment to display the default widgets. */
    class WidgetFragment : FragmentWithPreview() {
        private lateinit var mWrapper: LinearLayout
        private lateinit var widgetObserver: Disposable

        private val mWidgetManager by lazy { AppWidgetManager.getInstance(context) }
        private val mWidgetHost by lazy { BlissAppWidgetHost(context) }
        private val widgetsDbHelper by lazy { WidgetsDbHelper.getInstance(context) }
        private val launcher by lazy { Launcher.getLauncher(context) }

        private val mAppMonitorCallback: LauncherAppMonitorCallback =
            object : LauncherAppMonitorCallback {
                override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                    rebindWidgets()
                }
            }

        private var initialWidgetsAdded: Boolean
            set(value) {
                LauncherPrefs.getPrefs(context)
                    .edit()
                    .putBoolean(defaultWidgetsAdded, value)
                    .apply()
            }
            get() {
                return LauncherPrefs.getPrefs(context).getBoolean(defaultWidgetsAdded, false)
            }

        private val isQsbEnabled: Boolean
            get() = FeatureFlags.QSB_ON_FIRST_SCREEN.get()

        init {
            LauncherAppMonitor.getInstanceNoCreate().registerCallback(mAppMonitorCallback)
        }

        @Deprecated("Deprecated in Java")
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            mWrapper =
                LinearLayout(context, null).apply {
                    tag = "wrapper_children"
                    orientation = LinearLayout.VERTICAL
                }

            if (isQsbEnabled) {
                loadWidgets()
                mWidgetHost.startListening()
            }
            return mWrapper
        }

        override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
        }

        private fun loadWidgets() {
            if (!initialWidgetsAdded) {
                mWidgetHost.deleteHost()
                Logger.e(TAG, "default not added ${mWidgetHost.appWidgetIds.size}")

                DefaultWidgets.getWidgetsList(context).forEach {
                    try {
                        bindWidget(it)
                    } catch (e: Exception) {
                        Logger.e(TAG, "Could not add widget ${it.flattenToString()}")
                    }
                }

                initialWidgetsAdded = true
            } else {
                rebindWidgets()
                Logger.e(TAG, "saved widgets added")
            }

            widgetObserver =
                defaultWidgets.observable.subscribe {
                    Logger.d(TAG, "Component: ${it.flattenToString()}")
                    bindWidget(it)
                }

            CoroutineScope(Dispatchers.Main).launch { eventFlow.collect { rebindWidgets() } }
        }

        private fun rebindWidgets() {
            mWrapper.removeAllViews()
            mWidgetHost.appWidgetIds.sorted().forEach(::addView)
        }

        private fun bindWidget(provider: ComponentName) {
            val widgetId = mWidgetHost.allocateAppWidgetId()
            val isWidgetBound = mWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)

            if (!isWidgetBound) {
                mWidgetHost.deleteAppWidgetId(widgetId)
                Logger.e(TAG, "Could not add widget ${provider.flattenToString()}")
            }

            configureWidget(widgetId)
        }

        private fun configureWidget(widgetId: Int) {
            val widgetInfo =
                LauncherAppWidgetProviderInfo.fromProviderInfo(
                    Launcher.getLauncher(context),
                    mWidgetManager.getAppWidgetInfo(widgetId)
                )

            if (widgetInfo != null) {
                if (widgetInfo.configure != null) {
                    sendIntent(widgetId)
                } else {
                    addView(widgetId)
                }
            } else {
                mWidgetHost.deleteAppWidgetId(id)
            }
        }

        private fun addView(widgetId: Int) {
            val info =
                LauncherAppWidgetProviderInfo.fromProviderInfo(
                    Launcher.getLauncher(context),
                    mWidgetManager.getAppWidgetInfo(widgetId)
                )

            if (info != null) {
                mWidgetHost
                    .createView(widgetId, info)
                    .apply {
                        id = widgetId
                        layoutTransition = LayoutTransition()
                        setOnLongClickListener {
                            if (
                                (info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) ==
                                    AppWidgetProviderInfo.RESIZE_VERTICAL
                            ) {
                                launcher.hideWidgetResizeContainer()
                                launcher.showWidgetResizeContainer(this as RoundedWidgetView)
                            }
                            true
                        }
                    }
                    .also {
                        val opts = mWidgetManager.getAppWidgetOptions(it.appWidgetId)
                        val params =
                            LayoutParams(
                                opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                                opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                            )

                        Logger.d(TAG, "Widget height: ${params.height} | opts: $opts")

                        if (params.height > 0) {
                            mWrapper.addView(it, params)
                        } else {
                            mWrapper.addView(it)
                        }

                        widgetsDbHelper.insert(
                            WidgetInfo(
                                mWrapper.indexOfChild(it),
                                it.appWidgetInfo.provider,
                                it.appWidgetId,
                                it.height
                            )
                        )
                    }
            } else {
                mWidgetHost.deleteAppWidgetId(widgetId)
            }
        }

        private fun sendIntent(widgetId: Int) {
            val bundle = Bundle().apply { putInt(EXTRA_APPWIDGET_ID, widgetId) }
            // Access internal AppWidgetService through Stub
            val intentSender =
                IAppWidgetService.Stub.asInterface(
                        ServiceManager.getService(Context.APPWIDGET_SERVICE)
                    )
                    .createAppWidgetConfigIntentSender(context.opPackageName, widgetId, 0)

            startIntentSenderForResult(
                intentSender,
                REQUEST_CONFIGURE_APPWIDGET,
                null,
                0,
                0,
                0,
                bundle
            )
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val widgetId = data?.getIntExtra(EXTRA_APPWIDGET_ID, -1) ?: -1
            Logger.d(TAG, "Request: $requestCode | Result: $resultCode | Widget: $widgetId")
            if (resultCode == RESULT_OK && requestCode == REQUEST_CONFIGURE_APPWIDGET) {
                addView(widgetId)
            } else {
                mWidgetHost.deleteAppWidgetId(widgetId)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onDestroy() {
            super.onDestroy()
            widgetObserver.dispose()
        }

        companion object {
            const val TAG = "WidgetFragment"
            const val defaultWidgetsAdded = "default_widgets_added"
            val defaultWidgets = ObservableList<ComponentName>()
            val eventFlow = MutableSharedFlow<Unit>()

            @JvmStatic
            fun onWidgetClick(context: Context, view: View, closeSheet: (Boolean) -> Unit) {
                val tag =
                    when {
                        view is WidgetCell -> {
                            view.getTag()
                        }
                        view.parent is WidgetCell -> {
                            (view.parent as WidgetCell).tag
                        }
                        else -> null
                    }

                if (tag is PendingAddShortcutInfo) {
                    Toast.makeText(context, "Please select a widget", Toast.LENGTH_SHORT).show()
                } else {
                    closeSheet(true)
                    val widget = (view.parent as WidgetCell).tag as PendingAddItemInfo
                    defaultWidgets.add(widget.componentName)
                }
            }
        }
    }
}
