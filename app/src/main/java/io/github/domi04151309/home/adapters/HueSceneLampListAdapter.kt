package io.github.domi04151309.home.adapters

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
    private val items: ArrayList<SimpleListItem>,
    private val colors: ArrayList<Int>
    ) : RecyclerView.Adapter<HueSceneLampListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_simple, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.drawable.setImageResource(items[position].icon)
        holder.title.text = items[position].title
        holder.summary.text = items[position].summary
        holder.hidden.text = items[position].hidden
        ImageViewCompat.setImageTintList(holder.drawable, ColorStateList.valueOf(colors[position]))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var drawable: ImageView = view.findViewById(R.id.drawable)
        var title: TextView = view.findViewById(R.id.title)
        var summary: TextView = view.findViewById(R.id.summary)
        var hidden: TextView = view.findViewById(R.id.hidden)
    }
}