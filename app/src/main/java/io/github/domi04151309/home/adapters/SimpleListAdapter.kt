package io.github.domi04151309.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.SimpleListItem

internal class SimpleListAdapter(
        private val itemArray: ArrayList<SimpleListItem>
) : BaseAdapter() {

    override fun getCount(): Int {
        return itemArray.size
    }

    override fun getItem(position: Int): SimpleListItem {
        return itemArray[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vi: View = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.list_item_simple, parent, false)
        vi.findViewById<ImageView>(R.id.drawable).setImageResource(itemArray[position].icon)
        vi.findViewById<TextView>(R.id.title).text = itemArray[position].title
        vi.findViewById<TextView>(R.id.summary).text = itemArray[position].summary
        vi.findViewById<TextView>(R.id.hidden).text = itemArray[position].hidden
        return vi
    }
}