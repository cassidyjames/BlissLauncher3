/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BlissDbHelper(context: Context, dbName: String) : SQLiteOpenHelper(context, dbName, null, 5) {

    private var database: SQLiteDatabase? = null

    override fun onCreate(db: SQLiteDatabase?) {
        // Do nothing, since we are not creating a new database
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Do nothing, since we are not creating a new database
    }

    fun open() {
        database = writableDatabase
    }

    override fun close() {
        database?.close()
    }
}
