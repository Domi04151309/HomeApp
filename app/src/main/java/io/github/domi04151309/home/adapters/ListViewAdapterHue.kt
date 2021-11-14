package io.github.domi04151309.home.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem

internal class ListViewAdapterHue(
        context: Context,
        private val itemArray: ArrayList<ListViewItem>,
        private var animate: Boolean = true
) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return itemArray.size
    }

    override fun getItem(position: Int): ListViewItem {
        return itemArray[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun updateSwitch(position: Int, state: Boolean) {
        animate = false
        itemArray[position].state = state
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vi: View = convertView ?: inflater.inflate(R.layout.list_item_layered, parent, false)
        vi.findViewById<ImageView>(R.id.drawable).setImageResource(R.drawable.ic_hue_lamp_color)
        vi.findViewById<ImageView>(R.id.base).setImageResource(R.drawable.ic_hue_lamp_base)
        val stateSwitch = vi.findViewById<Switch>(R.id.state)
        vi.findViewById<TextView>(R.id.title).text = itemArray[position].title
        vi.findViewById<TextView>(R.id.summary).text = itemArray[position].summary
        vi.findViewById<TextView>(R.id.hidden).text = itemArray[position].hidden
        if (itemArray[position].state == null) {
            stateSwitch.visibility = View.GONE
        } else {
            stateSwitch.isChecked = itemArray[position].state ?: false
            stateSwitch.setOnCheckedChangeListener(itemArray[position].stateListener)
        }
        return vi
    }
}