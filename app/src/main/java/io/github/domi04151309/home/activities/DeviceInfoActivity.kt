package io.github.domi04151309.home.activities

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.SimpleListAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceInfoActivity : BaseActivity(), RecyclerViewHelperInterface {
    companion object {
        private const val TO_PERCENT = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        val devices = Devices(this)
        val id = intent.getStringExtra("device") ?: ""
        if (!devices.idExists(id)) {
            finish()
            return
        }

        val device = devices.getDeviceById(id)
        val queue = Volley.newRequestQueue(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val items = arrayListOf<SimpleListItem>()
        recyclerView.layoutManager = LinearLayoutManager(this)
        items.add(
            SimpleListItem(
                device.name,
                device.address,
                icon = device.iconId,
            ),
        )

        when (device.mode) {
            "Hue API" -> showHueInfo(device, queue, items, recyclerView)
            "Shelly Gen 2" -> showShelly2Info(device, queue, items, recyclerView)
        }
    }

    override fun onItemClicked(
        view: View,
        position: Int,
    ) {
        // Do nothing.
    }

    private fun boolToString(bool: Boolean): String {
        return resources.getString(if (bool) R.string.str_on else R.string.str_off)
    }

    @Suppress("MagicNumber")
    private fun rssiToPercent(rssi: Int): Int {
        return if (rssi <= -100) {
            0
        } else if (rssi >= -50) {
            100
        } else {
            2 * (rssi + 100)
        }
    }

    private fun showHueInfo(
        device: DeviceItem,
        queue: RequestQueue,
        items: ArrayList<SimpleListItem>,
        recyclerView: RecyclerView,
    ) {
        val hueAPI = HueAPI(this, device.id)
        val addressPrefix = device.address + "api/" + hueAPI.getUsername()

        queue.add(
            JsonObjectRequest(
                Request.Method.GET,
                "$addressPrefix/config",
                null,
                { response ->
                    items.addAll(
                        arrayOf(
                            SimpleListItem(summary = resources.getString(R.string.hue_bridge)),
                            SimpleListItem(
                                response.optString("name"),
                                resources.getString(R.string.hue_bridge_name),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                response.optString("modelid"),
                                resources.getString(R.string.hue_bridge_model),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                response.optString("bridgeid"),
                                resources.getString(R.string.hue_bridge_id),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                response.optString("swversion"),
                                resources.getString(R.string.hue_bridge_software),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                response.optString("zigbeechannel"),
                                resources.getString(R.string.hue_bridge_zigbee),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                response.optString("timezone"),
                                resources.getString(R.string.hue_bridge_time_zone),
                                icon = R.drawable.ic_about_info,
                            ),
                        ),
                    )

                    queue.add(
                        JsonObjectRequest(
                            Request.Method.GET,
                            "$addressPrefix/sensors",
                            null,
                            { innerResponse ->
                                val sensorItems = arrayListOf<SimpleListItem>()
                                for (i in innerResponse.keys()) {
                                    val current = innerResponse.optJSONObject(i) ?: JSONObject()
                                    val config = current.optJSONObject("config") ?: JSONObject()
                                    if (config.has("battery")) {
                                        sensorItems.add(
                                            SimpleListItem(
                                                current.optString("name"),
                                                config.optString("battery") + "%",
                                                icon =
                                                    if (config.optBoolean("reachable")) {
                                                        R.drawable.ic_device_raspberry_pi
                                                    } else {
                                                        R.drawable.ic_warning
                                                    },
                                            ),
                                        )
                                    }
                                }
                                items.add(
                                    SimpleListItem(summary = resources.getString(R.string.hue_controls)),
                                )
                                sensorItems.sortBy { it.title }
                                items.addAll(sensorItems)

                                queue.add(
                                    JsonObjectRequest(
                                        Request.Method.GET,
                                        "$addressPrefix/lights",
                                        null,
                                        { innerInnerResponse ->
                                            val lightItems = arrayListOf<SimpleListItem>()
                                            for (i in innerInnerResponse.keys()) {
                                                val current =
                                                    innerInnerResponse.optJSONObject(i)
                                                        ?: JSONObject()
                                                val state =
                                                    current.optJSONObject("state") ?: JSONObject()
                                                lightItems.add(
                                                    SimpleListItem(
                                                        current.optString("name"),
                                                        (
                                                            if (state.optBoolean("on")) {
                                                                resources.getString(
                                                                    R.string.str_on,
                                                                )
                                                            } else {
                                                                resources.getString(R.string.str_off)
                                                            }
                                                        ) +
                                                            " · " +
                                                            current.optString("productname"),
                                                        icon =
                                                            if (state.optBoolean("reachable")) {
                                                                R.drawable.ic_device_lamp
                                                            } else {
                                                                R.drawable.ic_warning
                                                            },
                                                    ),
                                                )
                                            }
                                            items.add(
                                                SimpleListItem(summary = resources.getString(R.string.hue_lights)),
                                            )
                                            lightItems.sortBy { it.title }
                                            items.addAll(lightItems)
                                            recyclerView.adapter = SimpleListAdapter(items, this)
                                        },
                                        { },
                                    ),
                                )
                            },
                            { },
                        ),
                    )
                },
                { },
            ),
        )
    }

    private fun showShelly2Info(
        device: DeviceItem,
        queue: RequestQueue,
        items: ArrayList<SimpleListItem>,
        recyclerView: RecyclerView,
    ) {
        queue.add(
            JsonObjectRequest(
                Request.Method.GET,
                device.address + "rpc/Shelly.GetStatus",
                null,
                { response ->

                    items.addAll(
                        arrayOf(
                            SimpleListItem(summary = resources.getString(R.string.device_config_info_status)),
                            SimpleListItem(
                                (response.optJSONObject("wifi") ?: JSONObject()).run {
                                    optString("ssid") + " (" + rssiToPercent(optInt("rssi")) + " %)"
                                },
                                resources.getString(R.string.shelly_wifi),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                boolToString(
                                    (
                                        response.optJSONObject("mqtt")
                                            ?: JSONObject()
                                    ).optBoolean("connected"),
                                ),
                                resources.getString(R.string.shelly_mqtt),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                boolToString(
                                    (
                                        response.optJSONObject("cloud")
                                            ?: JSONObject()
                                    ).optBoolean("connected"),
                                ),
                                resources.getString(R.string.shelly_cloud),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                (
                                    response.optJSONObject("sys")
                                        ?: JSONObject()
                                ).optLong("uptime").run {
                                    String.format(
                                        Locale.getDefault(),
                                        "%02d:%02d:%02d",
                                        TimeUnit.SECONDS.toHours(this),
                                        TimeUnit.SECONDS.toMinutes(this) -
                                            TimeUnit.HOURS.toMinutes(
                                                TimeUnit.SECONDS.toHours(
                                                    this,
                                                ),
                                            ),
                                        TimeUnit.SECONDS.toSeconds(this) -
                                            TimeUnit.MINUTES.toSeconds(
                                                TimeUnit.SECONDS.toMinutes(
                                                    this,
                                                ),
                                            ),
                                    )
                                },
                                resources.getString(R.string.shelly_uptime),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                (response.optJSONObject("sys") ?: JSONObject()).run {
                                    "${(optInt("fs_free") / optInt("fs_size").toFloat() * TO_PERCENT).toInt()} %"
                                },
                                resources.getString(R.string.shelly_storage),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                (response.optJSONObject("sys") ?: JSONObject()).run {
                                    "${(optInt("ram_free") / optInt("ram_size").toFloat() * TO_PERCENT).toInt()} %"
                                },
                                resources.getString(R.string.shelly_ram),
                                icon = R.drawable.ic_about_info,
                            ),
                            SimpleListItem(
                                resources.getString(
                                    if ((
                                            (
                                                response.optJSONObject("sys")
                                                    ?: JSONObject()
                                            ).optJSONObject("available_updates")
                                                ?: JSONObject()
                                        ).has("stable")
                                    ) {
                                        R.string.str_yes
                                    } else {
                                        R.string.str_no
                                    },
                                ),
                                resources.getString(R.string.shelly_update),
                                icon = R.drawable.ic_about_info,
                            ),
                        ),
                    )
                    recyclerView.adapter = SimpleListAdapter(items, this)
                },
                { },
            ),
        )
    }
}
