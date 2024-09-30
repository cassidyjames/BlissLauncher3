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
package foundation.e.bliss.folder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout
import com.android.launcher3.views.ClipPathView

class GridFolderPage(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs), ClipPathView {

    private var mClipPath: Path? = null

    override fun setClipPath(clipPath: Path?) {
        mClipPath = clipPath
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        val clipPath = mClipPath
        if (clipPath != null) {
            val count: Int = canvas.save()
            canvas.clipPath(clipPath)
            background.draw(canvas)
            super.draw(canvas)
            canvas.restoreToCount(count)
        } else {
            background.draw(canvas)
            super.draw(canvas)
        }
    }
}
