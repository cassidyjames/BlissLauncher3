/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
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
import foundation.e.bliss.utils.OnDataChangedListener
import foundation.e.bliss.utils.disableComponent
import foundation.e.bliss.widgets.BlissAppWidgetHost.Companion.REQUEST_CONFIGURE_APPWIDGET
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@Suppress("Deprecation", "NewApi")
class WidgetContainer(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs), OnDataChangedListener {
    private val mLauncher by lazy { Launcher.getLauncher(context) }

    private lateinit var mManageWidgetLayout: LinearLayout
    private lateinit var mRemoveWidgetLayout: FrameLayout
    private lateinit var mResizeContainer: RelativeLayout
    private lateinit var mWidgetLinearLayout: LinearLayout
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mWidgetAdapter: StaggeredAdapter

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

        mManageWidgetLayout = findViewById(R.id.manage_widget_parent)
        mRemoveWidgetLayout = findViewById(R.id.remove_widget_parent)
        mResizeContainer = findViewById(R.id.widget_resizer_container)
        mWidgetLinearLayout = findViewById(R.id.widget_linear_layout)

        findViewById<Button>(R.id.manage_widgets).setOnClickListener {
            WidgetsFullSheet.show(mLauncher, true, true)
        }

        findViewById<Button>(R.id.remove_widgets).setOnClickListener {
            val intent =
                Intent(context, WidgetsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        mRecyclerView = findViewWithTag("wrapper_children")
        mWidgetAdapter = mRecyclerView.adapter as StaggeredAdapter
        mWidgetAdapter.addOnDataChangedListener(this)
        handleRemoveButtonVisibility(mWidgetAdapter.getWidgets().size)
        updatePadding()
        updateRecyclerViewHeight()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updatePadding()
        val spanCount =
            if (
                mLauncher != null &&
                    (mLauncher.deviceProfile.isTablet || mLauncher.deviceProfile.isLandscape)
            )
                2
            else 1
        mRecyclerView.layoutManager =
            NoScrollStaggeredLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL)
    }

    private fun updateRecyclerViewHeight() {
        mRecyclerView.post {
            val widgetFragment = mLauncher.fragmentManager.findFragmentByTag("qsb_view")
            (widgetFragment as WidgetFragment).setRecyclerViewHeight()
        }
    }

    private fun updatePadding() {
        val insets = mLauncher.workspace.mInsets
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
        if (::mRecyclerView.isInitialized) {
            val widgetDbHelper = WidgetsDbHelper.getInstance(context)
            val widgetManager = AppWidgetManager.getInstance(context)

            mWidgetAdapter.getWidgets().forEach {
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
                    widgetManager.updateAppWidgetOptions(it.id, opts)
                }
            }
            mWidgetAdapter.notifyListeners()
        }
    }

    class StaggeredAdapter : RecyclerView.Adapter<StaggeredAdapter.WidgetViewHolder>() {

        private var widgets = mutableListOf<View>()
        private val listeners = mutableListOf<OnDataChangedListener>()

        fun addOnDataChangedListener(listener: OnDataChangedListener) {
            listeners.add(listener)
        }

        fun removeOnDataChangedListener(listener: OnDataChangedListener) {
            listeners.remove(listener)
        }

        @SuppressLint("NotifyDataSetChanged")
        fun notifyWidgetsChanged() {
            notifyDataSetChanged()
            notifyListeners()
        }

        fun notifyListeners() {
            listeners.forEach { it.onDataChanged() }
        }

        fun setWidgets(widgets: MutableList<View>) {
            this.widgets.clear()
            this.widgets = widgets
            notifyWidgetsChanged()
        }

        fun addWidget(widget: View) {
            widgets.add(widget)
            notifyWidgetsChanged()
        }

        fun getWidgets(): MutableList<View> {
            return widgets
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.widget_item_layout, parent, false)
            return WidgetViewHolder(view)
        }

        override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
            val widget = widgets[position]
            holder.bind(widget)
        }

        override fun getItemCount(): Int {
            return widgets.size
        }

        class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(view: View) {
                (itemView as FrameLayout).removeAllViews()
                if (view.parent != null) {
                    (view.parent as ViewGroup).removeAllViews()
                }
                (itemView as FrameLayout).addView(view)
            }
        }
    }

    /** A fragment to display the default widgets. */
    class WidgetFragment : FragmentWithPreview() {
        private lateinit var recyclerView: RecyclerView
        private lateinit var widgetObserver: Disposable
        private lateinit var widgetsDbHelper: WidgetsDbHelper
        private lateinit var widgetsAdapter: StaggeredAdapter

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
            widgetsAdapter = StaggeredAdapter()
            val spanCount =
                if (
                    launcher != null &&
                        (launcher.deviceProfile.isTablet || launcher.deviceProfile.isLandscape)
                )
                    2
                else 1
            recyclerView =
                RecyclerView(context, null).apply {
                    tag = "wrapper_children"
                    layoutManager =
                        NoScrollStaggeredLayoutManager(
                            spanCount,
                            StaggeredGridLayoutManager.VERTICAL
                        )
                    adapter = widgetsAdapter
                    isNestedScrollingEnabled = false
                }
            if (isQsbEnabled) {
                loadWidgets()
            }

            return recyclerView
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

        fun rebindWidgets(backup: Boolean = false) {
            widgetsAdapter.setWidgets(mutableListOf())
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

                        widgetsAdapter.addWidget(it)

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
                                widgetsAdapter.getWidgets().indexOf(it),
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

        fun setRecyclerViewHeight() {
            val layoutManager = recyclerView.layoutManager as StaggeredGridLayoutManager
            val adapter = recyclerView.adapter as StaggeredAdapter

            adapter.let {
                recyclerView.viewTreeObserver.addOnGlobalLayoutListener {
                    // Our maximum column span is 2.
                    // Column span may change on rotation for phones
                    // Hardcode 2 to workaround
                    val totalHeight = IntArray(2) { 0 }

                    for (i in 0 until adapter.itemCount) {
                        if (layoutManager.spanCount == 1) {
                            totalHeight[0] += adapter.getWidgets()[i].measuredHeight
                            continue
                        }
                        val viewHolder = recyclerView.findViewHolderForAdapterPosition(i)
                        viewHolder?.itemView?.let { itemView ->
                            val layoutParams =
                                itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
                            val spanIndex = layoutParams.spanIndex
                            totalHeight[spanIndex] += itemView.measuredHeight
                        }
                    }

                    val maxHeight = totalHeight.maxOrNull() ?: 0
                    val padding = recyclerView.paddingTop + recyclerView.paddingBottom
                    val layoutParams = recyclerView.layoutParams
                    layoutParams.height = maxHeight + padding
                    recyclerView.layoutParams = layoutParams
                }
            }
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

    override fun onDataChanged() {
        if (::mWidgetAdapter.isInitialized) {
            handleRemoveButtonVisibility(mWidgetAdapter.getWidgets().size)
        }
    }
}
