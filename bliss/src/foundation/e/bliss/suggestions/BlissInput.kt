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
package foundation.e.bliss.suggestions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.AttributeSet
import android.view.DragEvent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.BubbleTextView
import com.android.launcher3.ExtendedEditText
import com.android.launcher3.ExtendedEditText.OnBackKeyListener
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.allapps.AllAppsStore.OnUpdateListener
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem
import com.android.launcher3.allapps.search.DefaultAppSearchAlgorithm
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.search.SearchCallback
import com.android.launcher3.testing.shared.ResourceUtils
import foundation.e.bliss.LauncherAppMonitor
import foundation.e.bliss.utils.Logger
import foundation.e.bliss.widgets.SwipeSearchContainer
import java.util.Timer
import java.util.TimerTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@SuppressLint("CheckResult")
class BlissInput(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs), SearchCallback<AdapterItem>, OnUpdateListener, OnBackKeyListener {
    private val mSearchAlgorithm = DefaultAppSearchAlgorithm(context, true)
    private val suggestionProvider by lazy { SearchSuggestionUtil.getSuggestionProvider(context) }
    private val suggestionAdapter by lazy { AutoCompleteAdapter(context) }
    private val idp by lazy { InvariantDeviceProfile.INSTANCE.get(context) }
    private val appUsageStats by lazy { AppUsageStats(context) }
    private val mAppsStore by lazy {
        LauncherAppMonitor.getInstanceNoCreate().launcher.appsView.appsStore
    }

    private var results: SuggestionsResult? = null
    private lateinit var mSearchInput: ExtendedEditText
    private lateinit var mIconGrid: GridLayout
    private lateinit var mAppsLayout: View
    private lateinit var mClearButton: ImageView
    private lateinit var mSuggestionRv: RecyclerView
    private var timer: Timer? = null

    private var previousAppsList: List<AppInfo> = emptyList()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mSearchInput = findViewById(R.id.search_input)!!
        mAppsLayout = (parent.parent as View).findViewById(R.id.used_apps_layout)!!
        mIconGrid = mAppsLayout.findViewById(R.id.suggestedAppGrid)!!
        mClearButton = (mSearchInput.parent as View).findViewById(R.id.clearSuggestions)!!
        mSuggestionRv = mAppsLayout.findViewById(R.id.suggestionRecyclerView)!!

