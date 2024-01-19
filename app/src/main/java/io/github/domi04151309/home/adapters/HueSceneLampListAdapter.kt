package io.github.domi04151309.home.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.SceneListItem
import io.github.domi04151309.home.interfaces.SceneRecyclerViewHelperInterface

class HueSceneLampListAdapter(
    private var items: List<SceneListItem>,
    private var helperInterface: SceneRecyclerViewHelperInterface,
) : RecyclerView.Adapter<HueSceneLampListAdapter.ViewHolder>() {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long =
        (position.toString() + '#' + items[position].hidden)
            .hashCode()
            .toLong()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item, parent, false),
        )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val id = getItemId(position)
        holder.drawable.setImageResource(R.drawable.ic_circle)
        holder.title.text = items[position].title
        holder.summary.text = generateSummary(holder.itemView.resources, items[position])
        holder.hidden.text = items[position].hidden
        holder.stateSwitch.isChecked = items[position].state
        holder.stateSwitch.setOnCheckedChangeListener { compoundButton, b ->
            items[getPosFromId(id)].state = b
            holder.summary.text = generateSummary(holder.itemView.resources, items[getPosFromId(id)])
            if (compoundButton.isPressed) {
                helperInterface.onStateChanged(
                    holder.itemView,
                    items[getPosFromId(id)],
                    b,
                )
            }
        }
        ImageViewCompat.setImageTintList(
            holder.drawable,
            ColorStateList.valueOf(items[position].color),
        )
        holder.itemView.setOnClickListener {
            helperInterface.onItemClicked(holder.itemView, items[getPosFromId(id)])
        }
    }

    override fun getItemCount(): Int = items.size

    fun changeSceneBrightness(brightness: String) {
        for (i in items.indices) {
            items[i].brightness = brightness
            if (items[i].state) notifyItemChanged(i)
        }
    }

    fun updateBrightness(
        id: String,
        brightness: String,
    ) {
        val i = items.indexOfFirst { it.hidden == id }
        items[i].brightness = brightness
        if (items[i].state) notifyItemChanged(i)
    }

    fun updateColor(
        id: String,
        color: Int,
    ) {
        val i = items.indexOfFirst { it.hidden == id }
        items[i].color = color
        notifyItemChanged(i)
    }

    private fun generateSummary(
        resources: Resources,
        item: SceneListItem,
    ): String =
        resources.getString(
            if (item.state) R.string.str_on else R.string.str_off,
        ) +
            " · " + resources.getString(R.string.hue_brightness) +
            ": " + if (item.state) item.brightness else "0%"

    private fun getPosFromId(id: Long): Int = items.indices.indexOfFirst { getItemId(it) == id }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val drawable: ImageView = view.findViewById(R.id.drawable)
        val title: TextView = view.findViewById(R.id.title)
        val summary: TextView = view.findViewById(R.id.summary)
        val hidden: TextView = view.findViewById(R.id.hidden)
        val stateSwitch: MaterialSwitch = view.findViewById(R.id.state)
    }
}
