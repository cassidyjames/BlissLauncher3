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
package foundation.e.bliss.utils

import android.util.Log as AndroidLog
import com.android.launcher3.BuildConfig
import timber.log.Timber

object Logger {
    private val isDebug = BuildConfig.DEBUG

    @JvmStatic fun plant() = Timber.plant(Timber.DebugTree())

    private fun log(
        tag: String,
        msg: String,
        tr: Throwable?,
        logFunction: (String, String, Throwable?) -> Int
    ) {
        if (isDebug) logFunction(tag, msg, tr)
    }

    @JvmStatic fun d(tag: String, msg: String) = log(tag, msg, null, AndroidLog::d)
    @JvmStatic fun d(tag: String, msg: String, tr: Throwable) = log(tag, msg, tr, AndroidLog::d)

    @JvmStatic fun e(tag: String, msg: String) = log(tag, msg, null, AndroidLog::e)
    @JvmStatic fun e(tag: String, msg: String, tr: Throwable) = log(tag, msg, tr, AndroidLog::e)

    @JvmStatic fun i(tag: String, msg: String) = log(tag, msg, null, AndroidLog::i)
    @JvmStatic fun i(tag: String, msg: String, tr: Throwable) = log(tag, msg, tr, AndroidLog::i)

    @JvmStatic fun v(tag: String, msg: String) = log(tag, msg, null, AndroidLog::v)
    @JvmStatic fun v(tag: String, msg: String, tr: Throwable) = log(tag, msg, tr, AndroidLog::v)

    @JvmStatic fun w(tag: String, msg: String) = log(tag, msg, null, AndroidLog::w)
    @JvmStatic fun w(tag: String, msg: String, tr: Throwable) = log(tag, msg, tr, AndroidLog::w)
}
