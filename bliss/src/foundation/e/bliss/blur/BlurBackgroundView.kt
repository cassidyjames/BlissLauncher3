/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.blur

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.android.launcher3.Insettable
import foundation.e.bliss.blur.OffsetParent.OffsetParentDelegate

class BlurBackgroundView(context: Context, attrs: AttributeSet?) :
    View(context, attrs), Insettable, BlurWallpaperProvider.Listener, OffsetParent {

    private val mBlurDelegate =
        BlurViewDelegate(this, BlurWallpaperProvider.blurConfigBackground, null)
    private val offsetParentDelegate = OffsetParentDelegate()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        mBlurDelegate.draw(canvas)
    }

    override fun setInsets(insets: Rect) {}

    override val offsetX: Float
        get() = translationX

    override val offsetY: Float
        get() = translationY

    override val needWallpaperScroll: Boolean
        get() = true

    override fun addOnOffsetChangeListener(listener: OffsetParent.OnOffsetChangeListener) {
        offsetParentDelegate.addOnOffsetChangeListener(listener)
    }

    override fun removeOnOffsetChangeListener(listener: OffsetParent.OnOffsetChangeListener) {
        offsetParentDelegate.removeOnOffsetChangeListener(listener)
    }
}
