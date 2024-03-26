/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.pageindicators

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import com.android.launcher3.DeviceProfile
import com.android.launcher3.Insettable
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.pageindicators.PageIndicatorDots
import com.android.launcher3.util.Themes
import com.android.quickstep.SysUINavigationMode

class WorkspacePageIndicatorDots
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    PageIndicatorDots(context, attrs, defStyleAttr), Insettable {

    var shouldAutoHide: Boolean = false
    private val mLauncher = Launcher.getLauncher(context)

    init {
        mCirclePaint.color = Themes.getAttrColor(getContext(), R.attr.workspaceTextColor)
    }

    override fun onDraw(canvas: Canvas) {
        if (mNumPages <= 1) {
            return
        }
        super.onDraw(canvas)
    }

    override fun setInsets(insets: Rect) {
        val grid: DeviceProfile = mLauncher.deviceProfile
        val isGestureMode =
            SysUINavigationMode.getMode(context) == SysUINavigationMode.Mode.NO_BUTTON

        val padding = grid.workspacePadding
        updateLayoutParams<MarginLayoutParams> {
            leftMargin = padding.left + grid.workspaceCellPaddingXPx
            rightMargin = padding.right + grid.workspaceCellPaddingXPx
            bottomMargin =
                if (isGestureMode) {
                    padding.bottom
                } else {
                    padding.bottom +
                        grid.hotseatBarBottomPaddingPx +
                        grid.workspacePageIndicatorHeight
                }
        }
    }

    // Place Holder
    fun pauseAnimations() {}
    fun skipAnimationsToEnd() {}
}
