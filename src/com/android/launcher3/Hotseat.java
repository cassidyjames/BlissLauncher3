/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.launcher3.folder.Folder;

import androidx.annotation.NonNull;

import foundation.e.bliss.blur.BlurViewDelegate;
import foundation.e.bliss.blur.BlurWallpaperProvider;
import foundation.e.bliss.blur.OffsetParent;
import foundation.e.bliss.folder.GridFolder;

/**
 * View class that represents the bottom row of the home screen.
 */
public class Hotseat extends CellLayout implements Insettable, OffsetParent {

    // Ratio of empty space, qsb should take up to appear visually centered.
    public static final float QSB_CENTER_FACTOR = .325f;

    private final OffsetParent.OffsetParentDelegate offsetParentDelegate =
            new OffsetParent.OffsetParentDelegate();
    public final BlurViewDelegate mBlurDelegate;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mHasVerticalHotseat;
    private Workspace<?> mWorkspace;
    private boolean mSendTouchToWorkspace;

    private final View mQsb;
    public boolean drawBlur;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mQsb = LayoutInflater.from(context).inflate(R.layout.search_container_hotseat, this, false);
        mBlurDelegate = new BlurViewDelegate(this, BlurWallpaperProvider.blurConfigDock, attrs);
        drawBlur = true;
        setWillNotDraw(false);
        addView(mQsb);
    }

    /**
     * Returns orientation specific cell X given invariant order in the hotseat
     */
    public int getCellXFromOrder(int rank) {
        return mHasVerticalHotseat ? 0 : rank;
    }

    /**
     * Returns orientation specific cell Y given invariant order in the hotseat
     */
    public int getCellYFromOrder(int rank) {
        return mHasVerticalHotseat ? (getCountY() - (rank + 1)) : 0;
    }

    public void resetLayout(boolean hasVerticalHotseat) {
        removeAllViewsInLayout();
        mHasVerticalHotseat = hasVerticalHotseat;
        DeviceProfile dp = mActivity.getDeviceProfile();
        resetCellSize(dp);
        if (hasVerticalHotseat) {
            setGridSize(1, dp.numShownHotseatIcons);
        } else {
            setGridSize(dp.numShownHotseatIcons, 1);
        }
    }

    @Override
    public void setInsets(Rect insets) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile grid = mActivity.getDeviceProfile();

        if (grid.isVerticalBarLayout()) {
            mQsb.setVisibility(View.GONE);
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (grid.isSeascape()) {
                lp.gravity = Gravity.LEFT;
                lp.width = grid.hotseatBarSizePx + insets.left;
            } else {
                lp.gravity = Gravity.RIGHT;
                lp.width = grid.hotseatBarSizePx + insets.right;
            }
        } else {
            mQsb.setVisibility(View.VISIBLE);
            lp.gravity = Gravity.BOTTOM;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = grid.hotseatBarSizePx;
        }

        Rect padding = grid.getHotseatLayoutPadding(getContext());
        setPadding(padding.left, padding.top, padding.right, padding.bottom);
        setLayoutParams(lp);
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    public void setWorkspace(Workspace<?> w) {
        mWorkspace = w;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We allow horizontal workspace scrolling from within the Hotseat. We do this by delegating
        // touch intercept the Workspace, and if it intercepts, delegating touch to the Workspace
        // for the remainder of the this input stream.
        int yThreshold = getMeasuredHeight() - getPaddingBottom();
        if (mWorkspace != null && ev.getY() <= yThreshold) {
            mSendTouchToWorkspace = mWorkspace.onInterceptTouchEvent(ev);
            return mSendTouchToWorkspace;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBlurDelegate != null && drawBlur) {
            mBlurDelegate.draw(canvas);
        }
        super.onDraw(canvas);
    }

    public void setBlurAlpha(int alpha) {
        if (alpha > 255) {
            alpha = 255;
        } else if (alpha < 0) {
            alpha = 0;
        }
        mBlurDelegate.setBlurAlpha(alpha);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // See comment in #onInterceptTouchEvent
        if (mSendTouchToWorkspace) {
            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mSendTouchToWorkspace = false;
            }
            return mWorkspace.onTouchEvent(event);
        }
        // Always let touch follow through to Workspace.
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        DeviceProfile dp = mActivity.getDeviceProfile();
        mQsb.measure(MeasureSpec.makeMeasureSpec(dp.hotseatQsbWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp.hotseatQsbHeight, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int qsbMeasuredWidth = mQsb.getMeasuredWidth();
        int left;
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isQsbInline) {
            int qsbSpace = dp.hotseatBorderSpace;
            left = Utilities.isRtl(getResources()) ? r - getPaddingRight() + qsbSpace
                    : l + getPaddingLeft() - qsbMeasuredWidth - qsbSpace;
        } else {
            left = (r - l - qsbMeasuredWidth) / 2;
        }
        int right = left + qsbMeasuredWidth;

        int bottom = b - t - dp.getQsbOffsetY();
        int top = bottom - dp.hotseatQsbHeight;
        mQsb.layout(left, top, right, bottom);

        // Setting name is hardcoded here to prevent recompilation of
        // framework jar for studio build
        Settings.Secure.putInt(getContext().getContentResolver(),
                "launcher_dock_width",
                dp.isVerticalBarLayout()
                        ? getWidth() - dp.getExtraStatusBarPadding()
                        : 0);
    }

    /**
     * Sets the alpha value of just our ShortcutAndWidgetContainer.
     */
    public void setIconsAlpha(float alpha) {
        getShortcutsAndWidgets().setAlpha(alpha);
    }

    /**
     * Sets the alpha value of just our QSB.
     */
    public void setQsbAlpha(float alpha) {
        mQsb.setAlpha(alpha);
    }

    public float getIconsAlpha() {
        return getShortcutsAndWidgets().getAlpha();
    }

    /**
     * Returns the QSB inside hotseat
     */
    public View getQsb() {
        return mQsb;
    }

    public Workspace<?> getWorkspace() {
        return mWorkspace;
    }

    // To reduce notifyOffsetChanged() calls
    public void setForcedTranslationXY(float translationX, float translationY){
        super.setTranslationX(translationX);
        super.setTranslationY(translationY);
        offsetParentDelegate.notifyOffsetChanged();
    }

    @Override
    public float getOffsetX() {
        return getTranslationX();
    }

    @Override
    public float getOffsetY() {
        return getTranslationY();
    }

    @Override
    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);
        offsetParentDelegate.notifyOffsetChanged();
    }

    @Override
    public void setTranslationY(float translationY) {
        offsetParentDelegate.notifyOffsetChanged();
    }

    @Override
    public boolean getNeedWallpaperScroll() {
        return true;
    }

    @Override
    public void addOnOffsetChangeListener(@NonNull OnOffsetChangeListener listener) {
        offsetParentDelegate.addOnOffsetChangeListener(listener);
    }

    @Override
    public void removeOnOffsetChangeListener(@NonNull OnOffsetChangeListener listener) {
        offsetParentDelegate.removeOnOffsetChangeListener(listener);
    }

    @Override
    public void setAlpha(float alpha) {
        Folder folder = Folder.getOpen(mActivity);
        if (folder instanceof GridFolder) {
            GridFolder gridFolder = (GridFolder) folder;
            if (gridFolder.isAnimating()) {
                super.setAlpha(alpha);
            }
        } else {
            super.setAlpha(alpha);
        }
    }
}