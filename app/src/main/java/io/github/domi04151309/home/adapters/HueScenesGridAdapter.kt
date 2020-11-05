package io.github.domi04151309.home.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ScenesGridItem

internal class HueScenesGridAdapter(context: Context, private val itemArray: ArrayList<ScenesGridItem>) : BaseAdapter() {

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
        vi.findViewById<ImageView>(R.id.drawable).setImageResource(itemArray[position].icon)
        vi.findViewById<TextView>(R.id.name).text = itemArray[position].name
        vi.findViewById<TextView>(R.id.hidden).text = itemArray[position].hidden
        return vi
    }
}