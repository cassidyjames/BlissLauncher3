/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
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
