/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package foundation.e.bliss.suggestions.duckduckgo

import retrofit2.http.GET
import retrofit2.http.Query

interface DuckDuckGoApi {
    @GET("ac/") suspend fun query(@Query("q") query: String?): List<DuckDuckGoResult?>

    companion object {
        const val BASE_URL = "https://duckduckgo.com/"
    }
}
