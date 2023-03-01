/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlin.Exception

fun Context.toggleKeyboard(view: View, hasFocus: Boolean) {
    val inputMethodManager = getSystemService(InputMethodManager::class.java)
    if (hasFocus) {
        inputMethodManager?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    } else {
        inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

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
