package io.github.domi04151309.home.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.github.domi04151309.home.helpers.SimpleHomeAPI
import io.github.domi04151309.home.helpers.HueAPI
import org.json.JSONException
import org.json.JSONObject
import android.widget.TextView
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.RequestCallbackObject
import io.github.domi04151309.home.helpers.*
import io.github.domi04151309.home.helpers.P
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme
import io.github.domi04151309.home.helpers.Tasmota
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.adapters.MainListAdapter
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface

class MainActivity : AppCompatActivity(), RecyclerViewHelperInterface {

    enum class Flavors {
        ONE, TWO_SIMPLE_HOME, TWO_HUE, TWO_TASMOTA, TWO_SHELLY, TWO_ESPEASY
    }

    private var currentDevice = ""
    private var tasmotaPosition: Int = 0
    private var level = Flavors.ONE
    private var reset : Boolean = false
    private val updateHandler = UpdateHandler()
    private var canReceiveRequest = false
    private var currentView: View? = null
    internal lateinit var devices: Devices
    internal lateinit var recyclerView: RecyclerView
    internal lateinit var adapter: MainListAdapter
    private lateinit var deviceIcon: ImageView
    private lateinit var deviceName: TextView
    private lateinit var fab: FloatingActionButton

    private var themeId = ""
    private fun getThemeId(): String =
            PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(P.PREF_THEME, P.PREF_THEME_DEFAULT) ?: P.PREF_THEME_DEFAULT

    /*
     * Things related to ESP Easy
     */
    private var espEasy: EspEasyAPI? = null
    internal val espEasyStateListener = CompoundButton.OnCheckedChangeListener { compoundButton, newState ->
        if (compoundButton.isPressed) {
            val view = (compoundButton.parent as ViewGroup)
            val gpioId = view.findViewById<TextView>(R.id.hidden).text.toString()
            if (gpioId.isEmpty()) {
                return@OnCheckedChangeListener
            }

            view.findViewById<TextView>(R.id.summary).text = resources.getString(
                if (newState) R.string.shelly_switch_summary_on
                else R.string.shelly_switch_summary_off
            )
            espEasy?.changeSwitchState(gpioId.toInt(), newState)
        }
    }
    private val espEasyRequestCallBack = object : EspEasyAPI.RequestCallBack {
        override fun onInfoLoaded(holder: RequestCallbackObject<ArrayList<ListViewItem>>) {
            if (holder.response != null) {
                adapter.updateData(holder.response, espEasyStateListener)
                setLevelTwo(devices.getDeviceById(holder.deviceId), Flavors.TWO_ESPEASY)
            } else {
                handleErrorOnLevelOne(holder.errorMessage)
            }
        }
    }

    /*
     * Things related to the Home API
     */
    private val homeAPI = SimpleHomeAPI(this)
    private val homeRequestCallBack = object : SimpleHomeAPI.RequestCallBack {

        override fun onExecutionFinished(context: Context, result: CharSequence, refresh: Boolean, deviceId: String) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
            if (refresh) homeAPI.loadCommands(deviceId, this)
        }

