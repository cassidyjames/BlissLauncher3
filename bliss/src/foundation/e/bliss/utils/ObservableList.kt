/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.utils

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class ObservableList<T : Any> {
    val list: MutableList<T>
    private val onAdd: PublishSubject<T>

    init {
        list = ArrayList()
        onAdd = PublishSubject.create()
    }

    fun add(value: T) {
        list.add(value)
        onAdd.onNext(value)
    }

    val observable: Observable<T>
        get() = onAdd
}
