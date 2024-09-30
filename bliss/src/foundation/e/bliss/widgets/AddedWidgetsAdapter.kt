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
package foundation.e.bliss.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R

@Keep data class BlissWidget(val id: Int, var info: AppWidgetProviderInfo)

class AddedWidgetsAdapter(private val mContext: Context, private val mDensity: Int) :
    RecyclerView.Adapter<AddedWidgetsAdapter.WidgetsViewHolder>() {
    private var mAppWidgetProviderInfos = mutableListOf<BlissWidget>()
    private val mOnActionClickListener: OnActionClickListener

    init {
        mOnActionClickListener = mContext as OnActionClickListener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): WidgetsViewHolder {
        val view: View =
            LayoutInflater.from(mContext).inflate(R.layout.item_added_widget, viewGroup, false)
        val widgetsViewHolder = WidgetsViewHolder(view)
        widgetsViewHolder.actionBtn.setImageResource(R.drawable.ic_remove_widget_red_24dp)
        widgetsViewHolder.actionBtn.setOnClickListener {
            val position = widgetsViewHolder.absoluteAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val widget = mAppWidgetProviderInfos[position]
                mAppWidgetProviderInfos.removeAt(position)
                mOnActionClickListener.removeWidget(widget.id)
                notifyItemRemoved(position)
            }
        }
        return widgetsViewHolder
    }

    override fun onBindViewHolder(widgetsViewHolder: WidgetsViewHolder, i: Int) {
        val info: AppWidgetProviderInfo = mAppWidgetProviderInfos[i].info
        widgetsViewHolder.icon.setImageDrawable(info.loadIcon(mContext, mDensity))
        widgetsViewHolder.label.text = info.loadLabel(mContext.packageManager)
    }

    override fun getItemCount(): Int {
        return mAppWidgetProviderInfos.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAppWidgetProviderInfos(appWidgetProviderInfos: List<BlissWidget>) {
        mAppWidgetProviderInfos = appWidgetProviderInfos.toMutableList()
        notifyDataSetChanged()
    }

    class WidgetsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView
        var label: TextView
        var actionBtn: ImageView

        init {
            icon = itemView.findViewById(R.id.widget_icon)!!
            label = itemView.findViewById(R.id.widget_label)!!
            actionBtn = itemView.findViewById(R.id.action_image_view)!!
        }
    }

    internal interface OnActionClickListener {
        fun removeWidget(id: Int)
    }
}
