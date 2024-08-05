/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.wobble

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.res.ResourcesCompat
import com.android.launcher3.R

class UninstallButtonRenderer(private val mContext: Context, iconSizePx: Int) {
    private val mSize: Int
    private val mPaint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)

    init {
        mSize = (SIZE_PERCENTAGE * iconSizePx).toInt()
    }

    fun draw(canvas: Canvas, iconBounds: Rect) {
        val uninstallDrawable =
            ResourcesCompat.getDrawable(
                mContext.resources,
                R.drawable.ic_remove_icon,
                mContext.theme
            )

        uninstallDrawable?.let {
            val halfSize = mSize / 2
            it.setBounds(
                iconBounds.right - halfSize,
                iconBounds.top - halfSize,
                iconBounds.right + halfSize,
                iconBounds.top + halfSize
            )

            it.draw(canvas)
        }
    }

    /**
     * We double the icons bounds here to increase the touch area of uninstall icon size.
     *
     * @param iconBounds
     * @return Doubled bounds for uninstall icon click.
     */
    fun getBoundsScaled(iconBounds: Rect): Rect {
        val uninstallIconBounds = Rect()
        uninstallIconBounds.left = iconBounds.right - mSize
        uninstallIconBounds.top = iconBounds.top - mSize
        uninstallIconBounds.right = uninstallIconBounds.left + (3 * mSize)
        uninstallIconBounds.bottom = uninstallIconBounds.top + (3 * mSize)
        return uninstallIconBounds
    }

    companion object {
        private const val SIZE_PERCENTAGE = 0.28f
    }
}
