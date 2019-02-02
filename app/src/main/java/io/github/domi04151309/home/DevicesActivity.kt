package io.github.domi04151309.home

import android.app.Dialog
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
import android.widget.ArrayAdapter
import android.widget.Spinner

class DevicesActivity : AppCompatActivity() {

    private var prefs: SharedPreferences? = null
    private var devices: Devices? = null
    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        devices = Devices(prefs!!)
        listView = findViewById<View>(R.id.listView) as ListView
        loadDevices()

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val actionTxt = view.findViewById(R.id.hidden) as TextView
            val action = actionTxt.text.toString()
            val titleTxt = view.findViewById(R.id.title) as TextView
            val title = titleTxt.text.toString()
            val summaryTxt = view.findViewById(R.id.summary) as TextView
            val summary = summaryTxt.text.toString()

            val builder = AlertDialog.Builder(this)

            var mName = ""
            var mAddress = ""

            val jsonDevices = JSONObject(prefs!!.getString("devices_json", Global.DEFAULT_JSON)).getJSONObject("devices")

            /*
             * Edit a device
             */
            if (action == "edit") {
                builder.setTitle(resources.getString(R.string.pref_edit_device))
                builder.setView(R.layout.dialog_edit)

                builder.setPositiveButton(resources.getString(android.R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
                    val dialog2 = Dialog::class.java.cast(dialog)
                    mName = dialog2.findViewById<EditText>(R.id.nameBox)!!.text.toString()
                    mAddress = dialog2.findViewById<EditText>(R.id.addressBox)!!.text.toString()
                    if(mName == "" || mAddress == "") {
                        Toast.makeText(this,resources.getString(R.string.pref_add_unsuccessful),Toast.LENGTH_LONG).show()
                        dialog.cancel()
                        return@OnClickListener
                    }
                    jsonDevices.remove(title)
                    addDevice(
                            jsonDevices,
                            mName,
                            mAddress,
                            dialog2.findViewById<Spinner>(R.id.iconSpinner)!!.selectedItem.toString(),
                            dialog2.findViewById<Spinner>(R.id.modeSpinner)!!.selectedItem.toString()
                    )
                    loadDevices()
                })
                builder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, _ -> dialog.cancel() })

                val dialog = builder.show()

                dialog.findViewById<EditText>(R.id.nameBox)!!.setText(title)
                dialog.findViewById<EditText>(R.id.addressBox)!!.setText(summary)
                val iconSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_icons))
                dialog.findViewById<Spinner>(R.id.iconSpinner)!!.setSelection(iconSpinnerArrayAdapter.getPosition(devices!!.getIcon(title)))
                val modeSpinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_add_mode_array))
                dialog.findViewById<Spinner>(R.id.modeSpinner)!!.setSelection(modeSpinnerArrayAdapter.getPosition(devices!!.getMode(title)))

                dialog.findViewById<Button>(R.id.deleteBtn)!!.setOnClickListener {
                    val secondBuilder = AlertDialog.Builder(this)
                    secondBuilder.setTitle(resources.getString(R.string.pref_delete_device))
                    secondBuilder.setMessage(resources.getString(R.string.pref_delete_device_question))
                    secondBuilder.setPositiveButton(resources.getString(android.R.string.ok), { _, _ ->
                        jsonDevices.remove(title)
                        prefs!!.edit().putString("devices_json", JSONObject().put("devices", jsonDevices).toString()).apply()
                        dialog.dismiss()
                        loadDevices()
                    })
                    secondBuilder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, _ -> dialog.cancel() })
                    secondBuilder.show()
                }
            }

            /*
             * Add a device
             */
            else if (action == "add") {
                builder.setTitle(resources.getString(R.string.pref_add))
                builder.setView(R.layout.dialog_add)

                builder.setPositiveButton(resources.getString(android.R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
                    val dialog2 = Dialog::class.java.cast(dialog)
                    mName = dialog2.findViewById<EditText>(R.id.nameBox)!!.text.toString()
                    mAddress = dialog2.findViewById<EditText>(R.id.addressBox)!!.text.toString()
                    if(mName == "" || mAddress == "") {
                        Toast.makeText(this,resources.getString(R.string.pref_add_unsuccessful),Toast.LENGTH_LONG).show()
                        dialog.cancel()
                        return@OnClickListener
                    }
                    addDevice(
                            jsonDevices,
                            mName,
                            mAddress,
                            dialog2.findViewById<Spinner>(R.id.iconSpinner)!!.selectedItem.toString(),
                            dialog2.findViewById<Spinner>(R.id.modeSpinner)!!.selectedItem.toString()
                    )
                    loadDevices()
                })
                builder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, _ -> dialog.cancel() })
                builder.show()
            }
        }
    }

    private fun addDevice(jsonDevices: JSONObject, name: String, address: String, icon: String, mode: String) {
        val deviceObject = JSONObject().put("address", address).put("icon", icon).put("mode", mode)
        jsonDevices.put(name, deviceObject)
        prefs!!.edit().putString("devices_json", JSONObject().put("devices", jsonDevices).toString()).apply()
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
                        summaries[i] = Global.formatURL(devices!!.getAddress(name))
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
            actions[i] = "nonu"
            drawables[i] = R.drawable.ic_warning
            Log.e(Global.LOG_TAG, e.toString())
        }
        Log.d(Global.LOG_TAG, Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, actions, drawables)
        listView!!.adapter = adapter
    }
}
