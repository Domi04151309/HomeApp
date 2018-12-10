package io.github.domi04151309.home

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class DevicesActivity : AppCompatActivity() {

    private var prefs: SharedPreferences? = null
    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        listView = findViewById<View>(R.id.listView) as ListView
        loadDevices()

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val actionTxt = view.findViewById(R.id.hidden) as TextView
            val action = actionTxt.text.toString()
            val titleTxt = view.findViewById(R.id.title) as TextView
            val title = titleTxt.text.toString()
            val builder = AlertDialog.Builder(this)
            val layout = LinearLayout(this)
            layout.setPaddingRelative(80, 0, 80, 0)
            if (action == "edit") {
                builder.setTitle(resources.getString(R.string.pref_edit_device))
                layout.orientation = LinearLayout.HORIZONTAL

                val deleteBtn = Button(this)
                deleteBtn.text = resources.getString(R.string.pref_delete_device)
                deleteBtn.isAllCaps = false
                deleteBtn.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_delete, 0, 0, 0)
                deleteBtn.background = resources.getDrawable(android.R.color.transparent)
                layout.addView(deleteBtn)

                builder.setView(layout)
                builder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, which -> dialog.cancel() })
                val dialog = builder.show()

                deleteBtn.setOnClickListener {
                    val jsonString = prefs!!.getString("devices_json", "{\"devices\":{}}")
                    val jsonDevices = JSONObject(jsonString).getJSONObject("devices")
                    jsonDevices.remove(title)
                    val json = JSONObject().put("devices", jsonDevices)
                    prefs!!.edit().putString("devices_json", json.toString()).apply()
                    dialog.dismiss()
                    loadDevices()
                }
            } else if (action == "add") {
                var mName = ""
                var mIP = ""

                builder.setTitle(resources.getString(R.string.pref_add))
                layout.orientation = LinearLayout.VERTICAL

                val nameBox = EditText(this)
                nameBox.hint = resources.getString(R.string.pref_add_name)
                nameBox.setSingleLine()
                layout.addView(nameBox)

                val ipBox = EditText(this)
                ipBox.hint = resources.getString(R.string.pref_add_ip)
                ipBox.setSingleLine()
                layout.addView(ipBox)

                builder.setView(layout)

                builder.setPositiveButton(resources.getString(android.R.string.ok), DialogInterface.OnClickListener { dialog, which ->
                    mName = nameBox.text.toString()
                    mIP = ipBox.text.toString()
                    if(mName == "" || mIP == "") {
                        Toast.makeText(this,resources.getString(R.string.pref_add_unsuccessful),Toast.LENGTH_LONG).show()
                        dialog.cancel()
                        return@OnClickListener
                    }
                    val jsonString = prefs!!.getString("devices_json", "{\"devices\":{}}")
                    val jsonDevices = JSONObject(jsonString).getJSONObject("devices").put(mName, mIP)
                    val json = JSONObject().put("devices", jsonDevices)
                    prefs!!.edit().putString("devices_json", json.toString()).apply()
                    loadDevices()
                })
                builder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, which -> dialog.cancel() })
                builder.show()
            }
        }
    }

    private fun loadDevices(){
        val jsonString = prefs!!.getString("devices_json", "{\"devices\":{}}")
        val jsonDevices = JSONObject(jsonString).getJSONObject("devices")
        val titles: Array<String?>?
        val summaries: Array<String?>?
        val actions: Array<String?>?
        var i = 0
        if (jsonDevices.length() == 0) {
            titles = arrayOfNulls(2)
            summaries = arrayOfNulls(2)
            actions = arrayOfNulls(2)
            titles[i] = resources.getString(R.string.main_no_devices)
            summaries[i] = ""
            actions[i] = "none"
            i++
        } else {
            val deviceList = jsonDevices.names()
            titles = arrayOfNulls(deviceList.length() + 1)
            summaries = arrayOfNulls(deviceList.length() + 1)
            actions = arrayOfNulls(deviceList.length() + 1)
            val count = deviceList.length()
            while (i < count) {
                try {
                    val mJsonString = deviceList.getString(i)
                    titles[i] = mJsonString.toString()
                    summaries[i] = formatURL(jsonDevices.getString(mJsonString))
                    actions[i] = "edit"
                } catch (e: JSONException) {
                    Log.e("Home", e.toString())
                }
                i++
            }
        }
        titles[i] = resources.getString(R.string.pref_add)
        summaries[i] = resources.getString(R.string.pref_add_summary)
        actions[i] = "add"
        Log.d("Home", Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, actions)
        listView!!.adapter = adapter
    }

    private fun formatURL(url: String): String {
        var _url = url
        if (!(_url.startsWith("https://") || _url.startsWith("http://")))
            _url = "http://$_url"
        if (!_url.endsWith("/"))
            _url += "/"
        return _url
    }
}
