package io.github.domi04151309.home.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat

import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ScenesGridItem

internal class HueScenesGridAdapter(context: Context, private val itemArray: List<ScenesGridItem>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return itemArray.size
    }

    override fun getItem(position: Int): ScenesGridItem {
        return itemArray[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vi: View = convertView ?: inflater.inflate(R.layout.grid_item, parent, false)
        vi.findViewById<TextView>(R.id.name).text = itemArray[position].name
        vi.findViewById<TextView>(R.id.hidden).text = itemArray[position].hidden
        vi.findViewById<ImageView>(R.id.base).setImageResource(itemArray[position].icon)
        if (itemArray[position].color != null) {
            val drawableView = vi.findViewById<ImageView>(R.id.drawable)
            drawableView.setImageResource(R.drawable.ic_hue_scene_color)
            ImageViewCompat.setImageTintList(drawableView, ColorStateList.valueOf(itemArray[position].color ?: Color.WHITE))
        }
        return vi
    }
}