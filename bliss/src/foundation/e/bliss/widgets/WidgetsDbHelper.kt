/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.widgets

import android.content.ComponentName
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import foundation.e.bliss.utils.Logger

class WidgetsDbHelper(context: Context) :
    SQLiteOpenHelper(context, WIDGETS_DB, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        Logger.d(TAG, "Creating widgets database")
        db.execSQL(
            "CREATE TABLE $WIDGETS_TABLE (" +
                "position INTEGER NOT NULL," +
                "component TEXT NOT NULL," +
                "height INTEGER NOT NULL," +
                "widgetId INTEGER NOT NULL PRIMARY KEY" +
                ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Do nothing, since we are not creating a new database
    }

    fun getWidgets(): List<WidgetInfo> {
        val widgets = mutableListOf<WidgetInfo>()

        writableDatabase?.use { db ->
            db.rawQuery("SELECT * FROM $WIDGETS_TABLE", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val position = cursor.getInt(cursor.getColumnIndexOrThrow("position"))
                    val component = cursor.getString(cursor.getColumnIndexOrThrow("component"))
                    val widgetId = cursor.getInt(cursor.getColumnIndexOrThrow("widgetId"))
                    val height = cursor.getInt(cursor.getColumnIndexOrThrow("height"))

                    widgets.add(
                        WidgetInfo(
                            position,
                            ComponentName.unflattenFromString(component)!!,
                            widgetId,
                            height
                        )
                    )
                }
            }
        }

        return widgets
    }

    fun delete(id: Int) {
        Logger.d(TAG, "Deleting widget $id")
        writableDatabase.use { db ->
            db.apply {
                execSQL("DELETE FROM $WIDGETS_TABLE WHERE widgetId = ?", arrayOf(id))
                execSQL(
                    "UPDATE $WIDGETS_TABLE SET position = position - 1 WHERE widgetId > ?",
                    arrayOf(id)
                )
            }
        }
    }

    fun insert(info: WidgetInfo) {
        Logger.d(
            TAG,
            "Inserting widget at position ${info.position} | ${info.component} | ${info.widgetId}"
        )
        writableDatabase.use { db ->
            db.execSQL(
                "INSERT OR REPLACE INTO $WIDGETS_TABLE (position, component, widgetId, height) VALUES (?, ?, ?, ?)",
                arrayOf(info.position, info.component.flattenToString(), info.widgetId, info.height)
            )
        }
    }

    fun updateHeight(id: Int, height: Int) {
        Logger.d(TAG, "Updating widget $id height to $height")
        writableDatabase.use { db ->
            db.execSQL(
                "UPDATE $WIDGETS_TABLE SET height = ? WHERE widgetId = ?",
                arrayOf(height, id)
            )
        }
    }

    fun getWidgetHeight(id: Int): Int? {
        var height: Int? = null
        writableDatabase?.use { db ->
            db.rawQuery(
                    "SELECT height FROM $WIDGETS_TABLE WHERE widgetId = ?",
                    arrayOf(id.toString())
                )
                .use { cursor ->
                    while (cursor.moveToNext()) {
                        height = cursor.getInt(cursor.getColumnIndexOrThrow("height"))
                    }
                }
        }
        return height
    }

    companion object {
        private const val TAG = "WidgetsDbHelper"
        private const val DATABASE_VERSION = 1
        private const val WIDGETS_DB = "qsb_widgets"
        private const val WIDGETS_TABLE = "widget_items"

        private var instance: WidgetsDbHelper? = null

        @JvmStatic
        fun getInstance(context: Context): WidgetsDbHelper {
            if (instance == null) {
                instance = WidgetsDbHelper(context)
            }
            return requireNotNull(instance)
        }
    }
}
