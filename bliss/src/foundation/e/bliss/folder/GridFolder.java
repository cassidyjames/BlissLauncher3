/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.folder;

import static com.android.launcher3.LauncherState.HOTSEAT_ICONS;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.OVERVIEW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Hotseat;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.AlphaUpdateListener;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.ScrimView;

import foundation.e.bliss.blur.BlurBackgroundView;

public class GridFolder extends Folder {
    private static final int MIN_CONTENT_DIMEN = 5;
    private final Launcher mLauncher;
    public View mFolderTab;
    public int mFolderTabHeight;
    public GridFolderPage mGridFolderPage;
    private LauncherState mLastStateBeforeOpen = NORMAL;
    private boolean mNeedResetState = false;

    public GridFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = mLauncherDelegate.getLauncher();
    }

    @SuppressLint("InflateParams")
    public static <T extends Context & ActivityContext> Folder fromXml(T activityContext) {
        return (Folder) LayoutInflater.from(activityContext).cloneInContext(activityContext)
                .inflate(R.layout.grid_folder_icon_normalized, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mGridFolderPage = findViewById(R.id.grid_folder_page);

        mFolderTab = findViewById(R.id.folder_tab);
        mFolderName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        mFooterHeight = getResources().getDimensionPixelSize(R.dimen.grid_folder_footer_height);

        DeviceProfile grid = mActivityContext.getDeviceProfile();
        int cellIconGap = (grid.folderCellWidthPx - mActivityContext.getDeviceProfile().iconSizePx);
        mContent.setPadding(cellIconGap, cellIconGap, cellIconGap, cellIconGap);

        int measureSpec = MeasureSpec.UNSPECIFIED;
        mFolderTab.measure(measureSpec, measureSpec);
        mFolderTabHeight = mFolderTab.getMeasuredHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = getContentAreaWidth();
        int contentHeight = getContentAreaHeight();

        int contentAreaWidthSpec = MeasureSpec.makeMeasureSpec(contentWidth, MeasureSpec.EXACTLY);

        mFolderTab.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(mFolderTabHeight, MeasureSpec.EXACTLY));

        mGridFolderPage.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));

        mContent.setFixedSize(contentWidth, contentHeight);
        mContent.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));

        mFooter.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(mFooterHeight, MeasureSpec.EXACTLY));

        int folderWidth = getPaddingLeft() + getPaddingRight() + contentWidth;
        int folderHeight = getFolderHeight();
        setMeasuredDimension(folderWidth, folderHeight);
    }

    private int getContentAreaWidth() {
        return Math.max(mContent.getDesiredWidth(), MIN_CONTENT_DIMEN);
    }

    protected int getContentAreaHeight() {
        DeviceProfile grid = mActivityContext.getDeviceProfile();
        int maxContentAreaHeight = grid.availableHeightPx - grid.getTotalWorkspacePadding().y
                + (grid.isVerticalBarLayout() ? 0 : grid.hotseatBarSizePx);
        int height = Math.min(maxContentAreaHeight, mContent.getDesiredHeight() + mFooterHeight);
        return Math.max(height, MIN_CONTENT_DIMEN);
    }

    private int getFolderWidth() {
        return getPaddingLeft() + getPaddingRight() + mContent.getDesiredWidth();
    }

    private int getFolderHeight() {
        return getPaddingTop() + getPaddingBottom() + mContent.getDesiredHeight() + mFolderTabHeight + mFooterHeight;
    }

    @Override
    protected void centerAboutIcon() {
        DeviceProfile grid = mActivityContext.getDeviceProfile();
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();

        int width = getFolderWidth();
        int height = getFolderHeight();
        int insetsTop = grid.getInsets().top;

        lp.width = width;
        lp.height = height;
        lp.x = (grid.availableWidthPx - width) / 2;
        if (grid.isVerticalBarLayout()) {
            lp.y = grid.getTotalWorkspacePadding().y / 2 + insetsTop;
        } else {
            int minTopHeight = insetsTop + grid.dropTargetBarSizePx;
            lp.y = Math.max((grid.availableHeightPx - height) / 2, minTopHeight);
        }
    }

    public void setNeedResetState(boolean isReset) {
        mNeedResetState = isReset;
    }

    private boolean isCanRestoredState(LauncherState state) {
        return state == NORMAL || state == OVERVIEW;
    }

    @Override
    public void onFolderOpenStart() {
        mLastStateBeforeOpen = mLauncher.getStateManager().getState();
        if (!mLauncher.isInState(NORMAL)) {
            mLauncher.getStateManager().goToState(LauncherState.NORMAL, false);
            if (isCanRestoredState(mLastStateBeforeOpen)) {
                mNeedResetState = true;
            }
        }

        showOrHideDesktop(mLauncher, true);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (!mLauncher.isInState(mLastStateBeforeOpen)) {
            if (mLauncher.getDragController().isDragging()) {
                mNeedResetState = false;
            }

            if (mNeedResetState) {
                mLauncher.getStateManager().goToState(mLastStateBeforeOpen, false);
            }

            if (!mLauncher.isInState(NORMAL) && mNeedResetState) {
                animate = false;
            }

        } else if (mNeedResetState) {
            animate = false;
        }
        mNeedResetState = false;

        super.handleClose(animate);
    }

    @Override
    protected void onFolderCloseComplete() {
        if (getOpen(mLauncher) == null) {
            showOrHideDesktop(mLauncher, false);
        }
    }

    private void showOrHideDesktop(Launcher launcher, boolean hide) {
        float hotseatIconsAlpha = hide ? 0 : 1;
        float pageIndicatorAlpha = hide ? 0 : 1;
        LauncherState state = launcher.getStateManager().getState();
        if (state == OVERVIEW) {
            hotseatIconsAlpha = (state.getVisibleElements(launcher) & HOTSEAT_ICONS) != 0 ? 1 : 0;
            pageIndicatorAlpha = 0;
        }

        Workspace<?> workspace = launcher.getWorkspace();
        if (workspace != null) {
            workspace.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
            if (workspace.getPageIndicator() != null) {
                workspace.getPageIndicator().setAlpha(pageIndicatorAlpha);
                AlphaUpdateListener.updateVisibility(workspace.getPageIndicator());
                if (!hide && launcher.isInState(LauncherState.SPRING_LOADED)) {
                    workspace.showPageIndicatorAtCurrentScroll();
                }
            }
        }

        Hotseat hotseat = launcher.getHotseat();
        if (hotseat != null) {
            hotseat.setAlpha(hotseatIconsAlpha);
            AlphaUpdateListener.updateVisibility(hotseat);
        }

        ScrimView scrimView = launcher.findViewById(R.id.scrim_view);
        if (scrimView != null) {
            scrimView.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        }

        BlurBackgroundView blur = launcher.mBlurLayer;
        if (blur != null) {
            blur.setAlpha(hide ? 1 : 0);
            AlphaUpdateListener.updateVisibility(blur);
        }

        showOrHideQsb(launcher, hide);
    }

    public void showOrHideQsb(Launcher launcher, boolean hide) {
        if (launcher.getAppsView() != null) {
            View qsb = launcher.getWorkspace().getHotseat();
            if (qsb != null) {
                qsb.setVisibility(hide ? INVISIBLE : VISIBLE);
            }
        }
    }

    @Override
    public void updateFolderOnAnimate(boolean isOpening) {
        mFolderTab.setVisibility(isOpening ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getUnusedOffsetYOnAnimate(boolean isOpening) {
        LinearLayout.LayoutParams tabLp = (LinearLayout.LayoutParams) mFolderTab.getLayoutParams();
        return mFolderTabHeight + tabLp.bottomMargin;

    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        return windowInsets;
    }
}
