package io.github.domi04151309.home.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.DeviceListAdapter
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterfaceAdvanced

class DevicesActivity : BaseActivity(), RecyclerViewHelperInterfaceAdvanced {
    private var reset = true
    private lateinit var devices: Devices
    private lateinit var recyclerView: RecyclerView
    private lateinit var itemTouchHelper: ItemTouchHelper

    private val itemTouchHelperCallback =
        object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ): Int =
                if (
                    viewHolder.adapterPosition == (recyclerView.adapter?.itemCount ?: -1) - 1
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
                return if (target.adapterPosition == adapter.itemCount - 1) {
                    false
                } else {
                    recyclerView.adapter?.notifyItemMoved(
                        viewHolder.adapterPosition,
                        target.adapterPosition,
                    )
                    devices.moveDevice(viewHolder.adapterPosition, target.adapterPosition)
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
                devices.saveChanges()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        devices = Devices(this)
        recyclerView = findViewById(R.id.recyclerView)
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)

        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDevices() {
        val listItems: ArrayList<SimpleListItem> = ArrayList(devices.length)
        var currentDevice: DeviceItem
        for (i in 0 until devices.length) {
            currentDevice = devices.getDeviceByIndex(i)
            listItems +=
                SimpleListItem(
                    title = currentDevice.name,
                    summary =
                        if (currentDevice.hide) {
                            resources.getString(R.string.device_config_hidden) + " · " + currentDevice.address
                        } else {
                            currentDevice.address
                        },
                    hidden = "edit#${currentDevice.id}",
                    icon = currentDevice.iconId,
                )
        }
        listItems +=
            SimpleListItem(
                title = resources.getString(R.string.pref_add),
                summary = resources.getString(R.string.pref_add_summary),
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
                Intent(this, EditDeviceActivity::class.java)
                    .putExtra("deviceId", action.substring(action.indexOf('#') + 1)),
            )
        } else if (action == "add") {
            reset = true
            MaterialAlertDialogBuilder(this)
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
        if (reset) {
            reset = false
            loadDevices()
        }
    }
}
