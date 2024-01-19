package io.github.domi04151309.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class DeviceDiscoveryListAdapter(
    private val items: MutableList<ListViewItem>,
    private val helperInterface: RecyclerViewHelperInterface,
) : RecyclerView.Adapter<DeviceDiscoveryListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_device_discovery, parent, false),
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.drawable.setImageResource(items[position].icon)
        holder.title.text = items[position].title
        holder.summary.text = items[position].summary
        holder.hidden.text = items[position].hidden
        holder.stateDrawable.setImageResource(
            if (items[position].state == true) {
                R.drawable.ic_done
            } else {
                android.R.color.transparent
            },
        )
        holder.itemView.setOnClickListener { helperInterface.onItemClicked(holder.itemView, position) }
    }

    override fun getItemCount(): Int = items.size

    fun add(item: ListViewItem): Int {
        items.add(item)
        notifyItemInserted(items.size - 1)
        return items.size - 1
    }

    fun changeState(
        i: Int,
        state: Boolean,
    ) {
        items[i].state = state
        notifyItemChanged(i)
    }

    fun changeTitle(
        i: Int,
        title: String,
    ) {
        items[i].title = title
        notifyItemChanged(i)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val drawable: ImageView = view.findViewById(R.id.drawable)
        val title: TextView = view.findViewById(R.id.title)
        val summary: TextView = view.findViewById(R.id.summary)
        val hidden: TextView = view.findViewById(R.id.hidden)
        val stateDrawable: ImageView = view.findViewById(R.id.state)
    }
}
