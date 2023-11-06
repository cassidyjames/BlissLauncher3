/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.folder;

import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.OVERVIEW;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.android.launcher3.Alarm;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Hotseat;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.OnAlarmListener;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.ClipPathView;
import com.android.launcher3.views.ScrimView;

import java.util.concurrent.atomic.AtomicInteger;

import foundation.e.bliss.blur.BlurBackgroundView;

public class GridFolder extends Folder implements OnAlarmListener {
    private static final int MIN_CONTENT_DIMEN = 5;
    private final Launcher mLauncher;
    private final Alarm wobbleExpireAlarm = new Alarm();
    public View mFolderTab;
    public int mFolderTabHeight;
    public FrameLayout mGridFolderPage;
    private LauncherState mLastStateBeforeOpen = NORMAL;
    private boolean mNeedResetState = false;

    private boolean isFolderWobbling = false;
    private boolean isAnimating = false;

    public GridFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = mLauncherDelegate.getLauncher();
        wobbleExpireAlarm.setOnAlarmListener(this);
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
        mFolderName.setSelectAllOnFocus(false);
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
        int contentAreaHeightSpec = MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY);

        mContent.setFixedSize(contentWidth, contentHeight);
        mContent.measure(contentAreaWidthSpec, contentAreaHeightSpec);

        mFolderTab.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(mFolderTabHeight, MeasureSpec.EXACTLY));

        mGridFolderPage.measure(contentAreaWidthSpec, contentAreaHeightSpec);

        mFooter.measure(contentAreaWidthSpec, MeasureSpec.makeMeasureSpec(mFooterHeight, MeasureSpec.EXACTLY));

        int folderWidth = getPaddingLeft() + getPaddingRight() + contentWidth;
        int folderHeight = getFolderHeight();
        setMeasuredDimension(folderWidth, folderHeight);
    }

    @Override
    public Drawable getBackground() {
        return mGridFolderPage.getBackground();
    }

    private int getContentAreaWidth() {
        return Math.max(mContent.getDesiredWidth(), MIN_CONTENT_DIMEN);
    }

    protected int getContentAreaHeight() {
        DeviceProfile grid = mActivityContext.getDeviceProfile();
        int maxContentAreaHeight = grid.availableHeightPx - grid.getTotalWorkspacePadding().y
                + (grid.isVerticalBarLayout() ? 0 : grid.hotseatBarSizePx);
        int height = Math.min(maxContentAreaHeight, getFolderHeight());
        return Math.max(height, MIN_CONTENT_DIMEN);
    }

    private int getFolderWidth() {
        return getPaddingLeft() + getPaddingRight() + mContent.getDesiredWidth();
    }

    private int getFolderHeight() {
        return mContent.getDesiredHeight() + mFooterHeight;
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
        if (mLauncher.getWorkspace().isWobbling()) {
            wobbleFolder(true);
        } else if (isFolderWobbling) {
            wobbleFolder(false);
        }
    }

    public boolean isFolderWobbling() {
        return isFolderWobbling;
    }

    public boolean isAnimating() {
        return isAnimating;
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

        if (isFolderWobbling()) {
            wobbleFolder(false);
        }

        showOrHideDesktop(mLauncher, false);

        super.handleClose(animate);
    }

    @Override
    protected void onFolderCloseComplete() {
        if (getOpen(mLauncher) == null) {
            showOrHideDesktop(mLauncher, false);
        }
    }

    @Override
    public boolean startDrag(View v, DragOptions options) {
        if (!isFolderWobbling) {
            wobbleFolder(true);
        }
        Object tag = v.getTag();
        if (tag instanceof WorkspaceItemInfo) {
            v.clearAnimation();
        }
        return super.startDrag(v, options);
    }

    @Override
    public void onDragEnd() {
        if (isFolderWobbling) {
            wobbleFolder(true);
        }
        super.onDragEnd();
    }

    private void showOrHideDesktop(Launcher launcher, boolean hide) {
        AnimatorSet set = new AnimatorSet();

        Workspace<?> workspace = launcher.getWorkspace();
        if (workspace != null) {
            play(set, getAnimator(workspace, 1f, 0f, hide));
            if (workspace.getPageIndicator() != null) {
                play(set, getAnimator(workspace.getPageIndicator(), 1f, 0f, hide));
                if (!hide && launcher.isInState(LauncherState.SPRING_LOADED)) {
                    workspace.showPageIndicatorAtCurrentScroll();
                }
            }
        }

        Hotseat hotseat = launcher.getHotseat();
        if (hotseat != null) {
            play(set, getAnimator(hotseat, 1f, 0f, hide));
            View qsb = hotseat.getQsb();
            if (qsb != null) {
                play(set, getAnimator(qsb, 1f, 0f, hide));
            }

        }

        ScrimView scrimView = launcher.findViewById(R.id.scrim_view);
        if (scrimView != null) {
            play(set, getAnimator(scrimView, 1f, 0f, hide));
        }

        BlurBackgroundView blur = launcher.mBlurLayer;
        if (blur != null) {
            play(set, getAnimator(blur, 0f, 1f, hide));
        }
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimating = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                isAnimating = true;
                if (!hide && hotseat != null) {
                    hotseat.setVisibility(VISIBLE);
                }
            }
        });
        set.start();
    }

    @Override
    public void updateFolderOnAnimate(boolean isOpening) {
        mFolderTab.setVisibility(isOpening ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getUnusedOffsetYOnAnimate(boolean isOpening) {
        return mFolderTabHeight + mFolderTab.getPaddingBottom() + mFooterHeight + mFooter.getPaddingBottom()
                + mContent.getPaddingBottom() + mContent.getPaddingTop() + mContent.getPaddingBottom()
                + mGridFolderPage.getPaddingBottom() + mGridFolderPage.getPaddingTop();

    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        return windowInsets;
    }

    public void wobbleFolder(boolean wobble) {
        wobbleFolder(wobble, false);
    }

    public void wobbleFolder(boolean wobble, boolean excludeDraggingView) {
        if (wobble) {
            isFolderWobbling = true;
            AtomicInteger index = new AtomicInteger();
            ItemInfo draggingViewInfo = mLauncher.getWorkspace().getDragObjectInfo();
            iterateOverItems((info, view) -> {
                view.setOnTouchListener(ItemLongClickListener.INSTANCE_WORKSPACE_WOBBLE);
                if (excludeDraggingView && draggingViewInfo != null) {
                    if (draggingViewInfo instanceof WorkspaceItemInfo && draggingViewInfo.equals(view.getTag())) {
                        return false;
                    }
                }
                index.getAndIncrement();
                if (index.get() % 2 == 0) {
                    view.startAnimation(mLauncher.getWorkspace().getWobbleAnimation());
                } else {
                    view.startAnimation(mLauncher.getWorkspace().getReverseWobbleAnimation());
                }
                if (view instanceof BubbleTextView) {
                    ((BubbleTextView) view).applyUninstallIconState(true);
                }
                return false;
            });
            wobbleExpireAlarm.setAlarm(Workspace.WOBBLE_EXPIRATION_TIMEOUT);
        } else {
            wobbleExpireAlarm.cancelAlarm();
            iterateOverItems((info, view) -> {
                isFolderWobbling = false;
                view.setOnTouchListener(null);
                view.clearAnimation();
                if (view instanceof BubbleTextView) {
                    ((BubbleTextView) view).applyUninstallIconState(false);
                }
                return false;
            });
        }
    }

    @Override
    public void onAlarm(Alarm alarm) {
        if (alarm == wobbleExpireAlarm) {
            wobbleFolder(false);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends View & ClipPathView> T getAnimateObject() {
        return (T) mGridFolderPage;
    }

    private Animator getAnimator(View view, float v1, float v2, boolean hide) {
        return hide
                ? ObjectAnimator.ofFloat(view, View.ALPHA, v1, v2)
                : ObjectAnimator.ofFloat(view, View.ALPHA, v2, v1);
    }

    private void play(AnimatorSet as, Animator a) {
        a.setStartDelay(a.getStartDelay());
        a.setDuration(0);
        as.play(a);
    }
}
