package io.github.domi04151309.home

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import android.view.View
import io.github.domi04151309.home.data.ListViewItem

class DevicesActivity : AppCompatActivity() {

    private var devices: Devices? = null
    private var listView: ListView? = null
    private var reset = true

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        devices = Devices(PreferenceManager.getDefaultSharedPreferences(this))
        listView = findViewById<View>(R.id.listView) as ListView

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            val action =  view.findViewById<TextView>(R.id.hidden).text
            val titleTxt = view.findViewById<TextView>(R.id.title).text.toString()

            if (action == "edit") {
                reset = true
                startActivity(Intent(this, EditDeviceActivity::class.java).putExtra("Device", titleTxt))
            } else if (action == "add") {
                reset = true
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.pref_add_method)
                builder.setItems(resources.getStringArray(R.array.pref_add_method_array)) { _, which ->
                    if (which == 0) {
                        startActivity(Intent(this, EditDeviceActivity::class.java))
                    } else if (which == 1) {
                        startActivity(Intent(this, SearchDevicesActivity::class.java))
                    }
                }
                builder.show()
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
                for (i in 0 until devices!!.length()) {
                    val name = devices!!.getName(i)
                    val deviceItem = ListViewItem(name)
                    deviceItem.summary = devices!!.getAddress(name)
                    deviceItem.hidden = "edit"
                    deviceItem.icon = devices!!.getIconId(name)
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
