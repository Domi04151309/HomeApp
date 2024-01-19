package io.github.domi04151309.home.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.Global

internal class IconSpinnerAdapter(
    private var itemArray: Array<String>,
) : BaseAdapter(), Filterable {
    override fun getCount(): Int = itemArray.size

    override fun getItem(position: Int): String = itemArray[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = ItemFilter()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val vi: View =
            convertView
                ?: LayoutInflater.from(parent.context).inflate(R.layout.icon_dropdown_item, parent, false)
        vi.findViewById<ImageView>(R.id.drawable).setImageResource(Global.getIcon(itemArray[position]))
        vi.findViewById<TextView>(R.id.title).text = itemArray[position]
        return vi
    }

    inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val results = FilterResults()
            val search = constraint.toString().lowercase()

            val items: ArrayList<String> = ArrayList(itemArray.size)

            for (i in itemArray.indices) {
                if (itemArray[i].lowercase().contains(search)) items.add(itemArray[i])
            }

            results.values = items.toArray()
            results.count = items.size
            return results
        }

        override fun publishResults(
            constraint: CharSequence,
            results: FilterResults,
        ) {
            itemArray = (results.values as Array<*>).filterIsInstance<String>().toTypedArray()
            notifyDataSetChanged()
        }
    }
}
