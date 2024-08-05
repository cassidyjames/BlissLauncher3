/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.suggestions.duckduckgo

import android.util.Log
import foundation.e.bliss.suggestions.RetrofitService
import foundation.e.bliss.suggestions.SuggestionProvider
import foundation.e.bliss.suggestions.SuggestionsResult

class DuckDuckGoProvider : SuggestionProvider {
    private val suggestionService: DuckDuckGoApi
        get() =
            RetrofitService.getInstance(DuckDuckGoApi.BASE_URL).create(DuckDuckGoApi::class.java)

    override suspend fun query(query: String): SuggestionsResult {
        val result = kotlin.runCatching { suggestionService.query(query) }
        Log.d("DuckDuckGoProvider", "Result: $result")
        val suggestions = SuggestionsResult(query)
        return if (result.isSuccess) {
            suggestions.apply {
                networkItems = result.getOrNull()?.map { it?.phrase }?.take(3) ?: emptyList()
            }
        } else suggestions.apply { networkItems = emptyList() }
    }
}
