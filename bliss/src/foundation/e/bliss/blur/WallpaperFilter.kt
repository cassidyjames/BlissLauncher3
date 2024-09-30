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

interface WallpaperFilter<T> {

    fun apply(wallpaper: Bitmap): ApplyTask<T>

    class ApplyTask<T> {

        val emitter = Emitter()

        private var result: T? = null
        private var error: Throwable? = null

        private var callback: ((T?, Throwable?) -> Unit)? = null

        fun setCallback(callback: (T?, Throwable?) -> Unit): ApplyTask<T> {
            result?.let {
                callback(it, null)
                return this
            }
            error?.let {
                callback(null, it)
                return this
            }
            this.callback = callback
            return this
        }

        inner class Emitter {

            fun onSuccess(result: T) {
                callback?.let {
                    it(result, null)
                    return
                }
                this@ApplyTask.result = result
            }

            fun onError(error: Throwable) {
                callback?.let {
                    it(null, error)
                    return
                }
                this@ApplyTask.error = error
            }
        }

        companion object {
            inline fun <T> create(source: (ApplyTask<T>.Emitter) -> Unit): ApplyTask<T> {
                return ApplyTask<T>().also { source(it.emitter) }
            }
        }
    }
}
