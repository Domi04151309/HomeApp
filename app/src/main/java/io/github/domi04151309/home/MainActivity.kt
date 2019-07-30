package io.github.domi04151309.home

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.github.domi04151309.home.simplehome.SimpleHomeAPI
import io.github.domi04151309.home.hue.HueAPI
import io.github.domi04151309.home.hue.HueLampActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import android.widget.TextView
import android.view.ViewGroup
import androidx.preference.PreferenceManager


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var devices: Devices? = null
    private var listView: ListView? = null
    private var currentView: View? = null
    private var currentDevice = ""
    private var hueRoom: String = ""
    private var hueRoomState: Boolean = false
    private var hueCurrentIcon: Drawable? = null
    private var level = "one"
    private var reset = false

    private val hueGroupStateListener = CompoundButton.OnCheckedChangeListener { compoundButton, b ->
        val row = compoundButton.parent as ViewGroup
        val hidden = row.findViewById<TextView>(R.id.hidden).text
        val room = hidden.substring(hidden.lastIndexOf("@") + 1)
        hueAPI!!.switchGroupByID(room, b)
    }

    private val hueLampStateListener = CompoundButton.OnCheckedChangeListener { compoundButton, b ->
        val row = compoundButton.parent as ViewGroup
        val hidden = row.findViewById<TextView>(R.id.hidden).text.toString()
        if (hidden.startsWith("room#")) {
            val room = hidden.substring(hidden.lastIndexOf("#") + 1)
            hueAPI!!.switchGroupByID(room, b)
        } else {
            hueAPI!!.switchLightByID(hidden, b)
        }
    }

    /*
     * Things related to the Home API
     */
    private val homeAPI = SimpleHomeAPI(this)

    private val homeRequestCallBack = object : SimpleHomeAPI.RequestCallBack {

        override fun onExecutionFinished(context: Context, result: CharSequence) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
        }

        override fun onCommandsLoaded(
                context: Context,
                errorMessage: String,
                device: String,
                titles: Array<String?>,
                summaries: Array<String?>,
                commandAddresses: Array<String?>
        ) {
            if (errorMessage == "") {
                val adapter = ListAdapter(context, titles, summaries, commandAddresses)
                listView!!.adapter = adapter
                setLevelTwo(currentView!!.findViewById<ImageView>(R.id.drawable).drawable, device)
            } else {
                setLevelOne()
                currentView!!.findViewById<TextView>(R.id.summary).text = errorMessage
            }
        }
    }


    /*
     * Things related to the Hue API
     */
    private var hueAPI: HueAPI? = null

    private val hueRequestCallBack = object : HueAPI.RequestCallBack {

        override fun onGroupsLoaded(
                context: Context,
                response: JSONObject?,
                device: String,
                errorMessage: String
        ) {
            if (errorMessage == "") {
                try {
                    val count = response!!.length()
                    val titles = arrayOfNulls<String>(count)
                    val summaries = arrayOfNulls<String>(count)
                    val drawables = IntArray(count)
                    val lightIDs = arrayOfNulls<String>(count)
                    val states = BooleanArray(count)
                    var currentObjectName: String
                    var currentObject: JSONObject?
                    for (i in 0 until count) {
                        try {
                            currentObjectName = response.names().getString(i)
                            currentObject = response.getJSONObject(currentObjectName)
                            titles[i] = currentObject.getString("name")
                            summaries[i] = resources.getString(R.string.hue_tap)
                            drawables[i] = R.drawable.ic_room
                            lightIDs[i] = currentObject.getJSONArray("lights").toString() + "@" + currentObjectName
                            states[i] = currentObject.getJSONObject("state").getBoolean("any_on")
                        } catch (e: JSONException) {
                            Log.e(Global.LOG_TAG, e.toString())
                        }
                    }
                    val adapter = ListAdapter(context, titles, summaries, lightIDs, drawables, states, hueGroupStateListener)
                    listView!!.adapter = adapter
                    hueCurrentIcon = resources.getDrawable(devices!!.getIconId(device), context.theme)
                    setLevelTwoHue(hueCurrentIcon!!, device)
                } catch (e: Exception) {
                    setLevelOne()
                    currentView!!.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.err_wrong_format_summary)
                    Log.e(Global.LOG_TAG, e.toString())
                }
            } else {
                setLevelOne()
                currentView!!.findViewById<TextView>(R.id.summary).text = errorMessage
            }
        }

        override fun onLightsLoaded(
                context: Context,
                response: JSONObject?,
                device: String,
                errorMessage: String
        ) {
            if (errorMessage == "") {
                try {
                    val count = response!!.length() + 1
                    val titles = arrayOfNulls<String>(count)
                    val summaries = arrayOfNulls<String>(count)
                    val drawables = IntArray(count)
                    val lightIDs = arrayOfNulls<String>(count)
                    val states = BooleanArray(count)
                    titles[0] = resources.getString(R.string.hue_whole_room)
                    summaries[0] = resources.getString(R.string.hue_whole_room_summary)
                    drawables[0] = R.drawable.ic_room
                    lightIDs[0] = "room#$hueRoom"
                    states[0] = hueRoomState
                    var currentObjectName: String
                    var currentObject: JSONObject?
                    for (i in 1 until count) {
                        try {
                            currentObjectName = response.names().getString(i - 1)
                            currentObject = response.getJSONObject(currentObjectName)
                            titles[i] = currentObject!!.getString("name")
                            summaries[i] = resources.getString(R.string.hue_tap)
                            drawables[i] = R.drawable.ic_device_lamp
                            lightIDs[i] = currentObjectName
                            states[i] = currentObject!!.getJSONObject("state").getBoolean("on")
                        } catch (e: JSONException) {
                            Log.e(Global.LOG_TAG, e.toString())
                        }
                    }
                    val adapter = ListAdapter(context, titles, summaries, lightIDs, drawables, states, hueLampStateListener)
                    listView!!.adapter = adapter
                    setLevelThreeHue(resources.getDrawable(R.drawable.ic_device_lamp, context.theme), currentView!!.findViewById<TextView>(R.id.title).text)
                } catch (e: Exception) {
                    setLevelOne()
                    currentView!!.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.err_wrong_format_summary)
                    Log.e(Global.LOG_TAG, e.toString())
                }
            } else {
                setLevelTwoHue(hueCurrentIcon!!, device)
                currentView!!.findViewById<TextView>(R.id.summary).text = errorMessage
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.setNoActionBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devices = Devices(PreferenceManager.getDefaultSharedPreferences(this))
        listView = findViewById<View>(R.id.listView) as ListView

        fab.setOnClickListener {
            reset = true
            startActivity(Intent(this, DevicesActivity::class.java))
        }

        findViewById<ImageView>(R.id.menu_icon).setOnClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
        }

        nav_view.setNavigationItemSelectedListener(this)
        nav_view.setCheckedItem(R.id.nav_devices)

        loadDevices()

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            currentView = view
            val title = view.findViewById<TextView>(R.id.title).text.toString()
            if (title == resources.getString(R.string.main_no_devices) || title == resources.getString(R.string.err_wrong_format))
                return@OnItemClickListener
            val hidden = view.findViewById<TextView>(R.id.hidden).text.toString()
            when (level) {
                "one" -> {
                    view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_connecting)
                    when (devices!!.getMode(title)) {
                        "Website" -> {
                            startActivity(
                                    Intent(this, WebActivity::class.java)
                                            .putExtra("URI", devices!!.getAddress(title))
                                            .putExtra("title", title)
                            )
                            reset = true
                        }
                        "SimpleHome API" ->
                            homeAPI.loadCommands (title, homeRequestCallBack)
                        "Hue Bridge" -> {
                            hueAPI = HueAPI(this, title)
                            hueAPI!!.loadGroups(hueRequestCallBack)
                        }
                        else ->
                            Toast.makeText(this, R.string.main_unknown_mode, Toast.LENGTH_LONG).show()
                    }
                }
                "two" ->
                    homeAPI.executeCommand(hidden, homeRequestCallBack)
                "two_hue" -> {
                    hueRoom = hidden.substring(hidden.lastIndexOf("@") + 1)
                    hueRoomState = view.findViewById<Switch>(R.id.state).isChecked
                    hueAPI!!.loadLightsByIDs(JSONArray(hidden.substring(0 , hidden.indexOf("@"))), hueRequestCallBack)
                }
                "three_hue" ->
                    startActivity(Intent(this, HueLampActivity::class.java).putExtra("ID", hidden).putExtra("Device", currentDevice))
            }
        }
    }

    private fun loadDevices(){
        var titles: Array<String?>
        var summaries: Array<String?>
        var drawables: IntArray
        try {
            if (devices!!.length() == 0) {
                titles = arrayOfNulls(1)
                summaries = arrayOfNulls(1)
                drawables = IntArray(1)
                titles[0] = resources.getString(R.string.main_no_devices)
                summaries[0] = resources.getString(R.string.main_no_devices_summary)
                drawables[0] = R.drawable.ic_info
            } else {
                val count = devices!!.length()
                titles = arrayOfNulls(count)
                summaries = arrayOfNulls(count)
                drawables = IntArray(count)
                for (i in 0 until count) {
                    try {
                        val name = devices!!.getName(i)
                        titles[i] = name
                        summaries[i] = resources.getString(R.string.main_tap_to_connect)
                        drawables[i] = devices!!.getIconId(name)
                    } catch (e: JSONException) {
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                }
            }
        } catch (e: Exception){
            titles = arrayOfNulls(1)
            summaries = arrayOfNulls(1)
            drawables = IntArray(1)
            titles[0] = resources.getString(R.string.err_wrong_format)
            summaries[0] = resources.getString(R.string.err_wrong_format_summary)
            drawables[0] = R.drawable.ic_warning
            Log.e(Global.LOG_TAG, e.toString())
        }
        Log.d(Global.LOG_TAG, titles.toString() + summaries.toString())

        val adapter = ListAdapter(this, titles, summaries, drawables)
        listView!!.adapter = adapter
        setLevelOne()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START))
            drawer_layout.closeDrawer(GravityCompat.START)
        else if (level == "two" || level == "two_hue")
            loadDevices()
        else if (level == "three_hue")
            hueAPI!!.loadGroups(hueRequestCallBack)
        else
            super.onBackPressed()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_devices -> {
                loadDevices()
                reset = false
            }
            R.id.nav_wiki -> {
                val uri = "https://github.com/Domi04151309/HomeApp/wiki"
                startActivity(Intent(this, WebActivity::class.java).putExtra("URI", uri).putExtra("title", resources.getString(R.string.nav_wiki)))
                reset = true
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, Preferences::class.java))
                reset = true
            }
            R.id.nav_source -> {
                val uri = "https://github.com/Domi04151309/HomeApp"
                startActivity(Intent(this, WebActivity::class.java).putExtra("URI", uri).putExtra("title", resources.getString(R.string.nav_source)))
                reset = true
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setLevelOne() {
        val theme = resources.newTheme()
        theme.applyStyle(R.style.Dark, false)
        deviceIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_home, theme))
        deviceName.text = resources.getString(R.string.main_device_name)
        fab.show()
        level = "one"
    }

    private fun setLevelTwo(icon: Drawable, title: CharSequence) {
        fab.hide()
        deviceIcon.setImageDrawable(icon)
        deviceName.text = title
        level = "two"
    }

    private fun setLevelTwoHue(icon: Drawable, title: CharSequence) {
        fab.hide()
        deviceIcon.setImageDrawable(icon)
        deviceName.text = title
        currentDevice = title.toString()
        level = "two_hue"
    }

    private fun setLevelThreeHue(icon: Drawable, title: CharSequence) {
        deviceIcon.setImageDrawable(icon)
        deviceName.text = title
        level = "three_hue"
    }

    override fun onResume() {
        super.onResume()
        if(reset) {
            nav_view.setCheckedItem(R.id.nav_devices)
            loadDevices()
            reset = false
        }
    }
}
