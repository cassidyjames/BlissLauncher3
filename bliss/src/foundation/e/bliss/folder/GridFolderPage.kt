/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.folder

import android.content.Context
import android.graphics.Path
import android.util.AttributeSet
import com.android.launcher3.views.ClipPathView
import foundation.e.bliss.blur.BlurLayout

class GridFolderPage(context: Context, attrs: AttributeSet?) :
    BlurLayout(context, attrs), ClipPathView {

    private var mClipPath: Path? = null

    override fun setClipPath(clipPath: Path?) {
        mClipPath = clipPath
        invalidate()
    }
}
