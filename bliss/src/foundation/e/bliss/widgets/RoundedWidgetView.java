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
package foundation.e.bliss.widgets;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.uioverrides.QuickstepLauncher;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

import foundation.e.bliss.blur.BlurViewDelegate;
import foundation.e.bliss.blur.BlurWallpaperProvider;
import foundation.e.bliss.blur.OffsetParent;

@SuppressLint("ViewConstructor")
public class RoundedWidgetView extends LauncherAppWidgetHostView implements OffsetParent {
    private final Path stencilPath = new Path();
    private final float cornerRadius;
    private final Context mContext;
    private ImageView resizeBorder;
    private boolean mChildrenFocused;
    private BlurViewDelegate mBlurDelegate = null;
    protected QuickstepLauncher mLauncher;

    private final OffsetParentDelegate offsetParentDelegate = new OffsetParentDelegate();

    public RoundedWidgetView(Context context, boolean blurBackground) {
        super(context);
        this.mContext = context;
        this.cornerRadius = context.getResources().getDimensionPixelSize(R.dimen.default_widget_corner_radius);
        if (blurBackground) {
            mBlurDelegate = new BlurViewDelegate(this, BlurWallpaperProvider.blurConfigWidget, null);
            mBlurDelegate.setBlurCornerRadius(cornerRadius);
            setWillNotDraw(false);
            setOutlineProvider(mBlurDelegate.getOutlineProvider());
            setClipToOutline(true);
        }
    }

    public void setHeight(int newHeight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();
        params.height = newHeight;
        setLayoutParams(params);
    }

    @Override
    public void setAppWidget(int appWidgetId, AppWidgetProviderInfo info) {
        super.setAppWidget(appWidgetId, info);
        int widgetTopBottom = mContext.getResources().getDimensionPixelSize(R.dimen.widget_page_top_bottom_padding);
        int widgetLeftRight = mContext.getResources().getDimensionPixelSize(R.dimen.widget_page_all_padding);
        setPadding(widgetLeftRight, widgetTopBottom, widgetLeftRight, widgetTopBottom);
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

    public void addBorder() {
        if (resizeBorder != null) {
            removeBorder();
            return;
        }
        resizeBorder = new ImageView(mContext);
        resizeBorder.setImageResource(R.drawable.bliss_resize_frame);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        resizeBorder.setLayoutParams(layoutParams);
        addView(resizeBorder);
        startWobble();
    }

    public void startWobble() {
        mLauncher = Launcher.cast(Launcher.getLauncher(mContext));
        startAnimation(mLauncher.getWorkspace().getWobbleAnimation());
    }

    public void removeBorder() {
        if (resizeBorder != null) {
            removeView(resizeBorder);
            clearAnimation();
            resizeBorder = null;
        }
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
}
