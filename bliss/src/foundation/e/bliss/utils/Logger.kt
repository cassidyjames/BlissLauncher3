/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.utils

import android.util.Log as AndroidLog
import com.android.launcher3.BuildConfig

object Logger {
    private val isDebug = BuildConfig.DEBUG

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
