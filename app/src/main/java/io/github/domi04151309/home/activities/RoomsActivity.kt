package io.github.domi04151309.home.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.DeviceListAdapter
import io.github.domi04151309.home.data.RoomItem
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.Rooms
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterfaceAdvanced

class RoomsActivity : BaseActivity(), RecyclerViewHelperInterfaceAdvanced {
    private var reset = true
    private lateinit var rooms: Rooms
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val itemTouchHelperCallback =
        object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ): Int =
                if (
                    viewHolder.bindingAdapterPosition == (recyclerView.adapter?.itemCount ?: -1) - 1
                ) {
                    makeMovementFlags(0, 0)
                } else {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val adapter = recyclerView.adapter ?: return false
                return if (target.bindingAdapterPosition == adapter.itemCount - 1) {
                    false
                } else {
                    recyclerView.adapter?.notifyItemMoved(
                        viewHolder.bindingAdapterPosition,
                        target.bindingAdapterPosition,
                    )
                    rooms.moveRoom(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                    true
                }
            }

            override fun isLongPressDragEnabled(): Boolean = true

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int,
            ) {
                // Do nothing.
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ) {
                super.clearView(recyclerView, viewHolder)
                rooms.saveChanges()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.rooms_title)

        rooms = Rooms(this)
        recyclerView = findViewById(R.id.recyclerView)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)

        applyBottomInsetPadding(recyclerView)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val fab = findViewById<FloatingActionButton>(R.id.roomsFab)
        fab.setImageResource(R.drawable.ic_add)
        fab.setOnClickListener {
            reset = true
            startActivity(Intent(this, EditRoomActivity::class.java))
        }
    }

    private fun loadRooms() {
        val listItems: ArrayList<SimpleListItem> = ArrayList(rooms.length)
        var currentRoom: RoomItem
        for (i in 0 until rooms.length) {
            currentRoom = rooms.getRoomByIndex(i)
            listItems +=
                SimpleListItem(
                    title = currentRoom.name,
                    summary = "",
                    hidden = "edit#${currentRoom.id}",
                    icon = currentRoom.iconId,
                )
        }
        listItems +=
            SimpleListItem(
                title = resources.getString(R.string.rooms_add),
                summary = resources.getString(R.string.rooms_add_summary),
                hidden = "add",
                icon = R.drawable.ic_add,
            )

        recyclerView.adapter = DeviceListAdapter(listItems, this)
    }

    override fun onItemClicked(
        view: View,
        position: Int,
    ) {
        val action = view.findViewById<TextView>(R.id.hidden).text
        if (action.contains("edit")) {
            reset = true
            startActivity(
                Intent(this, EditRoomActivity::class.java)
                    .putExtra("roomId", action.substring(action.indexOf('#') + 1)),
            )
        } else if (action == "add") {
            reset = true
            startActivity(Intent(this, EditRoomActivity::class.java))
        }
    }

    override fun onItemHandleTouched(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onStart() {
        super.onStart()
        if (reset) {
            reset = false
            loadRooms()
        }
    }
}
