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

import android.animation.LayoutTransition
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
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
import androidx.core.view.forEach
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
import com.android.launcher3.widget.util.WidgetSizes
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.LauncherAppMonitorCallback
import foundation.e.bliss.utils.BlissDbUtils
import foundation.e.bliss.utils.Logger
import foundation.e.bliss.utils.ObservableList
import foundation.e.bliss.utils.disableComponent
import foundation.e.bliss.widgets.BlissAppWidgetHost.Companion.REQUEST_CONFIGURE_APPWIDGET
import foundation.e.bliss.widgets.DefaultWidgets.defaultWidgets
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Suppress("Deprecation", "NewApi")
class WidgetContainer(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val mLauncher by lazy { Launcher.getLauncher(context) }

    private lateinit var mManageWidgetLayout: LinearLayout
    private lateinit var mRemoveWidgetLayout: FrameLayout
    private lateinit var mResizeContainer: RelativeLayout
    private lateinit var mWidgetLinearLayout: LinearLayout
    private lateinit var mWrapper: LinearLayout

    private val mResizeContainerRect = Rect()
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

        mManageWidgetLayout = findViewById(R.id.manage_widget_parent)!!
        mRemoveWidgetLayout = findViewById(R.id.remove_widget_parent)!!
        mResizeContainer = findViewById(R.id.widget_resizer_container)!!
        mWidgetLinearLayout = findViewById(R.id.widget_linear_layout)!!

        findViewById<Button>(R.id.manage_widgets)!!.setOnClickListener {
            WidgetsFullSheet.show(mLauncher, true, true)
        }

        findViewById<Button>(R.id.remove_widgets)!!.setOnClickListener {
            val intent =
                Intent(context, WidgetsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        mWrapper =
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

        updatePadding()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updatePadding()
    }

    private fun updatePadding() {
        val insets = mLauncher.workspace.rootWindowInsets.getInsets(WindowInsets.Type.systemBars())
        if (::mResizeContainer.isInitialized) {
            mResizeContainer.apply {
                val layoutParams = this.layoutParams as LayoutParams
                layoutParams.bottomMargin = mInsetPadding + (insets.bottom)
                this.layoutParams = layoutParams
            }
        }

        if (::mWidgetLinearLayout.isInitialized) {
            mWidgetLinearLayout.apply {
                setPadding(
                    this.paddingLeft,
                    mInsetPadding + (insets.top),
                    this.paddingRight,
                    (insets.bottom),
                )
            }
        }

        if (::mManageWidgetLayout.isInitialized) {
            mManageWidgetLayout.apply {
                setPadding(
                    this.paddingLeft,
                    mInsetPadding,
                    this.paddingRight,
                    (insets.top - mInsetPadding),
                )
            }
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

    fun updateWidgets() {
        if (::mWrapper.isInitialized) {
            val widgetDbHelper = WidgetsDbHelper.getInstance(context)
            val widgetManager = AppWidgetManager.getInstance(context)

            mWrapper.forEach {
                val height = widgetDbHelper.getWidgetHeight(it.id) ?: 0

                val info = (it as AppWidgetHostView).appWidgetInfo
                val opts =
                    WidgetSizes.getWidgetSizeOptions(
                        mLauncher,
                        info.provider,
                        mLauncher.deviceProfile.inv.numColumns,
                        mLauncher.deviceProfile.inv.numRows
                    )

                if (height > 0) {
                    opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                    opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
                }
                val blacklistedComponents =
                    mLauncher.resources.getStringArray(R.array.blacklisted_widget_options)
                if (!blacklistedComponents.contains(info.provider.className)) {
                    widgetManager.updateAppWidgetOptions(it.appWidgetId, opts)
                }
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
                    if (!::widgetsDbHelper.isInitialized) {
                        return
                    }
                    val widgets =
                        widgetsDbHelper.getWidgets().filter {
                            it.component.packageName == packageName
                        }
                    if (packageName != null && widgets.isNotEmpty()) {
                        widgets.map { it.widgetId }.forEach { widgetsDbHelper.delete(it) }
                        rebindWidgets()
                    }
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
                        var opts = mWidgetManager.getAppWidgetOptions(it.appWidgetId)
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

                        opts =
                            WidgetSizes.getWidgetSizeOptions(
                                launcher,
                                info.provider,
                                launcher.deviceProfile.inv.numColumns,
                                launcher.deviceProfile.inv.numRows
                            )

                        if (params.height > 0) {
                            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, params.height)
                            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, params.height)
                        }

                        val blacklistedComponents =
                            launcher.resources.getStringArray(R.array.blacklisted_widget_options)
                        if (!blacklistedComponents.contains(info.provider.className)) {
                            mWidgetManager.updateAppWidgetOptions(it.appWidgetId, opts)
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
            mWidgetHost.startAppWidgetConfigureActivityForResult(
                launcher,
                widgetId,
                0,
                REQUEST_CONFIGURE_APPWIDGET,
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

            @JvmStatic
            fun onWidgetAdded(componentName: ComponentName) {
                defaultWidgets.add(componentName)
            }
        }
    }
}
