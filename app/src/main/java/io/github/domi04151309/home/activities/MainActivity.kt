package io.github.domi04151309.home.activities

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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.ListViewItem
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
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class MainActivity : AppCompatActivity() {

    enum class Flavors {
        ONE, TWO
    }

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
     * Unified callbacks
     */
    private var unified: UnifiedAPI? = null
    private val unifiedRequestCallback = object : UnifiedAPI.CallbackInterface {
        override fun onItemsLoaded(holder: UnifiedRequestCallback, recyclerViewInterface: HomeRecyclerViewHelperInterface?) {
            if (holder.response != null) {
                adapter.updateData(holder.response, recyclerViewInterface)
                setLevelTwo(devices.getDeviceById(holder.deviceId))
            } else {
                handleErrorOnLevelOne(holder.errorMessage)
            }
        }

        override fun onExecuted(result: String, deviceId: String, shouldRefresh: Boolean) {
            if (result.length < 64) {
                Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
            } else {
                Snackbar.make(recyclerView, R.string.main_execution_completed, Snackbar.LENGTH_LONG).setAction(R.string.str_show) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.main_execution_completed)
                        .setMessage(result)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
                }.show()
            }
            if (shouldRefresh) unified?.loadList(this)
        }
    }

    /*
     * Things related to ESP Easy and Shelly
     */
    private val switchOnlyHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {
            if (data.hidden.isEmpty()) return
            view.findViewById<TextView>(R.id.summary).text = resources.getString(
                if (state) R.string.shelly_switch_summary_on
                else R.string.shelly_switch_summary_off
            )
            unified?.changeSwitchState(data.hidden.toInt(), state)
        }
        override fun onItemClicked(view: View, data: ListViewItem, position: Int) {}
    }

    /*
     * Things related to the Home API
     */
    private val homeHelperInterface: HomeRecyclerViewHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) { }
        override fun onItemClicked(view: View, data: ListViewItem, position: Int) {
            unified?.execute(data.hidden, unifiedRequestCallback)
        }
    }

    /*
     * Things related to Tasmota
     */
    private val tasmotaHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {}
        override fun onItemClicked(view: View, data: ListViewItem, position: Int) {
            val helper = TasmotaHelper(this@MainActivity, unified ?: return)
            when (data.hidden) {
                "add" -> helper.addToList(unifiedRequestCallback)
                "execute_once" -> helper.executeOnce(unifiedRequestCallback)
                else -> unified?.execute(view.findViewById<TextView>(R.id.summary).text.toString(), unifiedRequestCallback)
            }
        }
    }

    /*
     * Things related to the Hue API
     */
    private var hueAPI: HueAPI? = null
    private val hueHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {
            hueAPI?.switchGroupByID(data.hidden.substring(data.hidden.lastIndexOf("#") + 1), state)
        }

        override fun onItemClicked(view: View, data: ListViewItem, position: Int) {
            startActivity(
                Intent(this@MainActivity, HueLampActivity::class.java)
                    .putExtra("ID", data.hidden)
                    .putExtra("Device", hueAPI?.deviceId)
            )
        }
    }
    private val hueRealTimeStatesCallback = object : HueAPI.RealTimeStatesCallback {
        override fun onStatesLoaded(states: ArrayList<Boolean>) {
            for (i in 0 until states.size) {
                adapter.updateSwitch(recyclerView, i, states[i])
            }
        }
    }

    /*
     * Things related to the main menu
     */
    private val mainHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {}
        override fun onItemClicked(view: View, data: ListViewItem, position: Int) {
            currentView = view
            if (data.title == resources.getString(R.string.main_no_devices) || data.title == resources.getString(R.string.err_wrong_format)) {
                reset = true
                startActivity(Intent(this@MainActivity, DevicesActivity::class.java))
                return
            }
            if (checkNetwork()) {
                view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_connecting)
                handleLevelOne(data.hidden)
            } else {
                view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_network_not_secure)
            }
        }
    }

    /*
     * Activity methods
     */
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

        adapter = MainListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            reset = true
            startActivity(Intent(this, DevicesActivity::class.java))
        }

        findViewById<ImageButton>(R.id.menu_icon).setOnClickListener {
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

    override fun onBackPressed() {
        if (level != Flavors.ONE) loadDevices()
        else super.onBackPressed()
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
        val helper = TasmotaHelper(this, unified ?: return super.onContextItemSelected(item))
        return when (item.title) {
            resources.getString(R.string.str_edit) -> {
                helper.updateItem(unifiedRequestCallback, tasmotaPosition)
                true
            }
            resources.getString(R.string.str_delete) -> {
                AlertDialog.Builder(this)
                        .setTitle(R.string.str_delete)
                        .setMessage(R.string.tasmota_delete_command)
                        .setPositiveButton(R.string.str_delete) { _, _ ->
                            helper.removeFromList(unifiedRequestCallback, tasmotaPosition)
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        if (getThemeId() != themeId) {
            themeId = getThemeId()
            recreate()
        }
        if (reset) {
            loadDevices()
            reset = false
        }
        canReceiveRequest = true
    }

    override fun onStop() {
        super.onStop()
        canReceiveRequest = false
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHandler.stop()
    }

    internal fun checkNetwork() : Boolean {
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

    internal fun handleLevelOne(deviceId: String) {
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
            "ESP Easy" -> {
                unified = EspEasyAPI(this, deviceId, switchOnlyHelperInterface)
                unified?.loadList(unifiedRequestCallback)
            }
            "SimpleHome API" -> {
                unified = SimpleHomeAPI(this, deviceId, homeHelperInterface)
                unified?.loadList(unifiedRequestCallback)
            }
            "Tasmota" -> {
                unified = Tasmota(this, deviceId, tasmotaHelperInterface)
                unified?.loadList(unifiedRequestCallback)
            }
            "Shelly Gen 1" -> {
                unified = ShellyAPI(this, deviceId, switchOnlyHelperInterface, 1)
                unified?.loadList(unifiedRequestCallback)
            }
            "Shelly Gen 2" -> {
                unified = ShellyAPI(this, deviceId, switchOnlyHelperInterface, 2)
                unified?.loadList(unifiedRequestCallback)
            }
            "Hue API" -> {
                hueAPI = HueAPI(this, deviceId, hueHelperInterface)
                hueAPI?.loadList(unifiedRequestCallback)
                updateHandler.setUpdateFunction {
                    if (canReceiveRequest && hueAPI?.readyForRequest == true) {
                        hueAPI?.loadStates(hueRealTimeStatesCallback)
                    }
                }
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
        adapter.updateData(listItems, mainHelperInterface)
        deviceIcon.setImageResource(R.drawable.ic_home_white)
        deviceName.text = resources.getString(R.string.main_device_name)
        fab.show()
        level = Flavors.ONE

        // Clean up memory
        unified = null
        hueAPI = null
    }

    internal fun setLevelTwo(device: DeviceItem) {
        fab.hide()
        deviceIcon.setImageResource(device.iconId)
        deviceName.text = device.name
        level = Flavors.TWO
    }
}
