/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.suggestions.qwant

import android.util.Log
import foundation.e.bliss.suggestions.RetrofitService
import foundation.e.bliss.suggestions.SuggestionProvider
import foundation.e.bliss.suggestions.SuggestionsResult

class QwantProvider : SuggestionProvider {

    private val suggestionService: QwantApi
        get() = RetrofitService.getInstance(QwantApi.BASE_URL).create(QwantApi::class.java)

    override suspend fun query(query: String): SuggestionsResult {
        val result = suggestionService.query(query)
        Log.d("QwantProvider", "Result: $result")
        return SuggestionsResult(query).apply {
            networkItems =
                if (result.status == "success") {
                    result.data?.items?.map { it.value }?.take(3) ?: emptyList()
                } else emptyList()
        }
    }
}
