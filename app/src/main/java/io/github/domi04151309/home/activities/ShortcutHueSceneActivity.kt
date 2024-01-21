package io.github.domi04151309.home.activities

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.SimpleListAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.api.UnifiedAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface
import org.json.JSONObject

class ShortcutHueSceneActivity : BaseActivity(), RecyclerViewHelperInterface {
    private var deviceId: String? = null
    private var group: String? = null
    private lateinit var recyclerView: RecyclerView

    private val device: DeviceItem
        get() = Devices(this).getDeviceById(deviceId ?: error("Device ID is null."))

    private val api: HueAPI
        get() = HueAPI(this, deviceId ?: error("Device ID is null."))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        recyclerView = findViewById(R.id.recyclerView)

        val devices = Devices(this)
        val listItems: ArrayList<SimpleListItem> = ArrayList(devices.length)
        var currentDevice: DeviceItem
        for (i in 0 until devices.length) {
            currentDevice = devices.getDeviceByIndex(i)
            if (currentDevice.mode == Global.HUE_API) {
                listItems +=
                    SimpleListItem(
                        title = currentDevice.name,
                        summary = currentDevice.address,
                        hidden = currentDevice.id,
                        icon = currentDevice.iconId,
                    )
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SimpleListAdapter(listItems, this)
    }

    private fun loadDevice(view: View) {
        deviceId = view.findViewById<TextView>(R.id.hidden).text.toString()
        api.loadList(
            object : UnifiedAPI.CallbackInterface {
                override fun onItemsLoaded(
                    holder: UnifiedRequestCallback,
                    recyclerViewInterface: HomeRecyclerViewHelperInterface?,
                ) {
                    if (holder.response != null) {
                        recyclerView.adapter =
                            SimpleListAdapter(
                                holder.response as List<SimpleListItem>,
                                this@ShortcutHueSceneActivity,
                            )
                    } else {
                        deviceId = null
                        Toast.makeText(
                            this@ShortcutHueSceneActivity,
                            holder.errorMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                override fun onExecuted(
                    result: String,
                    shouldRefresh: Boolean,
                ) {
                    // Do nothing.
                }
            },
        )
    }

    private fun loadGroup(view: View) {
        group = view.findViewById<TextView>(R.id.hidden).text.toString()
        Volley.newRequestQueue(this).add(
            JsonObjectRequest(
                Request.Method.GET,
                device.address + "api/" + api.getUsername() + "/scenes/",
                null,
                { response ->
                    val listItems: ArrayList<SimpleListItem> = ArrayList(response.length() / 2)
                    var currentObject: JSONObject
                    for (i in response.keys()) {
                        currentObject = response.getJSONObject(i)
                        if (currentObject.optString("group") == group) {
                            listItems.add(
                                SimpleListItem(
                                    currentObject.optString("name"),
                                    resources.getString(R.string.hue_tap),
                                    i,
                                    R.drawable.ic_scene,
                                ),
                            )
                        }
                    }
                    listItems.sortBy { it.title }
                    recyclerView.adapter = SimpleListAdapter(listItems, this)
                },
                { error ->
                    group = null
                    Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG)
                        .show()
                },
            ),
        )
    }

    private fun createShortcut(view: View) {
        val lampName = view.findViewById<TextView>(R.id.title).text
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = this.getSystemService(ShortcutManager::class.java)
            if (shortcutManager != null) {
                setResult(
                    RESULT_OK,
                    shortcutManager.createShortcutResultIntent(
                        ShortcutInfo.Builder(this, device.id + lampName)
                            .setShortLabel(view.findViewById<TextView>(R.id.title).text)
                            .setLongLabel(view.findViewById<TextView>(R.id.title).text)
                            .setIcon(Icon.createWithResource(this, device.iconId))
                            .setIntent(
                                Intent(this, ShortcutHueSceneActionActivity::class.java)
                                    .putExtra(
                                        "scene",
                                        view.findViewById<TextView>(R.id.hidden).text,
                                    )
                                    .putExtra("group", group)
                                    .putExtra(Devices.INTENT_EXTRA_DEVICE, device.id)
                                    .setAction(Intent.ACTION_MAIN)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            .build(),
                    ),
                )
                finish()
            }
        } else {
            Toast.makeText(this, R.string.pref_add_shortcut_failed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onItemClicked(
        view: View,
        position: Int,
    ) {
        if (deviceId == null) {
            loadDevice(view)
        } else if (group == null) {
            loadGroup(view)
        } else {
            createShortcut(view)
        }
    }
}
