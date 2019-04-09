package io.github.domi04151309.home

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import org.json.JSONException
import java.util.*

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

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val actionTxt = view.findViewById(R.id.hidden) as TextView
            val action = actionTxt.text.toString()
            val titleTxt = view.findViewById(R.id.title) as TextView

            if (action == "edit") {
                reset = true
                startActivity(Intent(this, EditDeviceActivity::class.java).putExtra("Device", titleTxt.text.toString()))
            } else if (action == "add") {
                reset = true
                startActivity(Intent(this, EditDeviceActivity::class.java))
            }
        }
    }

    private fun loadDevices(){
        var titles: Array<String?>?
        var summaries: Array<String?>?
        var actions: Array<String?>?
        var drawables: IntArray?
        var i = 0
        try {
            if (devices!!.length() == 0) {
                titles = arrayOfNulls(2)
                summaries = arrayOfNulls(2)
                actions = arrayOfNulls(2)
                drawables = IntArray(2)
                titles[i] = resources.getString(R.string.main_no_devices)
                summaries[i] = ""
                actions[i] = "none"
                i++
            } else {
                val count = devices!!.length()
                titles = arrayOfNulls(count + 1)
                summaries = arrayOfNulls(count + 1)
                actions = arrayOfNulls(count + 1)
                drawables = IntArray(count + 1)
                while (i < count) {
                    try {
                        val name = devices!!.getName(i)
                        titles[i] = name
                        summaries[i] = devices!!.getAddress(name)
                        actions[i] = "edit"
                        drawables[i] = Global.getIconId(devices!!.getIcon(name))
                    } catch (e: JSONException) {
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                    i++
                }
            }
            titles[i] = resources.getString(R.string.pref_add)
            summaries[i] = resources.getString(R.string.pref_add_summary)
            actions[i] = "add"
            drawables[i] = R.drawable.ic_add
        } catch (e: Exception) {
            titles = arrayOfNulls(1)
            summaries = arrayOfNulls(1)
            actions = arrayOfNulls(1)
            drawables = IntArray(1)
            titles[i] = resources.getString(R.string.err_wrong_format)
            summaries[i] = resources.getString(R.string.err_wrong_format_summary)
            actions[i] = "none"
            drawables[i] = R.drawable.ic_warning
            Log.e(Global.LOG_TAG, e.toString())
        }
        Log.d(Global.LOG_TAG, Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, actions, drawables)
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
