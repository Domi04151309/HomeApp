package io.github.domi04151309.home.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

@Suppress("TooManyFunctions")
class MainListAdapter(private var attachedTo: RecyclerView) : RecyclerView.Adapter<MainListAdapter.ViewHolder>() {
    private var items: MutableList<ListViewItem> = mutableListOf()
    private var helperInterface: HomeRecyclerViewHelperInterface? = null
    private var animate: Boolean = true
    private var offsets: IntArray = intArrayOf()

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

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.drawable.setImageResource(items[position].icon)
        holder.title.text = items[position].title
        holder.summary.text = items[position].summary
        holder.hidden.text = items[position].hidden

        val id = getItemId(position)
        if (items[position].state != null) {
            holder.stateSwitch.isChecked = items[position].state ?: false
            holder.stateSwitch.setOnCheckedChangeListener { compoundButton, b ->
                if (compoundButton.isPressed) {
                    helperInterface?.onStateChanged(
                        holder.itemView,
                        items[getPosFromId(id)],
                        b,
                    )
                }
            }
        } else {
            holder.stateSwitch.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            helperInterface?.onItemClicked(holder.itemView, items[getPosFromId(id)])
        }

        holder.itemView.setOnCreateContextMenuListener(holder.itemView.context as Activity)
        if (animate) playAnimation(holder.itemView)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.stateSwitch.visibility = View.VISIBLE
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = items.size

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedTo = recyclerView
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(
        newItems: List<ListViewItem>,
        newHelperInterface: HomeRecyclerViewHelperInterface? = null,
        preferredAnimationState: Boolean? = null,
    ) {
        offsets = IntArray(newItems.size) { 1 }
        if (newHelperInterface != null) {
            attachedTo.layoutManager?.scrollToPosition(0)
            animate = preferredAnimationState ?: true
            items.clear()
            items.addAll(newItems)
            helperInterface = newHelperInterface
            notifyDataSetChanged()
        } else {
            animate = preferredAnimationState ?: false
            val changed = mutableListOf<Int>()
            items.clear()
            for (i in 0 until items.size) {
                if (items[i] != newItems[i]) changed.add(i)
                items.add(newItems[i])
            }
            changed.forEach(::notifyItemChanged)
        }
    }

    fun updateSwitch(
        position: Int,
        state: Boolean,
        dynamicSummary: Boolean,
    ) {
        if (position > items.size - 1) {
            Log.w(Global.LOG_TAG, "The position $position with value $state is larger than the item count")
            return
        }
        if (items[position].state != state) {
            items[position].state = state
            val viewHolder = attachedTo.findViewHolderForAdapterPosition(position) as? ViewHolder
            if (viewHolder == null) {
                notifyItemChanged(position)
            } else {
                viewHolder.stateSwitch.isChecked = state
                if (dynamicSummary) {
                    viewHolder.summary.text =
                        attachedTo.context.resources.getString(
                            if (state) {
                                R.string.switch_summary_on
                            } else {
                                R.string.switch_summary_off
                            },
                        )
                }
            }
        }
    }

    fun getOffset(pos: Int): Int = offsets.copyOfRange(0, pos).sum()

    fun updateDirectView(
        id: String,
        newItems: List<ListViewItem>,
        directViewPos: Int,
    ) {
        newItems.forEach { it.hidden = id + '@' + it.hidden }

        val correctOffset = getOffset(directViewPos)
        val directViewSize = offsets[directViewPos]
        for (i in 0 until directViewSize) {
            items.removeAt(correctOffset)
            notifyItemRemoved(correctOffset)
        }
        offsets[directViewPos] = newItems.size
        items.addAll(correctOffset, newItems)
        notifyItemRangeInserted(correctOffset, newItems.size)
    }

    private fun getPosFromId(id: Long): Int = items.indices.indexOfFirst { getItemId(it) == id }

    fun getDirectViewPos(deviceId: String): Int {
        var currentPos = 0
        for (i in offsets.indices) {
            if (items[currentPos].hidden.contains(deviceId)) return i
            currentPos += offsets[i]
        }
        return -1
    }

    private fun playAnimation(v: View) {
        val set = AnimationSet(true)

        val firstAnimation: Animation = AlphaAnimation(0.0f, 1.0f)
        firstAnimation.duration = ANIMATION_DURATION
        set.addAnimation(firstAnimation)

        val secondAnimation =
            TranslateAnimation(
                Animation.RELATIVE_TO_SELF,
                -1.0f,
                Animation.RELATIVE_TO_SELF,
                0.0f,
                Animation.RELATIVE_TO_SELF,
                0.0f,
                Animation.RELATIVE_TO_SELF,
                0.0f,
            )
        secondAnimation.duration = ANIMATION_DURATION
        set.addAnimation(secondAnimation)

        v.startAnimation(set)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val drawable: ImageView = view.findViewById(R.id.drawable)
        val title: TextView = view.findViewById(R.id.title)
        val summary: TextView = view.findViewById(R.id.summary)
        val hidden: TextView = view.findViewById(R.id.hidden)
        val stateSwitch: MaterialSwitch = view.findViewById(R.id.state)
    }

    companion object {
        private const val ANIMATION_DURATION = 300L
    }
}
