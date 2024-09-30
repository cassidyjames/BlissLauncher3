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
