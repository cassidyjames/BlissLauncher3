/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.ColorUtils

fun Context.toggleKeyboard(view: View, hasFocus: Boolean) {
    val inputMethodManager = getSystemService(InputMethodManager::class.java)
    if (hasFocus) {
        inputMethodManager!!.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    } else {
        inputMethodManager!!.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

fun <T> resourcesToMap(array: List<T>): Map<T, T> {
    val map = mutableMapOf<T, T>()

    if (array.size.mod(2) == 0) {
        for (i in array.indices step 2) {
            map[array[i]] = array[i + 1]
        }
    } else {
        throw Exception("Failed to parse array resource")
    }

    return map
}

fun createNavbarColorAnimator(window: Window): ValueAnimator {
    val navColor: Int = window.navigationBarColor
    val colorAnimation =
        ValueAnimator.ofObject(
            ArgbEvaluator(),
            navColor,
            ColorUtils.setAlphaComponent(navColor, 160)
        )

    colorAnimation.apply {
        duration = 400
        interpolator = LinearInterpolator()
        addUpdateListener { window.navigationBarColor = it.animatedValue as Int }
    }

    return colorAnimation
}

fun runOnMainThread(r: () -> Unit) {
    runOnThread(mainHandler, r)
}

fun runOnThread(handler: Handler, r: () -> Unit) {
    if (handler.looper.thread.id == Looper.myLooper()?.thread?.id) {
        r()
    } else {
        handler.post(r)
    }
}

inline fun <T> Iterable<T>.safeForEach(action: (T) -> Unit) {
    val tmp = ArrayList<T>()
    tmp.addAll(this)
    for (element in tmp) action(element)
}

fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    return drawable?.let { drawableToBitmap(it, true) }
}

fun drawableToBitmap(drawable: Drawable, forceCreate: Boolean): Bitmap? {
    return drawableToBitmap(drawable, forceCreate, 0)
}

fun drawableToBitmap(drawable: Drawable, forceCreate: Boolean, fallbackSize: Int): Bitmap? {
    if (!forceCreate && drawable is BitmapDrawable) {
        return drawable.bitmap
    }

    var width = drawable.intrinsicWidth
    var height = drawable.intrinsicHeight
    if (width <= 0 || height <= 0) {
        if (fallbackSize > 0) {
            height = fallbackSize
            width = height
        } else {
            return null
        }
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
