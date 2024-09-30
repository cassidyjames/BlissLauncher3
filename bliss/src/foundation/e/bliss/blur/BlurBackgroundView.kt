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
