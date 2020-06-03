package io.github.domi04151309.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import io.github.domi04151309.home.data.ListViewItem

internal class ListViewAdapter(context: Context, private val itemArray: ArrayList<ListViewItem>, private val animate: Boolean = true) : BaseAdapter() {

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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val vi: View = convertView ?: inflater.inflate(R.layout.list_item, parent, false)
        if (animate) playAnimation(vi)
        val drawableView = vi.findViewById<ImageView>(R.id.drawable)
        val titleTxt = vi.findViewById<TextView>(R.id.title)
        val summaryTxt = vi.findViewById<TextView>(R.id.summary)
        val hiddenTxt = vi.findViewById<TextView>(R.id.hidden)
        val stateSwitch = vi.findViewById<Switch>(R.id.state)
        titleTxt.text = itemArray[position].title
        summaryTxt.text = itemArray[position].summary
        hiddenTxt.text = itemArray[position].hidden
        try {
            drawableView.setImageResource(itemArray[position].icon)
        } catch (e: Exception) {
            drawableView.setImageResource(android.R.color.transparent)
        }
        if (itemArray[position].state == null) {
            stateSwitch.visibility = View.GONE
        } else {
            stateSwitch.isChecked = itemArray[position].state ?: false
            stateSwitch.setOnCheckedChangeListener(itemArray[position].stateListener)
        }
        return vi
    }

    private fun playAnimation(v: View) {
        val set = AnimationSet(true)

        val firstAnimation: Animation = AlphaAnimation(0.0f, 1.0f)
        firstAnimation.duration = 300
        set.addAnimation(firstAnimation)

        val secondAnimation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        secondAnimation.duration = 300
        set.addAnimation(secondAnimation)

        v.startAnimation(set)
    }
}