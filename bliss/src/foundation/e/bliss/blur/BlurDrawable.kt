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

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

class BlurDrawable
internal constructor(
    private val blurWallpaperProvider: BlurWallpaperProvider,
    private val config: BlurWallpaperProvider.BlurConfig = BlurWallpaperProvider.blurConfigWidget
) : Drawable(), BlurWallpaperProvider.Listener {

    private var blurAlpha = 255
    private val blurPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var blurBitmap: Bitmap? = null
        set(value) {
            if (field != value) {
                field = value
                blurPaint.shader =
                    value?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
            }
        }

    private val blurBounds = RectF()
    private var offsetX = 0f
    private var offsetY = 0f

    override fun draw(canvas: Canvas) {
        val width = blurBounds.right.toInt() - blurBounds.left.toInt()
        val height = blurBounds.bottom.toInt() - blurBounds.top.toInt()
        if (width <= 0 || height <= 0) return
        if (blurAlpha == 0) return
        val wallpapers = blurWallpaperProvider.wallpapers
        blurBitmap = wallpapers?.let { config.getDrawable(it) }
        val scale = config.scale.toFloat()

        if (blurBitmap == null) {
            blurBitmap = blurWallpaperProvider.placeholder
        }

        val left = blurBounds.left + offsetX
        val top = blurBounds.top + offsetY
        val right = blurBounds.right + offsetX
        val bottom = blurBounds.bottom + offsetY
        val count = canvas.save()
        canvas.translate(-left, -top)
        canvas.clipRect(left, top, right, bottom)
        canvas.scale(scale, scale, 0f, 0f)
        canvas.drawPaint(blurPaint)
        canvas.restoreToCount(count)
    }

    override fun setAlpha(alpha: Int) {
        blurAlpha = alpha
        blurPaint.alpha = alpha
    }

    override fun getAlpha() = blurAlpha

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) =
        setBlurBounds(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    fun setBlurBounds(left: Float, top: Float, right: Float, bottom: Float) {
        if (
            blurBounds.left != left ||
                blurBounds.top != top ||
                blurBounds.right != right ||
                blurBounds.bottom != bottom
        ) {
            blurBounds.set(left, top, right, bottom)
        }
    }

    fun setOffsets(offsetX: Float, offsetY: Float) {
        this.offsetX = offsetX
        this.offsetY = offsetY
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onWallpaperChanged() {
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    fun startListening() = blurWallpaperProvider.addListener(this)

    fun stopListening() = blurWallpaperProvider.removeListener(this)
}
