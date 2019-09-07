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

class DevicesActivity : AppCompatActivity() {

    private var devices: Devices? = null
    private var listView: ListView? = null
    private var reset = true

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        devices = Devices(this)
        listView = findViewById<View>(R.id.listView) as ListView

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { _, view, pos, _ ->
            val action =  view.findViewById<TextView>(R.id.hidden).text
            if (action == "edit") {
                reset = true
                val deviceId = devices!!.getDeviceByIndex(pos).id
                startActivity(Intent(this, EditDeviceActivity::class.java).putExtra("deviceId", deviceId))
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
        var listItems: Array<ListViewItem> = arrayOf()
        try {
            if (devices!!.length() == 0) {
                val emptyItem = ListViewItem(resources.getString(R.string.main_no_devices))
                emptyItem.hidden = "none"
                listItems += emptyItem
            } else {
                var currentDevice: DeviceItem
                for (i in 0 until devices!!.length()) { 
                    currentDevice = devices!!.getDeviceByIndex(i)
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

        val adapter = ListViewAdapter(this, listItems)
        listView!!.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        if (reset){
            loadDevices()
            reset = false
        }
    }
}
