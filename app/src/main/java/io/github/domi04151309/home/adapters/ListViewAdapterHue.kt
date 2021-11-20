package io.github.domi04151309.home.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem

internal class ListViewAdapterHue(
        private val itemArray: ArrayList<ListViewItem>,
        private val colorArray: ArrayList<Int>
) : BaseAdapter() {

    override fun getCount(): Int {
        return itemArray.size
    }

    override fun getItem(position: Int): ListViewItem {
        return itemArray[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vi: View = convertView
            ?: LayoutInflater.from(parent.context).inflate(R.layout.list_item_layered, parent, false)
        val drawableView = vi.findViewById<ImageView>(R.id.drawable)
        drawableView.setImageResource(R.drawable.ic_hue_lamp_color)
        ImageViewCompat.setImageTintList(drawableView, ColorStateList.valueOf(colorArray[position]))
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