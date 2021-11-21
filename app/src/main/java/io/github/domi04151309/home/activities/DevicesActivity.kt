package io.github.domi04151309.home.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.DeviceListAdapter
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class DevicesActivity : AppCompatActivity(), RecyclerViewHelperInterface {

    private var reset = true
    private lateinit var devices: Devices
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val itemTouchHelperCallback = object: ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return if (viewHolder.adapterPosition == (recyclerView.adapter?.itemCount ?: -1) - 1) makeMovementFlags( 0, 0)
            else makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val adapter = recyclerView.adapter ?: return false
            return if (target.adapterPosition == adapter.itemCount - 1) {
                false
            } else {
                recyclerView.adapter?.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                devices.moveDevice(viewHolder.adapterPosition, target.adapterPosition)
                true
            }
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            devices.saveChanges()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        devices = Devices(this)
        recyclerView = findViewById(R.id.recyclerView)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)

        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDevices(){
        val listItems: ArrayList<SimpleListItem> = ArrayList(devices.length())
        try {
            if (devices.length() == 0) {
                listItems += SimpleListItem(
                        title = resources.getString(R.string.main_no_devices),
                        hidden = "none"
                )
            } else {
                var currentDevice: DeviceItem
                for (i in 0 until devices.length()) {
                    currentDevice = devices.getDeviceByIndex(i)
                    listItems += SimpleListItem(
                            title = currentDevice.name,
                            summary = currentDevice.address,
                            hidden = "edit",
                            icon = currentDevice.iconId
                    )
                }
            }
            listItems += SimpleListItem(
                    title = resources.getString(R.string.pref_add),
                    summary = resources.getString(R.string.pref_add_summary),
                    hidden = "add",
                    icon = R.drawable.ic_add
            )
        } catch (e: Exception) {
            listItems += SimpleListItem(
                    title = resources.getString(R.string.err_wrong_format),
                    summary = resources.getString(R.string.err_wrong_format_summary),
                    hidden = "none",
                    icon = R.drawable.ic_warning
            )
            Log.e(Global.LOG_TAG, e.toString())
        }

        recyclerView.adapter = DeviceListAdapter(listItems, this)
    }

    override fun onItemClicked(view: View, pos: Int) {
        val action =  view.findViewById<TextView>(R.id.hidden).text
        if (action == "edit") {
            reset = true
            startActivity(Intent(this, EditDeviceActivity::class.java).putExtra("deviceId", devices.getDeviceByIndex(pos).id))
        } else if (action == "add") {
            reset = true
            AlertDialog.Builder(this)
                .setTitle(R.string.pref_add_method)
                .setItems(resources.getStringArray(R.array.pref_add_method_array)) { _, which ->
                    if (which == 0) {
                        startActivity(Intent(this, EditDeviceActivity::class.java))
                    } else if (which == 1) {
                        startActivity(Intent(this, SearchDevicesActivity::class.java))
                    }
                }
                .show()
        }
    }

    override fun onItemHandleTouched(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onStart() {
        super.onStart()
        if (reset){
            reset = false
            loadDevices()
        }
    }
}
