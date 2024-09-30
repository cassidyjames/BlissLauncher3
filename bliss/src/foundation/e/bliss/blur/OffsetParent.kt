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
