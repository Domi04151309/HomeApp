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

internal class HueSceneGridAdapter(private val itemArray: List<SceneGridItem>) : BaseAdapter() {

    override fun getCount(): Int {
        return itemArray.size
    }

    override fun getItem(position: Int): SceneGridItem {
        return itemArray[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vi: View = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.grid_item, parent, false)
        vi.findViewById<TextView>(R.id.name).text = itemArray[position].name
        vi.findViewById<TextView>(R.id.hidden).text = itemArray[position].hidden
        val drawableView = vi.findViewById<ImageView>(R.id.drawable)
        if (itemArray[position].color == null) {
            drawableView.setImageResource(R.drawable.ic_hue_scene_add)
        } else {
            val finalDrawable = LayerDrawable(arrayOf(
                ContextCompat.getDrawable(parent.context, R.drawable.ic_hue_scene_base),
                ContextCompat.getDrawable(parent.context, R.drawable.ic_hue_scene_color)
            ))
            finalDrawable.getDrawable(1).setTint(itemArray[position].color ?: Color.WHITE)
            drawableView.setImageDrawable(finalDrawable)
        }
        return vi
    }
}