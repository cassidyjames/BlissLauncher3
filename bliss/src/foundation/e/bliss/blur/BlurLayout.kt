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

open class BlurLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs), OffsetParent {

    private val delegate =
        BlurViewDelegate(this.rootView, BlurWallpaperProvider.blurConfigWidget, attrs)
    private val offsetParentDelegate = OffsetParent.OffsetParentDelegate()

    init {
        this.setWillNotDraw(false)
        clipToOutline = true

        outlineProvider = delegate.outlineProvider
    }

    override fun onDraw(canvas: Canvas) {
        delegate.draw(canvas)
        super.onDraw(canvas)
    }

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
