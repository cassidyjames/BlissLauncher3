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
package foundation.e.bliss.suggestions.qwant

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class QwantData(
    @SerializedName("items") val items: List<QwantItem>,
    @SerializedName("special") val special: List<Any>
)

@Keep
data class QwantItem(
    @SerializedName("value") val value: String? = null,
    @SerializedName("suggestType") val suggestType: Int? = null
)

@Keep
data class QwantResult(
    @SerializedName("status") val status: String? = null,
    @SerializedName("data") val data: QwantData? = null
)
