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
import android.widget.ArrayAdapter
import android.widget.Spinner

class DevicesActivity : AppCompatActivity() {

    private var prefs: SharedPreferences? = null
    private var devices: Devices? = null
    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            val attributes = obtainStyledAttributes(intArrayOf(R.attr.dialogPreferredPadding))
            val dimension = attributes.getDimensionPixelSize(0, 0)
            val halfDimension = dimension/2
            attributes.recycle()
            layout.setPaddingRelative(dimension, 0, dimension, 0)

            val nameTxt = TextView(this)
            nameTxt.text = resources.getString(R.string.pref_add_name)
            nameTxt.setPaddingRelative(0,halfDimension,0,0)

            val nameBox = EditText(this)
            nameBox.hint = resources.getString(R.string.pref_add_name_example)
            nameBox.setSingleLine()

            val addressTxt = TextView(this)
            addressTxt.text = resources.getString(R.string.pref_add_address)
            addressTxt.setPaddingRelative(0,halfDimension,0,0)

            val addressBox = EditText(this)
            addressBox.hint = resources.getString(R.string.pref_add_address_example)
            addressBox.setSingleLine()

            val iconTxt = TextView(this)
            iconTxt.text = resources.getString(R.string.pref_add_icon)
            iconTxt.setPaddingRelative(0,halfDimension,0,0)

            val spinner = Spinner(this)
            val spinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, resources.getStringArray(R.array.pref_icons))
            spinner.adapter = spinnerArrayAdapter

            layout.addView(nameTxt)
            layout.addView(nameBox)
            layout.addView(addressTxt)
            layout.addView(addressBox)
            layout.addView(iconTxt)
            layout.addView(spinner)

            var mName = ""
            var mAddress = ""

            val jsonDevices = JSONObject(prefs!!.getString("devices_json", Global.DEFAULT_JSON)).getJSONObject("devices")
            if (action == "edit") {
                builder.setTitle(resources.getString(R.string.pref_edit_device))

                nameBox.setText(title)
                addressBox.setText(summary)
                spinner.setSelection(spinnerArrayAdapter.getPosition(devices!!.getIcon(title)))

                val deleteBtn = Button(this)
                deleteBtn.text = resources.getString(R.string.pref_delete_device)
                deleteBtn.isAllCaps = false
                deleteBtn.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                deleteBtn.setCompoundDrawablesWithIntrinsicBounds( R.drawable.ic_delete, 0, 0, 0)
                deleteBtn.background = resources.getDrawable(android.R.color.transparent)
                iconTxt.setPaddingRelative(0,halfDimension,0,0)
                layout.addView(deleteBtn)

                builder.setView(layout)
                builder.setPositiveButton(resources.getString(android.R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
                    mName = nameBox.text.toString()
                    mAddress = addressBox.text.toString()
                    if(mName == "" || mAddress == "") {
                        Toast.makeText(this,resources.getString(R.string.pref_add_unsuccessful),Toast.LENGTH_LONG).show()
                        dialog.cancel()
                        return@OnClickListener
                    }
                    jsonDevices.remove(title)
                    addDevice(jsonDevices, mName, mAddress, spinner.selectedItem.toString())
                    loadDevices()
                })
                builder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, _ -> dialog.cancel() })
                val dialog = builder.show()

                deleteBtn.setOnClickListener {
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
            } else if (action == "add") {
                builder.setTitle(resources.getString(R.string.pref_add))
                builder.setView(layout)

                builder.setPositiveButton(resources.getString(android.R.string.ok), DialogInterface.OnClickListener { dialog, _ ->
                    mName = nameBox.text.toString()
                    mAddress = addressBox.text.toString()
                    if(mName == "" || mAddress == "") {
                        Toast.makeText(this,resources.getString(R.string.pref_add_unsuccessful),Toast.LENGTH_LONG).show()
                        dialog.cancel()
                        return@OnClickListener
                    }
                    addDevice(jsonDevices, mName, mAddress, spinner.selectedItem.toString())
                    loadDevices()
                })
                builder.setNegativeButton(resources.getString(android.R.string.cancel), { dialog, _ -> dialog.cancel() })
                builder.show()
            }
        }
    }

    private fun addDevice(jsonDevices: JSONObject, name: String, address: String, icon: String) {
        val deviceObject = JSONObject().put("address", address).put("icon", icon)
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
            actions[i] = "null"
            drawables[i] = R.drawable.ic_warning
            Log.e(Global.LOG_TAG, e.toString())
        }
        Log.d(Global.LOG_TAG, Arrays.toString(titles) + Arrays.toString(summaries))

        val adapter = ListAdapter(this, titles, summaries, actions, drawables)
        listView!!.adapter = adapter
    }
}
