/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import androidx.recyclerview.widget.StaggeredGridLayoutManager

class NoScrollStaggeredLayoutManager(spanCount: Int, orientation: Int) :
    StaggeredGridLayoutManager(spanCount, orientation) {

    override fun canScrollHorizontally(): Boolean {
        return false
    }

    override fun canScrollVertically(): Boolean {
        return false
    }
}
