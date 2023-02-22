/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.blur

import android.content.Context
import android.graphics.Bitmap
import com.hoko.blur.HokoBlur

class BlurWallpaperFilter(private val context: Context) :
    WallpaperFilter<BlurWallpaperProvider.BlurSizes> {

    override fun apply(
        wallpaper: Bitmap
    ): WallpaperFilter.ApplyTask<BlurWallpaperProvider.BlurSizes> {
        return WallpaperFilter.ApplyTask.create { emitter ->
            var blurBackground: Bitmap? = null
            var blurDock: Bitmap? = null
            var blurAppGroup: Bitmap? = null
            var blurWidget: Bitmap? = null
            try {
                blurBackground = blur(wallpaper, BlurWallpaperProvider.blurConfigBackground)
                blurDock = blur(wallpaper, BlurWallpaperProvider.blurConfigDock)
                blurAppGroup = blur(wallpaper, BlurWallpaperProvider.blurConfigAppGroup)
                blurWidget = blur(wallpaper, BlurWallpaperProvider.blurConfigWidget)
                emitter.onSuccess(
                    BlurWallpaperProvider.BlurSizes(
                        blurBackground,
                        blurDock,
                        blurAppGroup,
                        blurWidget
                    )
                )
            } catch (t: Throwable) {
                blurBackground?.recycle()
                blurDock?.recycle()
                blurAppGroup?.recycle()
                blurWidget?.recycle()
                emitter.onError(t)
            }
        }
    }

    private fun blur(wallpaper: Bitmap, config: BlurWallpaperProvider.BlurConfig): Bitmap {
        val source =
            if (config.scale == 1) {
                wallpaper
            } else {
                Bitmap.createScaledBitmap(
                    wallpaper,
                    wallpaper.width / config.scale,
                    wallpaper.height / config.scale,
                    true
                )
            }

        return HokoBlur.with(context)
            .scheme(HokoBlur.SCHEME_NATIVE)
            .mode(HokoBlur.MODE_STACK)
            .radius(config.radius)
            .forceCopy(false)
            .processor()
            .blur(source)
    }
}
