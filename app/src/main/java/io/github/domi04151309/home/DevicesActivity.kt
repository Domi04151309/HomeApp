package io.github.domi04151309.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import android.view.View
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.ListViewAdapter
import io.github.domi04151309.home.objects.Global
import io.github.domi04151309.home.objects.Theme

class DevicesActivity : AppCompatActivity() {

    private var reset = true
    private lateinit var devices: Devices
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        devices = Devices(this)
        listView = findViewById<View>(R.id.listView) as ListView

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, pos, _ ->
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
    }

    private fun loadDevices(){
        val listItems: ArrayList<ListViewItem> = ArrayList(devices.length())
        try {
            if (devices.length() == 0) {
                val emptyItem = ListViewItem(resources.getString(R.string.main_no_devices))
                emptyItem.hidden = "none"
                listItems += emptyItem
            } else {
                var currentDevice: DeviceItem
                for (i in 0 until devices.length()) {
                    currentDevice = devices.getDeviceByIndex(i)
                    val deviceItem = ListViewItem(currentDevice.name)
                    deviceItem.summary = currentDevice.address
                    deviceItem.hidden = "edit"
                    deviceItem.icon = currentDevice.iconId
                    listItems += deviceItem
                }
            }
            val addItem = ListViewItem(resources.getString(R.string.pref_add))
            addItem.summary = resources.getString(R.string.pref_add_summary)
            addItem.hidden = "add"
            addItem.icon = R.drawable.ic_add
            listItems += addItem
        } catch (e: Exception) {
            val errorItem = ListViewItem(resources.getString(R.string.err_wrong_format))
            errorItem.summary = resources.getString(R.string.err_wrong_format_summary)
            errorItem.hidden = "none"
            errorItem.icon = R.drawable.ic_warning
            listItems += errorItem
            Log.e(Global.LOG_TAG, e.toString())
        }

        listView.adapter = ListViewAdapter(this, listItems)
    }

    override fun onStart() {
        super.onStart()
        if (reset){
            loadDevices()
            reset = false
        }
    }
}
