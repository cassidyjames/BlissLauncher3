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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R
import java.util.Locale

class AutoCompleteAdapter(private val context: Context) :
    RecyclerView.Adapter<AutoCompleteAdapter.AutoCompleteViewHolder>() {
    lateinit var mOnSuggestionClick: (String) -> Unit
    private var mItems: List<String?> = emptyList()
    private var mQueryText: String? = null

    class AutoCompleteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mSuggestionTextView: TextView? = itemView.findViewById(R.id.suggestionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AutoCompleteViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_suggestion, parent, false)
        val holder = AutoCompleteViewHolder(view)
        view.setOnClickListener { mItems[holder.absoluteAdapterPosition]?.let(mOnSuggestionClick) }
        return holder
    }

    override fun onBindViewHolder(holder: AutoCompleteViewHolder, position: Int) {
        val suggestion = mItems[position] ?: ""
        if (mQueryText != null) {
            val spannable = SpannableStringBuilder(suggestion)
            val lcSuggestion = suggestion.lowercase(Locale.getDefault())
            var queryTextPos = lcSuggestion.indexOf(mQueryText!!)
            while (queryTextPos >= 0) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    queryTextPos,
                    queryTextPos + mQueryText!!.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                queryTextPos =
                    lcSuggestion.indexOf(mQueryText!!, queryTextPos + mQueryText!!.length)
            }
            holder.mSuggestionTextView!!.text = spannable
        } else {
            holder.mSuggestionTextView!!.text = suggestion
        }
        setFadeAnimation(holder.itemView)
    }

    override fun getItemCount() = mItems.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateSuggestions(suggestions: List<String?>, queryText: String?) {
        mItems = suggestions
        mQueryText = queryText
        notifyDataSetChanged()
    }

    private fun setFadeAnimation(view: View) {
        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 300
        view.startAnimation(anim)
    }
}
