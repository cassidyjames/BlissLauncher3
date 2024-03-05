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
import android.graphics.Insets
import android.graphics.Rect
import android.os.Bundle
import android.os.ServiceManager
import android.os.UserHandle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import com.android.internal.appwidget.IAppWidgetService
import com.android.launcher3.InvariantDeviceProfile
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
import foundation.e.bliss.utils.BlissDbUtils
import foundation.e.bliss.utils.Logger
import foundation.e.bliss.utils.ObservableList
import foundation.e.bliss.utils.disableComponent
import foundation.e.bliss.widgets.BlissAppWidgetHost.Companion.REQUEST_CONFIGURE_APPWIDGET
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Suppress("Deprecation", "NewApi")
class WidgetContainer(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val mLauncher by lazy { Launcher.getLauncher(context) }

    private lateinit var mRemoveWidgetLayout: FrameLayout
    private lateinit var mResizeContainer: RelativeLayout
    private val mResizeContainerRect = Rect()

    private var mInsets: Insets? = null
    private val mInsetPadding =
        context.resources.getDimension(R.dimen.widget_page_inset_padding).toInt()

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(0, 0, 0, 0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mResizeContainer.visibility == VISIBLE && ev.action == MotionEvent.ACTION_DOWN) {
            mResizeContainer.getHitRect(mResizeContainerRect)
            if (!mResizeContainerRect.contains(ev.x.toInt(), ev.y.toInt())) {
                mLauncher.hideWidgetResizeContainer()
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        mInsets = mLauncher.workspace.rootWindowInsets.getInsets(WindowInsets.Type.systemBars())
        val insetPadding = context.resources.getDimension(R.dimen.widget_page_inset_padding).toInt()

        findViewById<Button>(R.id.manage_widgets).setOnClickListener {
            WidgetsFullSheet.show(mLauncher, true, true)
        }

        findViewById<Button>(R.id.remove_widgets).setOnClickListener {
            val intent =
                Intent(context, WidgetsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        mRemoveWidgetLayout = findViewById(R.id.remove_widget_parent)

        findViewWithTag<LinearLayout?>("wrapper_children").apply {
            setOnHierarchyChangeListener(
                object : OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        handleRemoveButtonVisibility((parent as LinearLayout).childCount)
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {
                        handleRemoveButtonVisibility((parent as LinearLayout).childCount)
                    }
                }
            )
        }

        mResizeContainer =
            findViewById<RelativeLayout?>(R.id.widget_resizer_container).apply {
                val layoutParams = this.layoutParams as LayoutParams
                layoutParams.bottomMargin = mInsetPadding + (mInsets?.bottom ?: 0)
                this.layoutParams = layoutParams
            }

        findViewById<LinearLayout>(R.id.widget_linear_layout).apply {
            setPadding(
                this.paddingLeft,
                insetPadding + (mInsets!!.top),
                this.paddingRight,
                (mInsets!!.bottom),
            )
        }
    }

    private fun handleRemoveButtonVisibility(childCount: Int) {
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
        private lateinit var widgetsDbHelper: WidgetsDbHelper

        private val mOldWidgets by lazy { BlissDbUtils.getWidgetDetails(context) }
        private val mWidgetManager by lazy { AppWidgetManager.getInstance(context) }
        private val mWidgetHost by lazy { BlissAppWidgetHost(context) }
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
            widgetsDbHelper = WidgetsDbHelper.getInstance(context)

            mWrapper =
                LinearLayout(context, null).apply {
                    tag = "wrapper_children"
                    orientation = LinearLayout.VERTICAL
                }
            if (isQsbEnabled) {
                loadWidgets()
            }

            return mWrapper
        }

        override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
            if (isQsbEnabled) {
                mWidgetHost.startListening()
            }

            super.onViewCreated(view, savedInstanceState)
        }

        override fun onDestroyView() {
            mWidgetHost.stopListening()
            super.onDestroyView()
        }

        private fun loadWidgets() {
            if (!initialWidgetsAdded) {
                val oldWidgets = mWidgetHost.appWidgetIds
                if (oldWidgets.isEmpty()) {
                    mWidgetHost.deleteHost()
                    DefaultWidgets.defaultWidgets.forEach {
                        try {
                            bindWidget(it)
                        } catch (e: Exception) {
                            Logger.e(TAG, "Could not add widget ${it.flattenToString()}")
                        }
                    }
                } else {
                    rebindWidgets(true)
                }

                disableComponent(context, DefaultWidgets.oldWeatherWidget)
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

        private fun rebindWidgets(backup: Boolean = false) {
            mWrapper.removeAllViews()
            if (!backup) {

                val dbWidgets =
                    widgetsDbHelper.getWidgets().apply {
                        sortedBy { it.position }
                        forEach { addView(it.widgetId) }
                    }

                // Remove all widgets not present in db
                mWidgetHost.appWidgetIds
                    .filter { id -> dbWidgets.all { info -> info.widgetId != id } }
                    .forEach { mWidgetHost.deleteAppWidgetId(it) }
            } else {
                if (mOldWidgets.isNotEmpty()) {
                    mOldWidgets
                        .filter { mWidgetHost.appWidgetIds.contains(it.id) }
                        .sortedWith(
                            compareBy(BlissDbUtils.WidgetItems::order, BlissDbUtils.WidgetItems::id)
                        )
                        .forEach { addView(it.id, true) }
                } else {
                    mWidgetHost.appWidgetIds.sorted().forEach { addView(it, true) }
                }
            }
        }

        private fun bindWidget(provider: ComponentName) {
            val widgetId = mWidgetHost.allocateAppWidgetId()
            val isWidgetBound = mWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)

            if (!isWidgetBound) {
                mWidgetHost.deleteAppWidgetId(widgetId)
                Logger.e(TAG, "Could not add widget ${provider.flattenToString()}")
                return
            }

            configureWidget(widgetId)
        }

        private fun configureWidget(widgetId: Int) {
            val info = mWidgetManager.getAppWidgetInfo(widgetId)
            if (info != null) {
                if (info.configure != null) {
                    sendIntent(widgetId)
                } else {
                    addView(widgetId)
                }
            } else {
                mWidgetHost.deleteAppWidgetId(id)
            }
        }

        private fun addView(widgetId: Int, backup: Boolean = false) {
            val info = mWidgetManager.getAppWidgetInfo(widgetId)

            if (info != null) {
                val widgetInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(launcher, info)
                mWidgetHost
                    .createView(widgetId, widgetInfo)
                    .apply {
                        id = widgetId
                        layoutTransition = LayoutTransition()
                        setOnLongClickListener {
                            if (
                                (widgetInfo.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL) ==
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
                                -1,
                                opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                            )

                        if (backup) {
                            if (it.appWidgetInfo.provider.equals(DefaultWidgets.oldWeatherWidget)) {
                                mWidgetHost.deleteAppWidgetId(it.id)

                                // Swap with new widget
                                bindWidget(DefaultWidgets.weatherWidget)
                                return
                            }
                            val oldHeight =
                                if (mOldWidgets.isNotEmpty()) {
                                    mOldWidgets
                                        .find { widgetItems -> widgetItems.id == widgetId }
                                        ?.height
                                } else {
                                    0
                                }
                            val minHeight: Int = widgetInfo.minResizeHeight
                            val maxHeight: Int =
                                InvariantDeviceProfile.INSTANCE.get(context)
                                    .getDeviceProfile(context)
                                    .heightPx * 3 / 4
                            val normalisedDifference = (maxHeight - minHeight) / 100

                            if (oldHeight != null && oldHeight > 0) {
                                params.height = minHeight + normalisedDifference * oldHeight
                            } else {
                                params.height = 0
                            }
                            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
                            opts.remove(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                            it.updateAppWidgetOptions(opts)
                        } else {
                            params.height = widgetsDbHelper.getWidgetHeight(it.id) ?: 0
                        }

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
                                params.height
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
                rebindWidgets()
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
