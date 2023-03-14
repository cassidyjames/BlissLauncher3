/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.utils

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherProvider
import com.android.launcher3.LauncherSettings.Favorites.INTENT
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_FOLDER
import com.android.launcher3.LauncherSettings.Favorites.PROFILE_ID
import com.android.launcher3.LauncherSettings.Favorites.TABLE_NAME
import com.android.launcher3.LauncherSettings.Favorites.TABLE_NAME_ALL
import com.android.launcher3.model.DatabaseHelper
import com.android.launcher3.pm.UserCache
import com.android.launcher3.shortcuts.ShortcutKey
import java.net.URISyntaxException

object BlissDbUtils {
    private const val TAG = "BlissDbUtils"

    // Previous database details
    private const val oldDbName = "launcher_db"
    private const val oldTable = "launcher_items"

    // Container
    private const val hotSeat: Long = -101
    private const val homeScreen: Long = -100

    // Item types in old bliss database
    private const val appType = 0
    private const val pwaType = 1
    private const val folderType = 2

    @JvmStatic
    fun migrateDataFromDb(context: Context): Boolean {
        // Check if old database exists
        val oldFile = context.getDatabasePath(oldDbName)
        if (!oldFile.exists()) return false

        // Current database details
        val currentDbName = InvariantDeviceProfile.INSTANCE[context].dbFile
        val rowCount = InvariantDeviceProfile.INSTANCE[context].numRows
        val columnCount = InvariantDeviceProfile.INSTANCE[context].numColumns
        val numFolderRows = InvariantDeviceProfile.INSTANCE[context].numFolderRows[0]
        val numFolderColumns = InvariantDeviceProfile.INSTANCE[context].numFolderColumns[0]

        // Init database helper classes
        val dbHelper = DatabaseHelper(context, currentDbName, UserCache.INSTANCE.get(context)::getSerialNumberForUser) {}
        val oldDbHelper = BlissDbHelper(context, oldDbName)

        // Retrieve data from the old table
        val favoritesList = mutableListOf<Favorite>()
        oldDbHelper.readableDatabase.use { database ->
            database.rawQuery("SELECT * FROM $oldTable", null).use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        val cell = cursor.getInt(cursor.getColumnIndexOrThrow("cell"))
                        val container = cursor.getLong(cursor.getColumnIndexOrThrow("container"))
                        val intentUri = cursor.getString(cursor.getColumnIndexOrThrow("intent_uri"))
                        val itemId = cursor.getString(cursor.getColumnIndexOrThrow("item_id"))
                        val itemType = cursor.getInt(cursor.getColumnIndexOrThrow("item_type"))
                        val packageName = cursor.getString(cursor.getColumnIndexOrThrow("package"))
                        val screen = cursor.getInt(cursor.getColumnIndexOrThrow("screen_id")) + 1
                        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                        var isFolder = false
                        var isInFolder = false
                        var isPwa = false
                        var isInWorkProfile = false

                        // Set isFolder true if its a folder
                        // If its not folder its either app or pwa,
                        // if its inside a folder and set isInFolder true
                        if (itemId.toLongOrNull() != null && itemType == folderType) {
                            isFolder = true
                        } else if (
                            (itemType == appType || itemType == pwaType) &&
                                container != homeScreen &&
                                container != hotSeat
                        ) {
                            isInFolder = true
                        }

                        // Set isPwa true if its pwa
                        if (
                            itemType == pwaType &&
                                itemId.toLongOrNull() == null &&
                                !itemId.contains(packageName)
                        ) {
                            isPwa = true
                        }

                        // Set isInWorkProfile if the app in work profile
                        if (hasIntAfterLastSlash(itemId)) {
                            isInWorkProfile = true
                        }

                        favoritesList.add(
                            Favorite(
                                cursor.count,
                                cell,
                                container,
                                intentUri,
                                itemId,
                                itemType,
                                packageName,
                                screen,
                                title,
                                isFolder,
                                isInFolder,
                                isPwa,
                                isInWorkProfile
                            )
                        )
                    } catch (e: URISyntaxException) {
                        Log.e(TAG, "migrateDataFromDb: ", e)
                        return false
                    }
                }
            }
        }

        // Close oldDbHelper
        oldDbHelper.close()

        if (favoritesList.isEmpty()) return false

        // Store each type separately
        val folderList = mutableMapOf<Favorite, Long>()
        val appsPwaList = mutableListOf<Favorite>()
        favoritesList.forEach { fav ->
            if (fav.isFolder) {
                // Value added here is temp, it will be replaced
                // with id returned from database insertion
                folderList[fav] = 0
            } else {
                appsPwaList.add(fav)
            }
        }

        // Insert folder first, so we can store it's id and use it for apps/pwa.
        for (item in folderList) {
            val fav = item.key
            val values = getBaseContentValues(fav)
            if (fav.container == hotSeat) {
                // Hot-seat is 4x1 grid size
                val (y, x) = getGridPosition(fav.cell, 1, columnCount)
                values.put("cellX", x)
                values.put("cellY", y)
                values.put("screen", x)
            } else {
                // Home screen is 4x5 grid size
                val (y, x) = getGridPosition(fav.cell, rowCount, columnCount)
                values.put("cellX", x)
                values.put("cellY", y)
            }
            values.put("itemType", ITEM_TYPE_FOLDER)
            folderList[fav] = dbHelper.writableDatabase.insert(TABLE_NAME_ALL, null, values)
        }

        // Insert apps and pwa together and add checks for extra pwa inserts
        for (item in appsPwaList) {
            if (item.packageName != null) {
                val values = getBaseContentValues(item)
                if (item.isPwa) {
                    values.put("intent", pwaIntentUri(context, item.packageName, item.itemId))
                } else {
                    values.put("intent", intentUri(item.packageName, item.itemId))
                }
                if (item.isInFolder) {
                    // folder is 3x3 for 4x5, 4x4 for 5x5 and so on depending on columnSize grid
                    // size
                    val (y, x) = getGridPosition(item.cell, numFolderRows, numFolderColumns)
                    values.put("cellX", x)
                    values.put("cellY", y)
                    if (item.isPwa) {
                        // Pwa inside folder has rank of 1 as per launcher3
                        values.put("rank", 1)
                    }
                    values.put(
                        "container",
                        folderList.entries
                            .find { it.key.itemId.toLongOrNull() == item.container }
                            ?.value
                    )
                } else if (item.container == hotSeat) {
                    // Hot-seat is 4x1 grid size
                    val (y, x) = getGridPosition(item.cell, 1, columnCount)
                    values.put("screen", x)
                    values.put("cellX", x)
                    values.put("cellY", y)
                } else {
                    // Home screen is 4x5 grid size
                    val (y, x) = getGridPosition(item.cell, rowCount, columnCount)
                    values.put("cellX", x)
                    values.put("cellY", y)
                }
                if (item.isPwa) {
                    // Override itemType for pwa
                    values.put("itemType", ITEM_TYPE_DEEP_SHORTCUT)
                }
                if (item.isInWorkProfile) {
                    val profileId = item.itemId.substringAfterLast("/").toInt()
                    values.put("profileId", profileId)
                }
                dbHelper.writableDatabase.insert(TABLE_NAME_ALL, null, values)
            }
        }

        dbHelper.close()

        // Rename the database to old
        val newFile = context.getDatabasePath(oldDbName + "_old")
        oldFile.renameTo(newFile)

        return true
    }

    private fun getBaseContentValues(favorite: Favorite): ContentValues {
        return ContentValues().apply {
            put("appWidgetId", -1)
            put("appWidgetProvider", null as String?)
            put("appWidgetSource", -1)
            put("cellX", 0)
            put("cellY", 0)
            put("container", favorite.container)
            put("icon", null as ByteArray?)
            put("iconPackage", null as String?)
            put("iconResource", null as String?)
            put("intent", null as String?)
            put("itemType", ITEM_TYPE_APPLICATION)
            put("modified", System.currentTimeMillis())
            put("options", 0)
            put("profileId", 0)
            put("rank", 0)
            put("restored", 0)
            put("screen", favorite.screen)
            put("spanX", 1)
            put("spanY", 1)
            put("title", favorite.title)
        }
    }

    data class Favorite(
        val id: Int,
        val cell: Int,
        val container: Long,
        val intentUri: String?,
        val itemId: String,
        val itemType: Int,
        val packageName: String?,
        val screen: Int,
        val title: String,
        val isFolder: Boolean,
        val isInFolder: Boolean,
        val isPwa: Boolean,
        val isInWorkProfile: Boolean
    )

    private fun pwaIntentUri(context: Context, packageName: String, shortcutId: String): String? {
        val action = Intent.ACTION_MAIN
        val category = "com.android.launcher3.DEEP_SHORTCUT"
        val componentName = getLaunchActivity(context, packageName)
        val launchFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        if (componentName != null) {
            return "#Intent;action=$action;category=$category;launchFlags=$launchFlags;" +
                "package=$packageName;component=$componentName;S.shortcut_id=$shortcutId;end"
        }
        return null
    }

    private fun getLaunchActivity(context: Context, packageName: String): String? {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setPackage(packageName)

        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        if (resolveInfoList.isNotEmpty()) {
            val activityInfo = resolveInfoList[0].activityInfo
            return "${activityInfo.packageName}/${activityInfo.name}"
        }
        return null
    }

    private fun hasIntAfterLastSlash(inputString: String): Boolean {
        val outputString = inputString.substringAfterLast("/")
        return try {
            outputString.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun intentUri(packageName: String, componentName: String): String {
        val intent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(packageName, componentName))
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
        return intent.toUri(Intent.URI_INTENT_SCHEME).substring(7)
    }

    private fun getGridPosition(cell: Int, rowCount: Int, columnCount: Int): Pair<Int, Int> {
        val pageSize = rowCount * columnCount - 1
        val cellIndexOnPage = cell % (pageSize + 1)
        val rowIndex = cellIndexOnPage / columnCount
        val columnIndex = cellIndexOnPage % columnCount
        return Pair(rowIndex, columnIndex)
    }

    @JvmStatic
    fun queryDeepShortcutsFromDb(context: Context): List<ShortcutKey> {
        val shortcutKeys = mutableListOf<ShortcutKey>()
        val dbName = InvariantDeviceProfile.INSTANCE[context].dbFile
        val dbHelper = DatabaseHelper(context, dbName, UserCache.INSTANCE.get(context)::getSerialNumberForUser) {}

        val userManager = context.getSystemService(UserManager::class.java)

        try {
            dbHelper.writableDatabase.use { database ->
                database
                    .rawQuery(
                        "SELECT $INTENT, $PROFILE_ID FROM $TABLE_NAME_ALL WHERE itemType=$ITEM_TYPE_DEEP_SHORTCUT UNION " +
                            "SELECT $INTENT, $PROFILE_ID FROM $TABLE_NAME WHERE itemType=$ITEM_TYPE_DEEP_SHORTCUT",
                        null
                    )
                    .use { cursor ->
                        while (cursor.moveToNext()) {
                            val user = userManager!!.getUserForSerialNumber(cursor.getInt(1).toLong())
                            val intent = Intent.parseUri(cursor.getString(0), 0)
                            shortcutKeys.add(ShortcutKey.fromIntent(intent, user))
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryDeepShortcutsFromeDb: ", e)
        }

        return shortcutKeys
    }
}
