package io.github.domi04151309.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.github.domi04151309.home.simplehome.SimpleHomeAPI
import io.github.domi04151309.home.hue.HueAPI
import io.github.domi04151309.home.hue.HueLampActivity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import android.widget.TextView
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.RequestCallbackObject
import io.github.domi04151309.home.helpers.Commands
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.ListViewAdapter
import io.github.domi04151309.home.helpers.UpdateHandler
import io.github.domi04151309.home.objects.Global
import io.github.domi04151309.home.objects.Theme
import io.github.domi04151309.home.tasmota.Tasmota

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var devices: Devices? = null
    private var listView: ListView? = null
    private var deviceIcon: ImageView? = null
    private var deviceName: TextView? = null
    private var fab: FloatingActionButton? = null
    private var drawerLayout: DrawerLayout? = null
    private var navView: NavigationView? = null
    private var currentView: View? = null
    private var currentDevice = ""
    private var hueRoom: String = ""
    private var hueRoomState: Boolean = false
    private var hueCurrentIcon: Int = 0
    private var tasmotaPosition: Int = 0
    private var level = "one"
    private var reset = false
    private val updateHandler = UpdateHandler()
    private var canReceiveRequest = false

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

        override fun onCommandsLoaded(holder: RequestCallbackObject) {
            if (holder.response != null) {
                val commandsObject = Commands(holder.response.getJSONObject("commands"))
                val listItems: ArrayList<ListViewItem> = arrayListOf()
                for (i in 0 until commandsObject.length()) {
                    try {
                        commandsObject.selectCommand(i)
                        val commandItem = ListViewItem(commandsObject.getSelectedTitle())
                        commandItem.summary = commandsObject.getSelectedSummary()
                        commandItem.hidden = devices!!.getDeviceById(holder.deviceId).address + commandsObject.getSelected()
                        commandItem.icon = R.drawable.ic_do
                        listItems += commandItem
                    } catch (e: JSONException) {
                        Log.e(Global.LOG_TAG, e.toString())
                    }
                }
                updateList(listItems)
                val device = devices!!.getDeviceById(holder.deviceId)
                setLevelTwo(device.iconId, device.name)
            } else {
                handleErrorOnLevelOne(holder.errorMessage)
            }
        }
    }

    /*
     * Things related to the Hue API
     */
    private var hueAPI: HueAPI? = null

    private val hueRequestCallBack = object : HueAPI.RequestCallBack {

        override fun onGroupsLoaded(holder: RequestCallbackObject) {
            if (holder.response != null) {
                try {
                    var currentObjectName: String
                    var currentObject: JSONObject
                    val listItems: ArrayList<ListViewItem> = arrayListOf()
                    for (i in 0 until holder.response.length()) {
                        try {
                            currentObjectName = holder.response.names()!!.getString(i)
                            currentObject = holder.response.getJSONObject(currentObjectName)
                            val listItem = ListViewItem(currentObject.getString("name"))
                            listItem.summary = resources.getString(R.string.hue_tap)
                            listItem.hidden = currentObject.getJSONArray("lights").toString() + "@" + currentObjectName
                            listItem.icon = R.drawable.ic_room
                            listItem.state = currentObject.getJSONObject("action").getBoolean("on")
                            listItem.stateListener = hueGroupStateListener
                            listItems += listItem
                        } catch (e: JSONException) {
                            Log.e(Global.LOG_TAG, e.toString())
                        }
                    }
                    updateList(listItems)
                    val device = devices!!.getDeviceById(holder.deviceId)
                    hueCurrentIcon = device.iconId
                    setLevelTwoHue(holder.deviceId, device.iconId, device.name)
                } catch (e: Exception) {
                    handleErrorOnLevelOne(resources.getString(R.string.err_wrong_format_summary))
                    Log.e(Global.LOG_TAG, e.toString())
                }
            } else {
                handleErrorOnLevelOne(holder.errorMessage)
            }
        }

        override fun onLightsLoaded(holder: RequestCallbackObject) {
            if (holder.response != null) {
                try {
                    val roomItem = ListViewItem(resources.getString(R.string.hue_whole_room))
                    roomItem.summary = resources.getString(R.string.hue_whole_room_summary)
                    roomItem.hidden = "room#$hueRoom"
                    roomItem.icon = R.drawable.ic_room
                    roomItem.state = hueRoomState
                    roomItem.stateListener = hueLampStateListener
                    var currentObjectName: String
                    var currentObject: JSONObject
                    val listItems: ArrayList<ListViewItem> = arrayListOf(roomItem)
                    for (i in 1 until (holder.response.length() + 1)) {
                        try {
                            currentObjectName = holder.response.names()!!.getString(i - 1)
                            currentObject = holder.response.getJSONObject(currentObjectName)
                            val listItem = ListViewItem(currentObject.getString("name"))
                            listItem.summary = resources.getString(R.string.hue_tap)
                            listItem.hidden = currentObjectName
                            listItem.icon = R.drawable.ic_device_lamp
                            listItem.state = currentObject.getJSONObject("state").getBoolean("on")
                            listItem.stateListener = hueLampStateListener
                            listItems += listItem
                        } catch (e: JSONException) {
                            Log.e(Global.LOG_TAG, e.toString())
                        }
                    }
                    updateList(listItems)
                    setLevelThreeHue(currentView!!.findViewById<TextView>(R.id.title).text)
                } catch (e: Exception) {
                    setLevelTwo(hueCurrentIcon, devices!!.getDeviceById(holder.deviceId).name)
                    currentView!!.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.err_wrong_format_summary)
                    Log.e(Global.LOG_TAG, e.toString())
                }
            } else {
                setLevelTwoHue(holder.deviceId, hueCurrentIcon, devices!!.getDeviceById(holder.deviceId).name)
                currentView!!.findViewById<TextView>(R.id.summary).text = holder.errorMessage
            }
        }
    }
    private val hueRequestUpdaterCallBack = object : HueAPI.RequestCallBack {

        override fun onGroupsLoaded(holder: RequestCallbackObject) {
            if (holder.response != null) {
                try {
                    for (i in 0 until holder.response.length()) {
                        listView!!.getChildAt(i).findViewById<Switch>(R.id.state).isChecked = holder.response
                                .getJSONObject(holder.response.names()!!.getString(i))
                                .getJSONObject("action")
                                .getBoolean("on")
                    }
                } catch (e: Exception) {
                    Log.e(Global.LOG_TAG, e.toString())
                }
            }
        }

        override fun onLightsLoaded(holder: RequestCallbackObject) {
            if (holder.response != null) {
                try {
                    for (i in 1 until (holder.response.length() + 1)) {
                        listView!!.getChildAt(i).findViewById<Switch>(R.id.state).isChecked = holder.response
                                .getJSONObject(holder.response.names()!!.getString(i - 1))
                                .getJSONObject("state")
                                .getBoolean("on")
                    }
                } catch (e: Exception) {
                    Log.e(Global.LOG_TAG, e.toString())
                }
            }
        }
    }

    /*
     * Things related to Tasmota
     */
    private var tasmota: Tasmota? = null
    private val tasmotaRequestCallBack = object : Tasmota.RequestCallBack {

        override fun onItemsChanged(context: Context) {
            listView!!.adapter = ListViewAdapter(context, tasmota!!.loadList(), false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.setNoActionBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devices = Devices(this)
        listView = findViewById<View>(R.id.listView) as ListView
        deviceIcon = findViewById(R.id.deviceIcon)
        deviceName = findViewById(R.id.deviceName)
        fab = findViewById(R.id.fab)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        fab!!.setOnClickListener {
            reset = true
            startActivity(Intent(this, DevicesActivity::class.java))
        }

        findViewById<ImageView>(R.id.menu_icon).setOnClickListener {
            drawerLayout!!.openDrawer(GravityCompat.START)
        }

        navView!!.setNavigationItemSelectedListener(this)
        navView!!.setCheckedItem(R.id.nav_devices)

        listView!!.onItemClickListener = AdapterView.OnItemClickListener { _, view, _, _ ->
            currentView = view
            val title = view.findViewById<TextView>(R.id.title).text.toString()
            if (title == resources.getString(R.string.main_no_devices) || title == resources.getString(R.string.err_wrong_format))
                return@OnItemClickListener
            val hidden = view.findViewById<TextView>(R.id.hidden).text.toString()
            when (level) {
                "one" -> {
                    view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_connecting)
                    handleLevelOne(hidden)
                }
                "two" ->
                    homeAPI.executeCommand(hidden, homeRequestCallBack)
                "two_hue" -> {
                    hueRoom = hidden.substring(hidden.lastIndexOf("@") + 1)
                    hueRoomState = view.findViewById<Switch>(R.id.state).isChecked
                    val localIds = JSONArray(hidden.substring(0 , hidden.indexOf("@")))
                    hueAPI!!.loadLightsByIDs(localIds, hueRequestCallBack)
                    updateHandler.setUpdateFunction {
                        if (canReceiveRequest && hueAPI?.readyForRequest == true) {
                            hueAPI!!.loadLightsByIDs(localIds, hueRequestUpdaterCallBack)
                        }
                    }
                }
                "three_hue" ->
                    startActivity(Intent(this, HueLampActivity::class.java).putExtra("ID", hidden).putExtra("Device", currentDevice))
                "two_tasmota" -> {
                    when (hidden) {
                        "add" -> tasmota!!.addToList(tasmotaRequestCallBack)
                        "execute_once" -> tasmota!!.executeOnce()
                        else -> tasmota!!.execute(view.findViewById<TextView>(R.id.summary).text.toString())
                    }
                }
            }
        }

        registerForContextMenu(listView)

        //Handle shortcut
        if(intent.hasExtra("device")) {
            val deviceId = intent.getStringExtra("device") ?: ""
            if (devices!!.idExists(deviceId)) {
                val device = devices!!.getDeviceById(deviceId)
                deviceIcon!!.setImageResource(device.iconId)
                deviceName!!.text = device.name
                handleLevelOne(deviceId)
            } else {
                loadDevices()
                Toast.makeText(this, R.string.main_device_nonexistent, Toast.LENGTH_LONG).show()
            }
        } else {
            loadDevices()
        }
    }

    private fun handleLevelOne(deviceId: String) {
        val deviceObj = devices!!.getDeviceById(deviceId)
        when (deviceObj.mode) {
            "Website" -> {
                startActivity(
                        Intent(this, WebActivity::class.java)
                                .putExtra("URI", deviceObj.address)
                                .putExtra("title", deviceObj.name)
                )
                reset = true
            }
            "SimpleHome API" ->
                homeAPI.loadCommands(deviceId, homeRequestCallBack)
            "Hue API" -> {
                hueAPI = HueAPI(this, deviceId)
                loadHueGroups()
            }
            "Tasmota" -> {
                tasmota = Tasmota(this, deviceId)
                updateList(tasmota!!.loadList())
                setLevelTwoTasmota(deviceObj.iconId, deviceObj.name)
            }
            else ->
                Toast.makeText(this, R.string.main_unknown_mode, Toast.LENGTH_LONG).show()
        }
    }

    private fun handleErrorOnLevelOne(err: String) {
        if (currentView == null) {
            loadDevices()
            Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        } else {
            setLevelOne()
            currentView!!.findViewById<TextView>(R.id.summary).text = err
        }
    }

    private fun loadDevices() {
        updateHandler.stop()
        val listItems: ArrayList<ListViewItem> = arrayListOf()
        try {
            if (devices!!.length() == 0) {
                val emptyItem = ListViewItem(resources.getString(R.string.main_no_devices))
                emptyItem.summary = resources.getString(R.string.main_no_devices_summary)
                emptyItem.icon = R.drawable.ic_info
                listItems += emptyItem
            } else {
                var currentDevice: DeviceItem
                for (i in 0 until devices!!.length()) {
                    currentDevice = devices!!.getDeviceByIndex(i)
                    val deviceItem = ListViewItem(currentDevice.name)
                    deviceItem.summary = resources.getString(R.string.main_tap_to_connect)
                    deviceItem.hidden = currentDevice.id
                    deviceItem.icon = currentDevice.iconId
                    listItems += deviceItem
                }
            }
        } catch (e: Exception) {
            val errorItem = ListViewItem(resources.getString(R.string.err_wrong_format))
            errorItem.summary = resources.getString(R.string.err_wrong_format_summary)
            errorItem.hidden = "none"
            errorItem.icon = R.drawable.ic_warning
            listItems += errorItem
            Log.e(Global.LOG_TAG, e.toString())
        }

        updateList(listItems)
        setLevelOne()
    }

    private fun loadHueGroups() {
        hueAPI!!.loadGroups(hueRequestCallBack)
        updateHandler.setUpdateFunction {
            if (canReceiveRequest && hueAPI?.readyForRequest == true) {
                hueAPI!!.loadGroups(hueRequestUpdaterCallBack)
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout!!.isDrawerOpen(GravityCompat.START))
            drawerLayout!!.closeDrawer(GravityCompat.START)
        else if (level == "two" || level == "two_hue" || level == "two_tasmota")
            loadDevices()
        else if (level == "three_hue")
            loadHueGroups()
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
        drawerLayout!!.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        tasmotaPosition = (menuInfo as AdapterView.AdapterContextMenuInfo).position
        val hidden = listView!!.getChildAt(tasmotaPosition).findViewById<TextView>(R.id.hidden).text
        if (hidden == "tasmota_command") {
            menuInflater.inflate(R.menu.activity_main_tasmota_context, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.title) {
            resources.getString(R.string.str_edit) -> {
                val editing = listView!!.getChildAt(tasmotaPosition)
                tasmota!!.removeFromList(tasmotaRequestCallBack, tasmotaPosition)
                tasmota!!.addToList(
                        tasmotaRequestCallBack,
                        editing.findViewById<TextView>(R.id.title).text.toString(),
                        editing.findViewById<TextView>(R.id.summary).text.toString()
                )
            }
            resources.getString(R.string.str_delete) -> {
                AlertDialog.Builder(this)
                        .setTitle(R.string.str_delete)
                        .setMessage(R.string.tasmota_delete_command)
                        .setPositiveButton(R.string.str_delete) { _, _ ->
                            tasmota!!.removeFromList(tasmotaRequestCallBack, tasmotaPosition)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .show()
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun setLevelOne() {
        val theme = resources.newTheme()
        theme.applyStyle(R.style.TintHouse, false)
        deviceIcon!!.setImageDrawable(resources.getDrawable(R.drawable.ic_home, theme))
        deviceName!!.text = resources.getString(R.string.main_device_name)
        fab!!.show()
        level = "one"
    }

    private fun setLevelTwo(icon: Int, title: CharSequence) {
        fab!!.hide()
        deviceIcon!!.setImageResource(icon)
        deviceName!!.text = title
        level = "two"
    }

    private fun setLevelTwoHue(deviceId: String, icon: Int, title: CharSequence) {
        fab!!.hide()
        deviceIcon!!.setImageResource(icon)
        deviceName!!.text = title
        currentDevice = deviceId
        level = "two_hue"
    }

    private fun setLevelTwoTasmota(icon: Int, title: CharSequence) {
        fab!!.hide()
        deviceIcon!!.setImageResource(icon)
        deviceName!!.text = title
        level = "two_tasmota"
    }

    private fun setLevelThreeHue(title: CharSequence) {
        deviceIcon!!.setImageResource(R.drawable.ic_device_lamp)
        deviceName!!.text = title
        level = "three_hue"
    }

    private fun updateList(items: ArrayList<ListViewItem>) {
        listView!!.adapter = ListViewAdapter(this, items)
    }

    override fun onStart() {
        super.onStart()
        canReceiveRequest = true
        if(reset) {
            navView!!.setCheckedItem(R.id.nav_devices)
            loadDevices()
            reset = false
        }
    }

    override fun onStop() {
        super.onStop()
        canReceiveRequest = false
        if(reset) {
            navView!!.setCheckedItem(R.id.nav_devices)
            loadDevices()
            reset = false
        }
    }
}
