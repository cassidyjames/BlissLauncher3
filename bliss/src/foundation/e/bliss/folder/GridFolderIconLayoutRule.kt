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
import com.android.launcher3.R
import com.android.launcher3.folder.ClippedFolderIconLayoutRule
import com.android.launcher3.folder.PreviewItemDrawingParams

class GridFolderIconLayoutRule(context: Context) : ClippedFolderIconLayoutRule() {
    private val mGridCountX: Int
    private val mGridCountY: Int
    private val mItemIconScale: Float
    private val maxNumItemsInPreview: Int

    init {
        val resources = context.resources
        mGridCountX = resources.getInteger(R.integer.grid_folder_icon_rows)
        mGridCountY = resources.getInteger(R.integer.grid_folder_icon_columns)
        mItemIconScale =
            resources.getInteger(R.integer.grid_folder_app_icon_size_percentage) / 100.0f
        maxNumItemsInPreview = mGridCountX * mGridCountY
    }

    override fun computePreviewItemDrawingParams(
        index: Int,
        curNumItems: Int,
        params: PreviewItemDrawingParams?
    ): PreviewItemDrawingParams {
        val transX: Float
        val transY: Float
        val scale = scaleForItem(index)
        val point = FloatArray(2)

        if (index < maxNumItemsInPreview) {
            var baseX = index % mGridCountX
            val baseY = index / mGridCountY
            if (mIsRtl) {
                baseX = (mGridCountX - 1) - baseX
            }
            var paddingX = (mAvailableSpace - (iconSize * scale) * mGridCountX) / (mGridCountX + 1)
            if (paddingX < 0) {
                paddingX = 0f
            }
            var paddingY = (mAvailableSpace - (iconSize * scale) * mGridCountY) / (mGridCountY + 1)
            if (paddingY < 0) {
                paddingY = 0f
            }
            point[0] = (baseX + 1) * paddingX + baseX * (iconSize * scale)
            point[1] = (baseY + 1) * paddingY + baseY * (iconSize * scale)
        } else {
            point[1] = mAvailableSpace / 2 - (iconSize * scale) / 2
            point[0] = point[1]
        }
        transX = point[0]
        transY = point[1]
        if (params == null) {
            return PreviewItemDrawingParams(transX, transY, scale)
        }

        params.update(transX, transY, scale)
        return params
    }

    override fun scaleForItem(numItems: Int): Float {
        return mItemIconScale * mBaselineIconScale
    }

    override fun getMaxNumItemsInPreview(): Int {
        return maxNumItemsInPreview
    }
}