        override fun onCommandsLoaded(holder: RequestCallbackObject<ArrayList<ListViewItem>>) {
            if (holder.response != null) {
                adapter.updateData(holder.response)
                setLevelTwo(devices.getDeviceById(holder.deviceId), Flavors.TWO_SIMPLE_HOME)
            } else {
                handleErrorOnLevelOne(holder.errorMessage)
            }
        }
    }

    /*
     * Things related to the Hue API
     */
    private var hueAPI: HueAPI? = null
    internal val hueGroupStateListener = CompoundButton.OnCheckedChangeListener { compoundButton, b ->
        if (compoundButton.isPressed) {
            val hidden = (compoundButton.parent as ViewGroup).findViewById<TextView>(R.id.hidden).text
            hueAPI?.switchGroupByID(hidden.substring(hidden.lastIndexOf("#") + 1), b)
        }
    }
    private val hueRequestCallBack = object : HueAPI.RequestCallBack {
        override fun onLightsLoaded(holder: RequestCallbackObject<JSONObject>) {}
        override fun onGroupsLoaded(holder: RequestCallbackObject<JSONObject>) {
            if (holder.response != null) {
                try {
                    var currentObject: JSONObject
                    var type: String
                    val listItems: ArrayList<ListViewItem> = ArrayList(holder.response.length())
                    for (i in holder.response.keys()) {
                        try {
                            currentObject = holder.response.getJSONObject(i)
                            type = currentObject.getString("type")
                            listItems += ListViewItem(
                                    title = currentObject.getString("name"),
                                    summary = resources.getString(R.string.hue_tap),
                                    hidden = "${currentObject.getJSONArray("lights")}@${if (type == "Room") "room" else "zone"}#$i",
                                    icon = if (type == "Room") R.drawable.ic_room else R.drawable.ic_zone,
                                    state = currentObject.getJSONObject("action").getBoolean("on")
                            )
                        } catch (e: JSONException) {
                            Log.e(Global.LOG_TAG, e.toString())
                        }
                    }
                    adapter.updateData(listItems, hueGroupStateListener)
                    setLevelTwo(devices.getDeviceById(holder.deviceId), Flavors.TWO_HUE)
                } catch (e: Exception) {
                    handleErrorOnLevelOne(resources.getString(R.string.err_wrong_format_summary))
                    Log.e(Global.LOG_TAG, e.toString())
                }
            } else {
                handleErrorOnLevelOne(holder.errorMessage)
            }
        }
    }
    private val hueRequestUpdaterCallBack = object : HueAPI.RequestCallBack {
        override fun onLightsLoaded(holder: RequestCallbackObject<JSONObject>) {}
        override fun onGroupsLoaded(holder: RequestCallbackObject<JSONObject>) {
            if (holder.response != null) {
                try {
                    for (i in 0 until holder.response.length()) {
                        adapter.updateSwitch(recyclerView, i, holder.response
                                .getJSONObject(holder.response.names()?.getString(i) ?: "")
                                .getJSONObject("state")
                                .getBoolean("any_on")
                        )
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
    internal var tasmota: Tasmota? = null
    private val tasmotaRequestCallBack = object : Tasmota.RequestCallBack {
        override fun onItemsChanged(context: Context) {
            adapter.updateData(tasmota?.loadList() ?: arrayListOf(), preferredAnimationState = false)
        }

        override fun onResponse(context: Context, response: String) {
            Snackbar.make(recyclerView, R.string.main_execution_completed, Snackbar.LENGTH_LONG).setAction(R.string.str_show) {
                AlertDialog.Builder(context)
                        .setTitle(R.string.main_execution_completed)
                        .setMessage(response)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
            }.show()
        }
    }

    /*
     * Things related to Shelly
     */
    private var shelly: ShellyAPI? = null
    internal val shellyStateListener = CompoundButton.OnCheckedChangeListener { compoundButton, b ->
        if (compoundButton.isPressed) {
            val view = (compoundButton.parent as ViewGroup)
            view.findViewById<TextView>(R.id.summary).text = resources.getString(
                if (b) R.string.shelly_switch_summary_on
                else R.string.shelly_switch_summary_off
            )
            shelly?.changeSwitchState(
                view.findViewById<TextView>(R.id.hidden).text.toString().toInt(),
                b
            )
        }
    }
    private val shellyRequestCallBack = object : ShellyAPI.RequestCallBack {
        override fun onSwitchesLoaded(holder: RequestCallbackObject<ArrayList<ListViewItem>>) {
            if (holder.response != null) {
                adapter.updateData(holder.response, shellyStateListener)
                setLevelTwo(devices.getDeviceById(holder.deviceId), Flavors.TWO_SHELLY)
            } else {
                handleErrorOnLevelOne(holder.errorMessage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.setNoActionBar(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devices = Devices(this)
        recyclerView = findViewById(R.id.recyclerView)
        deviceIcon = findViewById(R.id.deviceIcon)
        deviceName = findViewById(R.id.deviceName)
        fab = findViewById(R.id.fab)
        themeId = getThemeId()

        adapter = MainListAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            reset = true
            startActivity(Intent(this, DevicesActivity::class.java))
        }

        findViewById<ImageView>(R.id.menu_icon).setOnClickListener {
            reset = true
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        //Handle shortcut
        if(intent.hasExtra("device")) {
            val deviceId = intent.getStringExtra("device") ?: ""
            if (devices.idExists(deviceId)) {
                if (checkNetwork()) {
                    val device = devices.getDeviceById(deviceId)
                    deviceIcon.setImageResource(device.iconId)
                    deviceName.text = device.name
                    handleLevelOne(deviceId)
                } else {
                    loadDevices()
                    Toast.makeText(this, R.string.main_network_not_secure, Toast.LENGTH_LONG).show()
                }
            } else {
                loadDevices()
                Toast.makeText(this, R.string.main_device_nonexistent, Toast.LENGTH_LONG).show()
            }
        } else {
            loadDevices()
        }
    }

    private fun checkNetwork() : Boolean {
        if (
            !PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("local_only", true)
        ) return true

        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return if (capabilities != null) {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else true
    }

    private fun handleLevelOne(deviceId: String) {
        val deviceObj = devices.getDeviceById(deviceId)
        when (deviceObj.mode) {
            "Website" -> {
                startActivity(
                    Intent(this, WebActivity::class.java)
                        .putExtra("URI", deviceObj.address)
                        .putExtra("title", deviceObj.name)
                )
                reset = true
            }
            "ESP Easy" -> {
                espEasy = EspEasyAPI(this, deviceId)
                espEasy?.loadInfo(espEasyRequestCallBack)
            }
            "Fritz! Auto-Login" -> {
                startActivity(
                    Intent(this, WebActivity::class.java)
                        .putExtra("URI", deviceObj.address)
                        .putExtra("title", deviceObj.name)
                        .putExtra("fritz_auto_login", deviceObj.id)
                )
                reset = true
            }
            "Node-RED" -> {
                startActivity(
                    Intent(this, WebActivity::class.java)
                        .putExtra("URI", deviceObj.address + "ui/")
                        .putExtra("title", deviceObj.name)
                )
                reset = true
            }
            "SimpleHome API" ->
                homeAPI.loadCommands(deviceId, homeRequestCallBack)
            "Hue API" -> {
                hueAPI = HueAPI(this, deviceId)
                hueAPI?.loadGroups(hueRequestCallBack)
                updateHandler.setUpdateFunction {
                    if (canReceiveRequest && hueAPI?.readyForRequest == true) {
                        hueAPI?.loadGroups(hueRequestUpdaterCallBack)
                    }
                }
            }
            "Tasmota" -> {
                tasmota = Tasmota(this, deviceId)
                adapter.updateData(tasmota?.loadList() ?: arrayListOf())
                setLevelTwo(deviceObj, Flavors.TWO_TASMOTA)
            }
            "Shelly Gen 1" -> {
                //TODO: check if authenticated
                shelly = ShellyAPI(this, deviceId, 1)
                shelly?.loadSwitches(shellyRequestCallBack)
            }
            "Shelly Gen 2" -> {
                //TODO: check if authenticated
                shelly = ShellyAPI(this, deviceId, 2)
                shelly?.loadSwitches(shellyRequestCallBack)
            }
            else ->
                Toast.makeText(this, R.string.main_unknown_mode, Toast.LENGTH_LONG).show()
        }
    }

    internal fun handleErrorOnLevelOne(err: String) {
        if (currentView == null) {
            loadDevices()
            Toast.makeText(this, err, Toast.LENGTH_LONG).show()
        } else if (level == Flavors.ONE) {
            currentView?.findViewById<TextView>(R.id.summary)?.text = err
        }
    }

    private fun loadDevices() {
        updateHandler.stop()
        val listItems: ArrayList<ListViewItem> = ArrayList(devices.length())
        try {
            if (devices.length() == 0) {
                listItems += ListViewItem(
                        title = resources.getString(R.string.main_no_devices),
                        summary = resources.getString(R.string.main_no_devices_summary),
                        icon = R.drawable.ic_info
                )
            } else {
                var currentDevice: DeviceItem
                for (i in 0 until devices.length()) {
                    currentDevice = devices.getDeviceByIndex(i)
                    listItems += ListViewItem(
                            title = currentDevice.name,
                            summary = resources.getString(R.string.main_tap_to_connect),
                            hidden = currentDevice.id,
                            icon = currentDevice.iconId
                    )
                }
            }
        } catch (e: Exception) {
            listItems += ListViewItem(
                    title = resources.getString(R.string.err_wrong_format),
                    summary = resources.getString(R.string.err_wrong_format_summary),
                    hidden = "none",
                    icon = R.drawable.ic_warning
            )
            Log.e(Global.LOG_TAG, e.toString())
        }
        adapter.updateData(listItems)
        setLevelOne()
    }

    override fun onBackPressed() {
        if (level != Flavors.ONE)
            loadDevices()
        else
            super.onBackPressed()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val hidden = v?.findViewById<TextView>(R.id.hidden)?.text ?: return
        if (hidden.contains("tasmota_command")) {
            tasmotaPosition = hidden.substring(hidden.lastIndexOf("#") + 1).toInt()
            menuInflater.inflate(R.menu.activity_main_tasmota_context, menu)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.title) {
            resources.getString(R.string.str_edit) -> {
                tasmota?.updateItem(tasmotaRequestCallBack, tasmotaPosition)
                true
            }
            resources.getString(R.string.str_delete) -> {
                AlertDialog.Builder(this)
                        .setTitle(R.string.str_delete)
                        .setMessage(R.string.tasmota_delete_command)
                        .setPositiveButton(R.string.str_delete) { _, _ ->
                            tasmota?.removeFromList(tasmotaRequestCallBack, tasmotaPosition)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun setLevelOne() {
        deviceIcon.setImageResource(R.drawable.ic_home_white)
        deviceName.text = resources.getString(R.string.main_device_name)
        fab.show()
        level = Flavors.ONE
    }

    internal fun setLevelTwo(device: DeviceItem, flavor: Flavors) {
        fab.hide()
        deviceIcon.setImageResource(device.iconId)
        deviceName.text = device.name
        currentDevice = device.id
        level = flavor
    }

    override fun onStart() {
        super.onStart()
        if (getThemeId() != themeId) {
            themeId = getThemeId()
            recreate()
        }

        canReceiveRequest = true
        if (reset) {
            loadDevices()
            reset = false
        }
    }

    override fun onStop() {
        super.onStop()
        canReceiveRequest = false
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHandler.stop()
    }

    override fun onItemClicked(view: View, position: Int) {
        currentView = view
        val title = view.findViewById<TextView>(R.id.title).text.toString()
        if (title == resources.getString(R.string.main_no_devices) || title == resources.getString(R.string.err_wrong_format)) {
            reset = true
            startActivity(Intent(this, DevicesActivity::class.java))
            return
        }
        val hidden = view.findViewById<TextView>(R.id.hidden).text.toString()
        when (level) {
            Flavors.ONE -> {
                if (checkNetwork()) {
                    view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_connecting)
                    handleLevelOne(hidden)
                } else {
                    view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_network_not_secure)
                }
            }
            Flavors.TWO_SIMPLE_HOME ->
                homeAPI.executeCommand(currentDevice, hidden, homeRequestCallBack)
            Flavors.TWO_ESPEASY -> {
                if (hidden.startsWith("http://") || hidden.startsWith("https://")) {
                    startActivity(
                        Intent(this, WebActivity::class.java)
                            .putExtra("URI", hidden)
                            .putExtra("title", title)
                    )
                }
            }
            Flavors.TWO_HUE ->
                startActivity(
                    Intent(this, HueLampActivity::class.java)
                        .putExtra("ID", hidden.substring(hidden.lastIndexOf("@") + 1))
                        .putExtra("Device", currentDevice)
                )
            Flavors.TWO_TASMOTA -> {
                when (hidden) {
                    "add" -> tasmota?.addToList(tasmotaRequestCallBack)
                    "execute_once" -> tasmota?.executeOnce(tasmotaRequestCallBack)
                    else -> tasmota?.execute(tasmotaRequestCallBack, view.findViewById<TextView>(R.id.summary).text.toString())
                }
            }
            Flavors.TWO_SHELLY -> {
                if (hidden.startsWith("http://") || hidden.startsWith("https://")) {
                    startActivity(
                        Intent(this, WebActivity::class.java)
                            .putExtra("URI", hidden)
                            .putExtra("title", title)
                    )
                }
            }
        }
    }
}
