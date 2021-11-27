package io.github.domi04151309.home.adapters

import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.SceneGridItem

internal class HueSceneGridAdapter() : BaseAdapter() {

    private var items: List<SceneGridItem> = arrayListOf()

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): SceneGridItem {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vi: View = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.grid_item, parent, false)
        vi.findViewById<TextView>(R.id.name).text = items[position].name
        vi.findViewById<TextView>(R.id.hidden).text = items[position].hidden
        val drawableView = vi.findViewById<ImageView>(R.id.drawable)
        if (items[position].color == null) {
            drawableView.setImageResource(R.drawable.ic_hue_scene_add)
        } else {
            val finalDrawable = LayerDrawable(arrayOf(
                ContextCompat.getDrawable(parent.context, R.drawable.ic_hue_scene_base),
                ContextCompat.getDrawable(parent.context, R.drawable.ic_hue_scene_color)
            ))
            finalDrawable.getDrawable(1).setTint(items[position].color ?: Color.WHITE)
            drawableView.setImageDrawable(finalDrawable)
        }
        return vi
    }

    fun updateData(newItems: List<SceneGridItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}