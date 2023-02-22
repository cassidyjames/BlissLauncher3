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
import android.util.AttributeSet
import android.widget.FrameLayout

class BlurLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val delegate = BlurViewDelegate(this, BlurWallpaperProvider.blurConfigWidget, attrs)

    init {
        setWillNotDraw(false)
        clipToOutline = true

        outlineProvider = delegate.outlineProvider
    }

    override fun onDraw(canvas: Canvas) {
        delegate.draw(canvas)
        super.onDraw(canvas)
    }
}
