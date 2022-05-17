package io.github.domi04151309.home.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.github.domi04151309.home.R
import android.view.LayoutInflater
import androidx.core.widget.ImageViewCompat
import io.github.domi04151309.home.data.SimpleListItem

class HueSceneLampListAdapter(
    private var items: ArrayList<Pair<SimpleListItem, Int>>
) : RecyclerView.Adapter<HueSceneLampListAdapter.ViewHolder>() {

    lateinit var c: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        c = parent.context
        return ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_simple, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.drawable.setImageResource(items[position].first.icon)
        holder.title.text = items[position].first.title
        holder.summary.text = items[position].first.summary
        holder.hidden.text = items[position].first.hidden
        ImageViewCompat.setImageTintList(
            holder.drawable,
            ColorStateList.valueOf(items[position].second)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun changeSceneBrightness(brightness: String) {
        for (i in 0 until items.size) {
            if (items[i].first.summary.contains(c.resources.getString(R.string.str_on))) {
                items[i].first.summary = items[i].first.summary.substring(
                    0,
                    items[i].first.summary.lastIndexOf(' ') + 1
                ) + brightness
                notifyItemChanged(i)
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val drawable: ImageView = view.findViewById(R.id.drawable)
        val title: TextView = view.findViewById(R.id.title)
        val summary: TextView = view.findViewById(R.id.summary)
        val hidden: TextView = view.findViewById(R.id.hidden)
    }
}