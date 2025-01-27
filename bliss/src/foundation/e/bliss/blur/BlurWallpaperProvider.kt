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

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.android.launcher3.Utilities
import com.android.launcher3.util.DisplayController
import com.android.launcher3.util.Executors
import com.android.launcher3.util.MainThreadInitializedObject
import foundation.e.bliss.utils.Logger
import foundation.e.bliss.utils.runOnMainThread
import foundation.e.bliss.utils.safeForEach
import kotlin.math.ceil

@SuppressLint("NewApi")
class BlurWallpaperProvider(val context: Context) {

    private val mWallpaperManager: WallpaperManager = WallpaperManager.getInstance(context)
    private val mListeners = ArrayList<Listener>()
    private val mDisplaySize = DisplayController.INSTANCE.get(context).info.currentSize

    var wallpapers: BlurSizes? = null
        private set(value) {
            if (field != value) {
                field?.recycle()
                field = value
            }
        }

    var placeholder: Bitmap? = null
        private set(value) {
            if (field != value) {
                field?.recycle()
                field = value
            }
        }

    private val mVibrancyPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    private var mWallpaperWidth: Int = 0

    private val mUpdateRunnable = Runnable { updateWallpaper() }

    private val wallpaperFilter = BlurWallpaperFilter(context)
    private var applyTask: WallpaperFilter.ApplyTask<BlurSizes>? = null

    private var updatePending = false

    private var isLiveWallpaper = false

    init {
        isEnabled = getEnabledStatus()
        updateAsync()
    }

    private fun getEnabledStatus() = mWallpaperManager.wallpaperInfo == null

    fun updateAsync() {
        Executors.THREAD_POOL_EXECUTOR.execute(mUpdateRunnable)
    }

    fun setLiveWallpaper(isLive: Boolean) {
        isLiveWallpaper = isLive
    }

    @SuppressLint("MissingPermission")
    private fun updateWallpaper() {
        if (applyTask != null) {
            updatePending = true
            return
        }

        val width = mDisplaySize.x
        val height = mDisplaySize.y

        // Prepare a placeholder before hand so that it can be used in case wallpaper is null
        placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(placeholder!!)
        canvas.drawColor(0x44000000)

        if (!isEnabled) {
            wallpapers = null
            runOnMainThread { mListeners.safeForEach(Listener::onEnabledChanged) }
        }

        var wallpaper =
            try {
                val wall = mWallpaperManager.drawable!!.toBitmap()
                if (isLiveWallpaper) {
                    createTransparentBitmap(wall.width, wall.height)
                } else {
                    wall
                }
            } catch (e: Exception) {
                runOnMainThread {
                    val msg = "Failed: ${e.message}"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    notifyWallpaperChanged()
                }
                return
            }

        wallpaper = scaleAndCropToScreenSize(wallpaper)
        mWallpaperWidth = wallpaper.width

        val offsetY: Float
        if (wallpaper.height > height) {
            offsetY = (wallpaper.height - height) * 0.5f
            mListeners.forEach { it.onOffsetChanged(offsetY) }
        }

        wallpaper = applyVibrancy(wallpaper)
        Logger.d(TAG, "starting blur")

        applyTask =
            wallpaperFilter.apply(wallpaper).setCallback { result, error ->
                if (error == null) {
                    this@BlurWallpaperProvider.wallpapers = result
                    runOnMainThread(::notifyWallpaperChanged)
                    wallpaper.recycle()
                } else {
                    if (error is OutOfMemoryError) {
                        runOnMainThread {
                            Toast.makeText(context, "Failed!", Toast.LENGTH_LONG).show()
                        }
                        notifyWallpaperChanged()
                    }
                    wallpaper.recycle()
                }
            }
        applyTask = null
        if (updatePending) {
            updatePending = false
            updateWallpaper()
        }
    }

    private fun createTransparentBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint =
            Paint().apply {
                color = Color.argb(68, 0, 0, 0) // Black with semi-transparency
                isAntiAlias = true
            }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun notifyWallpaperChanged() {
        mListeners.forEach(Listener::onWallpaperChanged)
    }

    private fun applyVibrancy(wallpaper: Bitmap): Bitmap {
        val width = wallpaper.width
        val height = wallpaper.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas()
        canvas.setBitmap(bitmap)

        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(1.25f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        mVibrancyPaint.colorFilter = filter
        canvas.drawBitmap(wallpaper, 0f, 0f, mVibrancyPaint)

        wallpaper.recycle()

        return bitmap
    }

    private fun scaleAndCropToScreenSize(wallpaper: Bitmap): Bitmap {
        val width = mDisplaySize.x
        val height = mDisplaySize.y

        val widthFactor = width.toFloat() / wallpaper.width
        val heightFactor = height.toFloat() / wallpaper.height

        val upscaleFactor = widthFactor.coerceAtLeast(heightFactor)
        if (upscaleFactor <= 0) {
            return wallpaper
        }

        val scaledWidth = width.coerceAtLeast(ceil(wallpaper.width * upscaleFactor).toInt())
        val scaledHeight = height.coerceAtLeast(ceil(wallpaper.height * upscaleFactor).toInt())

        return Bitmap.createScaledBitmap(wallpaper, scaledWidth, scaledHeight, false)
    }

    fun addListener(listener: Listener) {
        mListeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        mListeners.remove(listener)
    }

    fun createBlurDrawable(config: BlurConfig = blurConfigDock) = BlurDrawable(this, config)

    fun setWallpaperOffset(offset: Float) {
        if (!isEnabled) return
        if (wallpapers == null) return

        val availableWidth = mDisplaySize.x - mWallpaperWidth
        var xPixels = availableWidth / 2
        if (availableWidth < 0) {
            xPixels += (availableWidth * (offset - .5f) + .5f).toInt()
        }

        val scrollOffset =
            Utilities.boundToRange(
                (-xPixels).toFloat(),
                0f,
                (mWallpaperWidth - mDisplaySize.x).toFloat()
            )

        runOnMainThread { mListeners.forEach { it.onScrollOffsetChanged(scrollOffset) } }
    }

    fun orientationChanged() {
        updateWallpaper()
    }

    interface Listener {
        fun onWallpaperChanged() {}
        fun onEnabledChanged() {}
        fun onScrollOffsetChanged(offset: Float) {}
        fun onOffsetChanged(offset: Float) {}
    }

    data class BlurSizes(
        val background: Bitmap,
        val dock: Bitmap,
        val appGroup: Bitmap,
        val widget: Bitmap
    ) {
        fun recycle() {
            background.recycle()
            dock.recycle()
            appGroup.recycle()
            widget.recycle()
        }
    }

    data class BlurConfig(val getDrawable: (BlurSizes) -> Bitmap, val scale: Int, val radius: Int)

    companion object {
        val INSTANCE = MainThreadInitializedObject { context: Context ->
            BlurWallpaperProvider(context)
        }

        fun getInstance(context: Context): BlurWallpaperProvider {
            return INSTANCE.get(context)
        }

        fun getInstanceNoCreate(): BlurWallpaperProvider {
            return INSTANCE.noCreate
        }

        const val TAG = "BlurWallpaperProvider"

        @JvmField val blurConfigBackground = BlurConfig({ it.background }, 2, 8)

        @JvmField val blurConfigDock = BlurConfig({ it.dock }, 2, 0)

        @JvmField val blurConfigAppGroup = BlurConfig({ it.appGroup }, 6, 8)

        @JvmField val blurConfigWidget = BlurConfig({ it.widget }, 6, 10)

        var isEnabled: Boolean = false
    }
}
