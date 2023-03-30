/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import foundation.e.bliss.blur.OffsetParent

class SwipeSearchContainer
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs), OffsetParent {

    private val offsetParentDelegate = OffsetParent.OffsetParentDelegate()

    override val offsetX: Float
        get() = translationX

    override val offsetY: Float
        get() = translationY

    override val needWallpaperScroll: Boolean
        get() = true

    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX)
        offsetParentDelegate.notifyOffsetChanged()
    }

    override fun setTranslationY(translationY: Float) {
        super.setTranslationY(translationY)
        offsetParentDelegate.notifyOffsetChanged()
    }

    override fun addOnOffsetChangeListener(listener: OffsetParent.OnOffsetChangeListener) {
        offsetParentDelegate.addOnOffsetChangeListener(listener)
    }

    override fun removeOnOffsetChangeListener(listener: OffsetParent.OnOffsetChangeListener) {
        offsetParentDelegate.removeOnOffsetChangeListener(listener)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
    }
}
