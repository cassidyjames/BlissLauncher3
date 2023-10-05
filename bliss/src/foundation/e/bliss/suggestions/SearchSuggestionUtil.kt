/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
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
