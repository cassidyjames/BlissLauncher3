/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.suggestions

interface SuggestionProvider {
    suspend fun query(query: String): SuggestionsResult
}

enum class Providers(val key: String, val url: String) {
    DUCKDUCKGO("duckduckgo", "https://duckduckgo.com/"),
    QWANT("qwant", "https://www.qwant.com/"),
    SPOT("spot", "https://spot.murena.io/"),
    MOJEEK("mojeek", "https://www.mojeek.com/")
}