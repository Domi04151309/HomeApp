package io.github.domi04151309.home.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class HueLampListAdapter(
    private val stateListener: CompoundButton.OnCheckedChangeListener,
    private val helperInterface: RecyclerViewHelperInterface,
) : RecyclerView.Adapter<HueLampListAdapter.ViewHolder>() {
    private var items: List<ListViewItem> = mutableListOf()
    private var colors: List<Int> = mutableListOf()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item, parent, false),
        )

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val context = holder.itemView.context
        val finalDrawable =
            LayerDrawable(
                arrayOf(
                    ContextCompat.getDrawable(context, R.drawable.ic_hue_lamp_base),
                    ContextCompat.getDrawable(context, R.drawable.ic_hue_lamp_color),
                ),
            )
        finalDrawable.getDrawable(1).setTint(colors[position])
        holder.drawable.setImageDrawable(finalDrawable)
        holder.title.text = items[position].title
        holder.summary.text = items[position].summary
        holder.hidden.text = items[position].hidden
        holder.stateSwitch.isChecked = items[position].state ?: false
        holder.stateSwitch.setOnCheckedChangeListener(stateListener)
        holder.itemView.setOnClickListener { helperInterface.onItemClicked(holder.itemView, position) }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(
        recyclerView: RecyclerView,
        newItems: List<ListViewItem>,
        newColors: List<Int>,
    ) {
        if (newItems.size != items.size) {
            items = newItems
            colors = newColors
            notifyDataSetChanged()
            return
        }

        val changed = mutableListOf<Int>()
        for (i in items.indices) {
            if (items[i].hidden != newItems[i].hidden) {
                changed.add(i)
            } else {
                val holder = (recyclerView.findViewHolderForAdapterPosition(i) ?: return) as ViewHolder
                if (items[i].state != newItems[i].state) {
                    holder.stateSwitch.isChecked = newItems[i].state ?: false
                }
                if (colors[i] != newColors[i]) {
                    val context = holder.itemView.context
                    val finalDrawable =
                        LayerDrawable(
                            arrayOf(
                                ContextCompat.getDrawable(context, R.drawable.ic_hue_lamp_base),
                                ContextCompat.getDrawable(context, R.drawable.ic_hue_lamp_color),
                            ),
                        )
                    finalDrawable.getDrawable(1).setTint(newColors[i])
                    holder.drawable.setImageDrawable(finalDrawable)
                }
            }
        }
        items = newItems
        colors = newColors
        changed.forEach(::notifyItemChanged)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val drawable: ImageView = view.findViewById(R.id.drawable)
        val title: TextView = view.findViewById(R.id.title)
        val summary: TextView = view.findViewById(R.id.summary)
        val hidden: TextView = view.findViewById(R.id.hidden)
        val stateSwitch: MaterialSwitch = view.findViewById(R.id.state)
    }
}
