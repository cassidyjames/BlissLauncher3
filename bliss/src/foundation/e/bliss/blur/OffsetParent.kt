/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.blur

import java.util.concurrent.CopyOnWriteArrayList

interface OffsetParent {

    val offsetX: Float
    val offsetY: Float

    val needWallpaperScroll: Boolean

    fun addOnOffsetChangeListener(listener: OnOffsetChangeListener)
    fun removeOnOffsetChangeListener(listener: OnOffsetChangeListener)

    interface OnOffsetChangeListener {
        fun onOffsetChange()
    }

    class OffsetParentDelegate {
        private val listeners = CopyOnWriteArrayList<OnOffsetChangeListener>()

        fun notifyOffsetChanged() {
            listeners.forEach { it.onOffsetChange() }
        }

        fun addOnOffsetChangeListener(listener: OnOffsetChangeListener) {
            listeners.add(listener)
        }

        fun removeOnOffsetChangeListener(listener: OnOffsetChangeListener) {
            listeners.remove(listener)
        }
    }
}
