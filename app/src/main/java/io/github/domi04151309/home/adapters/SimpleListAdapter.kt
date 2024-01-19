package io.github.domi04151309.home.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class SimpleListAdapter(
    private val items: List<SimpleListItem>,
    private val helperInterface: RecyclerViewHelperInterface,
) : RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_simple, parent, false),
        )

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.drawable.setImageResource(items[position].icon)
        holder.title.text = items[position].title
        holder.summary.text = items[position].summary
        holder.hidden.text = items[position].hidden
        holder.itemView.setOnClickListener { helperInterface.onItemClicked(holder.itemView, position) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val drawable: ImageView = view.findViewById(R.id.drawable)
        val title: TextView = view.findViewById(R.id.title)
        val summary: TextView = view.findViewById(R.id.summary)
        val hidden: TextView = view.findViewById(R.id.hidden)
    }
}
