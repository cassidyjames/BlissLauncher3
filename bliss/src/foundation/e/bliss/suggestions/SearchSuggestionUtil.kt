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
package foundation.e.bliss.suggestions

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import foundation.e.bliss.suggestions.duckduckgo.DuckDuckGoProvider
import foundation.e.bliss.suggestions.qwant.QwantProvider

object SearchSuggestionUtil {

    fun getSuggestionProvider(context: Context): SuggestionProvider {
        return with(defaultSearchEngine(context)) {
            when {
                contains(Providers.QWANT.key, true) -> QwantProvider()
                else -> DuckDuckGoProvider()
            }
        }
    }

    fun getUriForQuery(context: Context, query: String): Uri {
        val defaultSearchEngine = defaultSearchEngine(context)

        return with(defaultSearchEngine) {
            when {
                contains(Providers.QWANT.key, true) -> "${Providers.QWANT.url}?q=$query"
                contains(Providers.DUCKDUCKGO.key, true) -> "${Providers.DUCKDUCKGO.url}?q=$query"
                contains(Providers.MOJEEK.key, true) -> "${Providers.MOJEEK.url}search?q=$query"
                else -> "${Providers.SPOT.url}?q=$query"
            }.toUri()
        }
    }

    private fun defaultSearchEngine(context: Context): String {
        val contentResolver = context.contentResolver
        val uri =
            Uri.parse("content://foundation.e.browser.provider")
                .buildUpon()
                .appendPath("search_engine")
                .build()

        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor.use {
            return if (it != null && it.moveToFirst()) {
                it.getString(0)
            } else {
                ""
            }
        }
    }
}
