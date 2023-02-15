/*
 * Copyright © MURENA SAS 2023.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
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
