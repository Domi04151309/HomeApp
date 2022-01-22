package io.github.domi04151309.home.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.github.domi04151309.home.api.SimpleHomeAPI
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.*
import androidx.recyclerview.widget.RecyclerView
import io.github.domi04151309.home.adapters.MainListAdapter
import io.github.domi04151309.home.api.*
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Global.checkNetwork
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import android.util.DisplayMetrics
import android.view.Gravity
import androidx.recyclerview.widget.GridLayoutManager
import kotlin.math.max
import kotlin.math.min
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private val WEB_MODES = arrayOf(
            "Fritz! Auto-Login", "Node-RED", "Website"
        )
        private val UNIFIED_MODES = arrayOf(
            "ESP Easy", "Hue API", "Shelly Gen 1", "Shelly Gen 2", "SimpleHome API", "Tasmota"
        )
    }

    private var tasmotaPosition: Int = 0
    private var shouldReset : Boolean = false
    private val updateHandler = UpdateHandler()
    private var isDeviceSelected = false
    private var canReceiveRequest = false
    private var currentView: View? = null
    internal lateinit var devices: Devices
    internal lateinit var adapter: MainListAdapter
    private lateinit var deviceIcon: ImageSwitcher
    private lateinit var deviceName: TextSwitcher
    private lateinit var fab: FloatingActionButton

    private var themeId = ""
    private fun getThemeId(): String =
        PreferenceManager.getDefaultSharedPreferences(this)
            .getString(P.PREF_THEME, P.PREF_THEME_DEFAULT) ?: P.PREF_THEME_DEFAULT
    private var columns: Int? = null
    private fun getColumns(): Int? = (
            PreferenceManager.getDefaultSharedPreferences(this)
                .getString(P.PREF_COLUMNS, P.PREF_COLUMNS_DEFAULT) ?: P.PREF_COLUMNS_DEFAULT
            ).toIntOrNull()

    /*
     * Unified callbacks
     */
    private var unified: UnifiedAPI? = null
    private val unifiedRequestCallback = object : UnifiedAPI.CallbackInterface {
        override fun onItemsLoaded(holder: UnifiedRequestCallback, recyclerViewInterface: HomeRecyclerViewHelperInterface?) {
            if (holder.response != null) {
                val device = devices.getDeviceById(holder.deviceId)
                deviceIcon.setImageResource(device.iconId)
                deviceName.setText(device.name)
                adapter.updateData(holder.response, recyclerViewInterface)
                fab.hide()
                isDeviceSelected = true
            } else {
                if (currentView == null) {
                    loadDeviceList()
                    Toast.makeText(this@MainActivity, holder.errorMessage, Toast.LENGTH_LONG).show()
                } else {
                    currentView?.findViewById<TextView>(R.id.summary)?.text = holder.errorMessage
                }
            }
        }

        override fun onExecuted(result: String, shouldRefresh: Boolean) {
            showExecutionResult(result)
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
        override fun onStatesLoaded(states: ArrayList<Boolean?>, offset: Int, dynamicSummary: Boolean) {
            for (i in 0 until states.size) {
                if (states[i] != null) adapter.updateSwitch(i + offset, states[i] ?: return, dynamicSummary)
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
        override fun onStateChanged(view: View, data: ListViewItem, state: Boolean) {
            if (data.hidden.isEmpty()) return

            val deviceId = data.hidden.substring(0, data.hidden.indexOf('@'))
            val api = getCorrectAPI(devices.getDeviceById(deviceId).mode, deviceId)
            if (api?.dynamicSummaries == true) view.findViewById<TextView>(R.id.summary).text =
                resources.getString(
                    if (state) R.string.switch_summary_on
                    else R.string.switch_summary_off
                )
            api?.changeSwitchState(data.hidden.substring(deviceId.length + 1), state)
        }
        override fun onItemClicked(view: View, data: ListViewItem) {
            currentView = view
            if (data.title == resources.getString(R.string.main_no_devices)) {
                startActivityAndReset(Intent(this@MainActivity, DevicesActivity::class.java))
            } else if (data.title == resources.getString(R.string.err_wrong_format)) {
                startActivityAndReset(Intent(this@MainActivity, SettingsActivity::class.java))
            } else if (data.hidden.contains('@')) {
                val deviceId = data.hidden.substring(0, data.hidden.indexOf('@'))
                val api = getCorrectAPI(devices.getDeviceById(deviceId).mode, deviceId)
                api?.execute(
                    data.hidden.substring(deviceId.length + 1),
                    object : UnifiedAPI.CallbackInterface {
                        override fun onItemsLoaded(
                            holder: UnifiedRequestCallback,
                            recyclerViewInterface: HomeRecyclerViewHelperInterface?
                        ) {}
                        override fun onExecuted(result: String, shouldRefresh: Boolean) {
                            showExecutionResult(result)
                            if (shouldRefresh) {
                                api.loadList(object : UnifiedAPI.CallbackInterface {
                                    override fun onItemsLoaded(
                                        holder: UnifiedRequestCallback,
                                        recyclerViewInterface: HomeRecyclerViewHelperInterface?
                                    ) {
                                        adapter.updateDirectView(
                                            deviceId,
                                            holder.response ?: arrayListOf(),
                                            adapter.getDirectViewPos(deviceId)
                                        )
                                    }
                                    override fun onExecuted(result: String, shouldRefresh: Boolean) {}
                                })
                            }
                        }
                    }
                )
            } else {
                if (checkNetwork(this@MainActivity)) {
                    view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_connecting)
                    selectDevice(data.hidden)
                } else {
                    view.findViewById<TextView>(R.id.summary).text = resources.getString(R.string.main_network_not_secure)
                }
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

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        devices = Devices(this)
        deviceIcon = findViewById(R.id.deviceIcon)
        deviceName = findViewById(R.id.deviceName)
        fab = findViewById(R.id.fab)
        themeId = getThemeId()
        columns = getColumns()

        deviceIcon.setFactory {
            val view = ImageView(this@MainActivity)
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            view
        }
        deviceName.setFactory {
            val view = TextView(this@MainActivity)
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            view.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
            view.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            view.gravity = Gravity.CENTER_VERTICAL
            view.ellipsize = TextUtils.TruncateAt.END
            view.maxLines = 1
            view
        }

        val inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        inAnimation.duration = inAnimation.duration / 2
        outAnimation.duration = outAnimation.duration / 2
        deviceIcon.inAnimation = inAnimation
        deviceIcon.outAnimation = outAnimation
        deviceName.inAnimation = inAnimation
        deviceName.outAnimation = outAnimation

        adapter = MainListAdapter(recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, numberOfRows())
        recyclerView.adapter = adapter

        fab.setOnClickListener {
            startActivityAndReset(Intent(this, DevicesActivity::class.java))
        }

        findViewById<ImageButton>(R.id.menu_icon).setOnClickListener {
            startActivityAndReset(Intent(this, SettingsActivity::class.java))
        }

        //Handle shortcut
        try {
            if (intent.hasExtra("device")) {
                val deviceId = intent.getStringExtra("device") ?: ""
                if (devices.idExists(deviceId)) {
                    if (checkNetwork(this)) {
                        val device = devices.getDeviceById(deviceId)
                        deviceIcon.setImageResource(device.iconId)
                        deviceName.setText(device.name)
                        selectDevice(deviceId)
                    } else {
                        loadDeviceList()
                        Toast.makeText(this, R.string.main_network_not_secure, Toast.LENGTH_LONG).show()
                    }
                } else {
                    loadDeviceList()
                    Toast.makeText(this, R.string.main_device_nonexistent, Toast.LENGTH_LONG).show()
                }
            } else {
                loadDeviceList()
            }
        } catch (e: Exception) {
            loadDeviceList()
            Toast.makeText(this, R.string.err_wrong_format_summary, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        if (isDeviceSelected) loadDeviceList()
        else super.onBackPressed()
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val hidden = v?.findViewById<TextView>(R.id.hidden)?.text ?: return
        if (hidden.contains("tasmota_command")) {
            tasmotaPosition = hidden.substring(hidden.lastIndexOf('#') + 1).toInt()
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
        if (getColumns() != columns) {
            columns = getColumns()
            recreate()
        }
        if (shouldReset) {
            loadDeviceList()
            shouldReset = false
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

    private fun numberOfRows(): Int {
        if (columns != null) return columns ?: 1
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val horizontal: Int = ((displayMetrics.widthPixels / displayMetrics.density) / 240).toInt()
        val vertical: Int = ((displayMetrics.heightPixels / displayMetrics.density) / 240).toInt()
        return max(1, min(horizontal, vertical))
    }

    internal fun selectDevice(deviceId: String) {
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
                startActivityAndReset(intent)
            }
            UNIFIED_MODES.contains(deviceObj.mode) -> {
                unified = getCorrectAPI(deviceObj.mode, deviceId, unifiedHelperInterface, tasmotaHelperInterface)
                unified?.loadList(unifiedRequestCallback)
                updateHandler.setUpdateFunction {
                    if (canReceiveRequest && unified?.needsRealTimeData == true)
                        unified?.loadStates(unifiedRealTimeStatesCallback, 0)
                }
            }
            else -> {
                Toast.makeText(this, R.string.main_unknown_mode, Toast.LENGTH_LONG).show()
            }
        }
    }

    internal fun loadDeviceList() {
        updateHandler.stop()
        val registeredForUpdates: HashMap<Int, UnifiedAPI?> = hashMapOf()
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
                var actualPosition = 0
                for (i in 0 until devices.length()) {
                    val currentDevice = devices.getDeviceByIndex(i)
                    if (!currentDevice.hide) {
                        if (
                            currentDevice.directView
                            && UNIFIED_MODES.contains(currentDevice.mode)
                            && checkNetwork(this)
                        ) {
                            actualPosition.let {
                                val api = getCorrectAPI(currentDevice.mode, currentDevice.id)
                                api?.loadList(object : UnifiedAPI.CallbackInterface {
                                    override fun onItemsLoaded(
                                        holder: UnifiedRequestCallback,
                                        recyclerViewInterface: HomeRecyclerViewHelperInterface?
                                    ) {
                                        if (holder.response != null) {
                                            adapter.updateDirectView(
                                                currentDevice.id, holder.response, it
                                            )
                                            registeredForUpdates[it] = api
                                        }
                                    }
                                    override fun onExecuted(
                                        result: String,
                                        shouldRefresh: Boolean
                                    ) {}
                                })
                            }

                        }
                        listItems += ListViewItem(
                            title = currentDevice.name,
                            summary = resources.getString(R.string.main_tap_to_connect),
                            hidden = currentDevice.id,
                            icon = currentDevice.iconId
                        )
                        actualPosition++
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
        deviceName.setText(resources.getString(R.string.main_device_name))
        fab.show()
        isDeviceSelected = false
        updateHandler.setUpdateFunction {
            if (canReceiveRequest) {
                for (i in registeredForUpdates.keys) {
                    if (registeredForUpdates[i]?.needsRealTimeData == true) {
                        registeredForUpdates[i]?.loadStates(
                            unifiedRealTimeStatesCallback,
                            adapter.getOffset(i)
                        )
                    }
                }
            }
        }
        unified = null
    }

    internal fun getCorrectAPI(
        identifier: String,
        deviceId: String,
        recyclerViewInterface: HomeRecyclerViewHelperInterface? = null,
        tasmotaHelperInterface: HomeRecyclerViewHelperInterface? = null
    ): UnifiedAPI? {
        return when (identifier) {
            "ESP Easy" -> EspEasyAPI(this, deviceId, recyclerViewInterface)
            "Hue API" -> HueAPI(this, deviceId, recyclerViewInterface)
            "SimpleHome API" -> SimpleHomeAPI(this, deviceId, recyclerViewInterface)
            "Tasmota" -> Tasmota(this, deviceId, tasmotaHelperInterface ?: recyclerViewInterface)
            "Shelly Gen 1" -> ShellyAPI(this, deviceId, recyclerViewInterface, 1)
            "Shelly Gen 2" -> ShellyAPI(this, deviceId, recyclerViewInterface, 2)
            else -> null
        }
    }

    internal fun startActivityAndReset(intent: Intent) {
        shouldReset = true
        startActivity(intent)
    }

    internal fun showExecutionResult(result: String) {
        if (result.length < 64) {
            Toast.makeText(this, result, Toast.LENGTH_LONG).show()
        } else {
            Snackbar
                .make(findViewById(android.R.id.content), R.string.main_execution_completed, Snackbar.LENGTH_LONG)
                .setAction(R.string.str_show) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.main_execution_completed)
                        .setMessage(result)
                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                        .show()
                }.show()
        }
    }
}
