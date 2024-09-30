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
