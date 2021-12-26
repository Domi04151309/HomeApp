package io.github.domi04151309.home.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.github.domi04151309.home.api.SimpleHomeAPI
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
import io.github.domi04151309.home.helpers.Theme
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.adapters.MainListAdapter
import io.github.domi04151309.home.api.*
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class MainActivity : AppCompatActivity() {

    companion object {
        private val WEB_MODES = arrayOf(
            "Fritz! Auto-Login", "Node-RED", "Website"
        )
        private val UNIFIED_MODES = arrayOf(
            "ESP Easy", "Hue API", "Shelly Gen 1", "Shelly Gen 2", "SimpleHome API", "Tasmota"
        )
    }

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
                val device = devices.getDeviceById(holder.deviceId)
                deviceIcon.setImageResource(device.iconId)
                deviceName.text = device.name
                adapter.updateData(holder.response, recyclerViewInterface)
                fab.hide()
                level = Flavors.TWO
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
    private val unifiedHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {
            if (data.hidden.isEmpty()) return
            if (unified?.dynamicSummaries == true) view.findViewById<TextView>(R.id.summary).text =
                resources.getString(
                    if (state) R.string.switch_summary_on
                    else R.string.switch_summary_off
                )
            unified?.changeSwitchState(data.hidden, state)
        }

        override fun onItemClicked(view: View, data: ListViewItem) {
            unified?.execute(data.hidden, unifiedRequestCallback)
        }
    }
    private val unifiedRealTimeStatesCallback = object : UnifiedAPI.RealTimeStatesCallback {
        override fun onStatesLoaded(states: ArrayList<Boolean?>) {
            for (i in 0 until states.size) {
                if (states[i] != null) adapter.updateSwitch(recyclerView, i, states[i] ?: return)
            }
        }
    }

    /*
     * Things related to Tasmota
     */
    private val tasmotaHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {}
        override fun onItemClicked(view: View, data: ListViewItem) {
            val helper = TasmotaHelper(this@MainActivity, unified ?: return)
            when (data.hidden) {
                "add" -> helper.addToList(unifiedRequestCallback)
                "execute_once" -> helper.executeOnce(unifiedRequestCallback)
                else -> unified?.execute(view.findViewById<TextView>(R.id.summary).text.toString(), unifiedRequestCallback)
            }
        }
    }

    /*
     * Things related to the main menu
     */
    private val mainHelperInterface = object : HomeRecyclerViewHelperInterface {
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {}
        override fun onItemClicked(view: View, data: ListViewItem) {
            currentView = view
            if (data.title == resources.getString(R.string.main_no_devices)) {
                reset = true
                startActivity(Intent(this@MainActivity, DevicesActivity::class.java))
                return
            } else if (data.title == resources.getString(R.string.err_wrong_format)) {
                reset = true
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
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
        try {
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
        } catch (e: Exception) {
            loadDevices()
            Toast.makeText(this, R.string.err_wrong_format_summary, Toast.LENGTH_LONG).show()
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
        when {
            WEB_MODES.contains(deviceObj.mode) -> {
                val intent = Intent(this, WebActivity::class.java)
                    .putExtra("title", deviceObj.name)

                when (deviceObj.mode) {
                    "Fritz! Auto-Login" -> {
                        intent.putExtra("URI", deviceObj.address)
                        intent.putExtra("fritz_auto_login", deviceObj.id)
                    }
                    "Node-RED" -> {
                        intent.putExtra("URI", deviceObj.address + "ui/")
                    }
                    "Website" -> {
                        intent.putExtra("URI", deviceObj.address)
                    }
                }
                startActivity(intent)
                reset = true
            }
            UNIFIED_MODES.contains(deviceObj.mode) -> {
                unified = when (deviceObj.mode) {
                    "ESP Easy" -> EspEasyAPI(this, deviceId, unifiedHelperInterface)
                    "Hue API" -> HueAPI(this, deviceId, unifiedHelperInterface)
                    "SimpleHome API" -> SimpleHomeAPI(this, deviceId, unifiedHelperInterface)
                    "Tasmota" -> Tasmota(this, deviceId, tasmotaHelperInterface)
                    "Shelly Gen 1" -> ShellyAPI(this, deviceId, unifiedHelperInterface, 1)
                    "Shelly Gen 2" -> ShellyAPI(this, deviceId, unifiedHelperInterface, 2)
                    else -> null
                }
                unified?.loadList(unifiedRequestCallback)
                updateHandler.setUpdateFunction {
                    if (canReceiveRequest) unified?.loadStates(unifiedRealTimeStatesCallback)
                }
            }
            else -> {
                Toast.makeText(this, R.string.main_unknown_mode, Toast.LENGTH_LONG).show()
            }
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

    //TODO: live data on level one
    //TODO: update onclick interface
    private fun loadDevices() {
        updateHandler.stop()
        val listItems: ArrayList<ListViewItem> = arrayListOf()
        try {
            listItems.ensureCapacity(devices.length())
            if (devices.length() == 0) {
                listItems += ListViewItem(
                    title = resources.getString(R.string.main_no_devices),
                    summary = resources.getString(R.string.main_no_devices_summary),
                    icon = R.drawable.ic_info
                )
            } else {
                for (i in 0 until devices.length()) {
                    val currentDevice = devices.getDeviceByIndex(i)
                    if (!currentDevice.hide) {
                        if (currentDevice.directView && UNIFIED_MODES.contains(currentDevice.mode)) {
                            Log.wtf(Global.LOG_TAG, "can perform request")
                            val api = when (currentDevice.mode) {
                                "ESP Easy" -> EspEasyAPI(this, currentDevice.id, null)
                                "Hue API" -> HueAPI(this, currentDevice.id, null)
                                "SimpleHome API" -> SimpleHomeAPI(this, currentDevice.id, null)
                                "Tasmota" -> Tasmota(this, currentDevice.id, null)
                                "Shelly Gen 1" -> ShellyAPI(this, currentDevice.id, null, 1)
                                "Shelly Gen 2" -> ShellyAPI(this, currentDevice.id, null, 2)
                                else -> null
                            }
                            api?.loadList(object : UnifiedAPI.CallbackInterface {
                                override fun onItemsLoaded(
                                    holder: UnifiedRequestCallback,
                                    recyclerViewInterface: HomeRecyclerViewHelperInterface?
                                ) {
                                    if (holder.response != null) adapter.insertItems(holder.response, i)
                                }

                                override fun onExecuted(
                                    result: String,
                                    deviceId: String,
                                    shouldRefresh: Boolean
                                ) {
                                    //TODO: Not yet implemented
                                }
                            })
                        }
                        listItems += ListViewItem(
                            title = currentDevice.name,
                            summary = resources.getString(R.string.main_tap_to_connect),
                            hidden = currentDevice.id,
                            icon = currentDevice.iconId
                        )
                    }
                }
            }
        } catch (e: Exception) {
            listItems += ListViewItem(
                title = resources.getString(R.string.err_wrong_format),
                summary = resources.getString(R.string.err_wrong_format_summary),
                hidden = "none",
                icon = R.drawable.ic_warning
            )
        }
        adapter.updateData(listItems, mainHelperInterface)
        deviceIcon.setImageResource(R.drawable.ic_home_white)
        deviceName.text = resources.getString(R.string.main_device_name)
        fab.show()
        level = Flavors.ONE
        unified = null
    }
}
