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

interface SuggestionProvider {
    suspend fun query(query: String): SuggestionsResult
}

enum class Providers(val key: String, val url: String) {
    DUCKDUCKGO("duckduckgo", "https://duckduckgo.com/"),
    QWANT("qwant", "https://www.qwant.com/"),
    SPOT("spot", "https://spot.murena.io/"),
    MOJEEK("mojeek", "https://www.mojeek.com/")
}
