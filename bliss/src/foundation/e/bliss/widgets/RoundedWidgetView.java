/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.android.launcher3.CheckLongPressHelper;
import com.android.launcher3.R;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.LauncherAppWidgetProviderInfo;

import foundation.e.bliss.blur.BlurViewDelegate;
import foundation.e.bliss.blur.BlurWallpaperProvider;

public class RoundedWidgetView extends LauncherAppWidgetHostView {

    private final Path stencilPath = new Path();
    private final float cornerRadius;
    private final CheckLongPressHelper mLongPressHelper;
    private Context mContext;
    private static final String TAG = "RoundedWidgetView";
    private ImageView resizeBorder;

    private OnTouchListener _onTouchListener;
    private OnLongClickListener _longClick;
    private long _down;
    private boolean mChildrenFocused;

    private boolean activated = false;

    private BlurViewDelegate mBlurDelegate = null;

    public RoundedWidgetView(Context context, boolean blurBackground) {
        super(context);
        this.mContext = context;
        this.cornerRadius = context.getResources().getDimensionPixelSize(R.dimen.default_dialog_corner_radius);
        mLongPressHelper = new CheckLongPressHelper(this);
        if (blurBackground) {
            mBlurDelegate = new BlurViewDelegate(this, BlurWallpaperProvider.Companion.getBlurConfigWidget(), null);
            mBlurDelegate.setBlurCornerRadius(cornerRadius);
            setWillNotDraw(false);
            setOutlineProvider(mBlurDelegate.getOutlineProvider());
            setClipToOutline(true);
        }
    }

    @Override
    public void setAppWidget(int appWidgetId, AppWidgetProviderInfo info) {
        super.setAppWidget(appWidgetId, info);
        setPadding(0, 0, 0, 0);
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // compute the path
        stencilPath.reset();
        stencilPath.addRoundRect(0, 0, w, h, cornerRadius, cornerRadius, Path.Direction.CW);
        stencilPath.close();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(stencilPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBlurDelegate != null) {
            mBlurDelegate.draw(canvas);
        }
        super.onDraw(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mLongPressHelper.onTouchEvent(ev);
        return mLongPressHelper.hasPerformedLongPress();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mLongPressHelper.onTouchEvent(event);
        return true;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    @Override
    public int getDescendantFocusability() {
        return mChildrenFocused ? ViewGroup.FOCUS_BEFORE_DESCENDANTS : ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        if (gainFocus) {
            mChildrenFocused = false;
            dispatchChildFocus(false);
        }
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        dispatchChildFocus(mChildrenFocused && focused != null);
        if (focused != null) {
            focused.setFocusableInTouchMode(false);
        }
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return mChildrenFocused;
    }

    private void dispatchChildFocus(boolean childIsFocused) {
        // The host view's background changes when selected, to indicate the focus is
        // inside.
        setSelected(childIsFocused);
    }

    @Override
    public AppWidgetProviderInfo getAppWidgetInfo() {
        return LauncherAppWidgetProviderInfo.fromProviderInfo(mContext, getProviderInfo());
    }
}
