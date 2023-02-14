/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.db

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherProvider
import com.android.launcher3.LauncherSettings.Favorites.INTENT
import com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT
import com.android.launcher3.LauncherSettings.Favorites.PROFILE_ID
import com.android.launcher3.LauncherSettings.Favorites.TABLE_NAME
import com.android.launcher3.LauncherSettings.Favorites.TABLE_NAME_ALL
import com.android.launcher3.model.DatabaseHelper
import com.android.launcher3.model.ModelDbController
import com.android.launcher3.pm.UserCache
import com.android.launcher3.shortcuts.ShortcutKey
import java.util.function.ToLongFunction

object BlissDbUtils {
    private const val TAG = "BlissDbUtils"

    @JvmStatic
    fun queryDeepShortcutsFromDb(context: Context): List<ShortcutKey> {
        val shortcutKeys = mutableListOf<ShortcutKey>()
        val dbName = InvariantDeviceProfile.INSTANCE[context].dbFile
        val userSerialProvider =
            ToLongFunction<UserHandle> {
                UserCache.INSTANCE.get(context).getSerialNumberForUser(it)
            }
        val dbHelper = DatabaseHelper(context, dbName, userSerialProvider) {}

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
                            val user = userManager?.getUserForSerialNumber(cursor.getInt(1).toLong())
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