        mSearchInput.apply {
            doOnTextChanged { _, _, _, _ -> timer?.cancel() }

            doAfterTextChanged { text ->
                if (text.isNullOrEmpty()) {
                    clearSearchResult()
                    loadSuggestions()
                    return@doAfterTextChanged
                }

                mClearButton.visibility = View.VISIBLE
                mSearchAlgorithm.cancel(false)
                mSearchAlgorithm.doSearch(text.trim().toString(), this@BlissInput)

                if (text.toString() != results?.queryText) {
                    timer = Timer()
                    timer?.schedule(
                        object : TimerTask() {
                            override fun run() {
                                loadSearchSuggestions(text.toString())
                            }
                        },
                        500
                    )
                }
            }

            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    clearFocus()
                    openSearch(text.toString())
                }
                true
            }

            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    mSearchInput.hideKeyboard()
                } else {
                    mSearchInput.showKeyboard()
                }
            }

            mClearButton.setOnClickListener {
                clearSearchResult()
                loadSuggestions()
            }
        }
        mSearchInput.setOnBackKeyListener(this)

        mSuggestionRv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            suggestionAdapter.mOnSuggestionClick = { openSearch(it) }
            adapter = suggestionAdapter
        }

        mAppsStore.addUpdateListener(this)
    }

    private fun loadSearchSuggestions(query: String) =
        CoroutineScope(Dispatchers.Main).launch {
            val task = async(Dispatchers.IO) { suggestionProvider.query(query) }
            results = task.await()
            results?.let { suggestionAdapter.updateSuggestions(it.networkItems, it.queryText) }
        }

    override fun onDragEvent(event: DragEvent): Boolean {
        // Without this drag/drop apps won't work on API <24.
        // EditTexts seem to interfere with drag/drop.
        return false
    }

    override fun onSearchResult(query: String?, items: ArrayList<AdapterItem>?) {
        if (items.isNullOrEmpty()) {
            clearSearchResult()
            return
        }

        mAppsLayout.clipToOutline = true
        mIconGrid.apply {
            removeAllViews()
            columnCount = idp.numColumns
        }

        items
            .map { it.itemInfo }
            .filter {
                it.componentName != null &&
                    !context.resources
                        .getStringArray(R.array.blacklisted_apps)
                        .contains(it.targetPackage)
            }
            .forEachIndexed { index, it ->
                if (index >= idp.numColumns) return

                mIconGrid.addView(createAppView(it), index)
            }
    }

    private fun createAppView(info: AppInfo): BubbleTextView {
        val width =
            idp.getDeviceProfile(context).availableWidthPx -
                ResourceUtils.pxFromDp(48f, context.resources.displayMetrics)
        val padding = ((width / idp.numColumns) - idp.iconBitmapSize) / 2
        return (LayoutInflater.from(context).inflate(R.layout.app_icon, null) as BubbleTextView)
            .apply {
                tag = info
                applyFromApplicationInfo(info)
                setForceHideDot(true)
                setWidth(width / idp.numColumns)
                setTextColor(Color.WHITE)
                setCenterVertically(false)
                setPaddingRelative(padding, 0, padding, 0)
                setOnClickListener(
                    LauncherAppMonitor.getInstanceNoCreate().launcher.itemOnClickListener
                )
            }
    }

    private fun loadSuggestions(forcedLoad: Boolean = true) {
        val currentAppsList = mAppsStore.apps.toList()
        if (currentAppsList == previousAppsList && !forcedLoad) {
            // No changes in the app list, so return
            return
        }

        // Update previousAppsList with the current list
        previousAppsList = currentAppsList

        Logger.i(TAG, "Apps List Size ${currentAppsList.size}")

        mIconGrid.removeAllViews()
        if (currentAppsList.isNotEmpty()) {
            val usageStats = appUsageStats.usageStats

            if (usageStats.isNotEmpty()) {
                usageStats
                    .mapNotNull { pkg ->
                        currentAppsList.find { it.targetPackage == pkg.packageName }
                    }
                    .take(idp.numColumns)
                    .forEachIndexed { index, it -> mIconGrid.addView(createAppView(it), index) }
            }
        }
    }

    private fun openSearch(query: String) {
        if (query.isEmpty()) return

        val launcher = Launcher.getLauncher(context)
        val intent = Intent(Intent.ACTION_VIEW, SearchSuggestionUtil.getUriForQuery(context, query))

        launcher.startActivitySafely(null, intent, null)
    }

    override fun clearSearchResult() {
        mIconGrid.removeAllViews()
        suggestionAdapter.updateSuggestions(emptyList(), "")
        mSearchInput?.text?.clear()
        mClearButton.visibility = View.GONE
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        // Clear search result when the search bar is hidden
        // on down swipe in home screen
        if (visibility != View.VISIBLE) {
            clearSearchResult()
            loadSuggestions()
            clearFocus()
        } else {
            val swipeParent = parent.parent.parent
            if (swipeParent is SwipeSearchContainer && swipeParent.visibility == View.VISIBLE) {
                mSearchInput.requestFocus()
            }
            super.onVisibilityChanged(changedView, visibility)
        }
    }

    override fun onAppsUpdated() {
        loadSuggestions(false)
    }

    override fun onBackKey(): Boolean {
        val launcher = LauncherAppMonitor.getInstanceNoCreate().launcher
        if (launcher.swipeSearchContainer.visibility == VISIBLE) {
            launcher.hideSwipeSearchContainer()
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "BlissInput"
    }
}
