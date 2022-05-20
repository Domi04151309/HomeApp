package io.github.domi04151309.home.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.SimpleListAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Theme
import io.github.domi04151309.home.interfaces.RecyclerViewHelperInterface
import org.json.JSONObject

class HueSettingsActivity : AppCompatActivity(), RecyclerViewHelperInterface {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_devices)

        val devices = Devices(this)
        val id = intent.getStringExtra("device") ?: ""
        if (!devices.idExists(id)) {
            finish()
            return
        }

        val device = devices.getDeviceById(id)
        val hueAPI = HueAPI(this, device.id)
        val addressPrefix = device.address + "api/" + hueAPI.getUsername()
        val queue = Volley.newRequestQueue(this)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val items = arrayListOf<SimpleListItem>()
        items.add(
            SimpleListItem(
                device.name,
                device.address,
                icon = device.iconId
            )
        )

        queue.add(
            JsonObjectRequest(
                Request.Method.GET, "$addressPrefix/config", null,
                { response ->
                    items.addAll(
                        arrayOf(
                            SimpleListItem(summary = resources.getString(R.string.hue_bridge)),
                            SimpleListItem(
                                response.optString("name"),
                                resources.getString(R.string.hue_bridge_name),
                                icon = R.drawable.ic_about_info
                            ),
                            SimpleListItem(
                                response.optString("modelid"),
                                resources.getString(R.string.hue_bridge_model),
                                icon = R.drawable.ic_about_info
                            ),
                            SimpleListItem(
                                response.optString("bridgeid"),
                                resources.getString(R.string.hue_bridge_id),
                                icon = R.drawable.ic_about_info
                            ),
                            SimpleListItem(
                                response.optString("swversion"),
                                resources.getString(R.string.hue_bridge_software),
                                icon = R.drawable.ic_about_info
                            ),
                            SimpleListItem(
                                response.optString("zigbeechannel"),
                                resources.getString(R.string.hue_bridge_zigbee),
                                icon = R.drawable.ic_about_info
                            ),
                            SimpleListItem(
                                response.optString("timezone"),
                                resources.getString(R.string.hue_bridge_time_zone),
                                icon = R.drawable.ic_about_info
                            )
                        )
                    )

                    queue.add(
                        JsonObjectRequest(
                            Request.Method.GET, "$addressPrefix/sensors", null,
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
                                                icon = if (config.optBoolean("reachable")) R.drawable.ic_device_raspberry_pi
                                                else R.drawable.ic_warning
                                            )
                                        )
                                    }
                                }
                                items.add(
                                    SimpleListItem(summary = resources.getString(R.string.hue_controls))
                                )
                                sensorItems.sortBy { it.title }
                                items.addAll(sensorItems)

                                queue.add(
                                    JsonObjectRequest(
                                        Request.Method.GET, "$addressPrefix/lights", null,
                                        { innerInnerResponse ->
                                            val lightItems = arrayListOf<SimpleListItem>()
                                            for (i in innerInnerResponse.keys()) {
                                                val current = innerInnerResponse.optJSONObject(i)
                                                    ?: JSONObject()
                                                val state =
                                                    current.optJSONObject("state") ?: JSONObject()
                                                lightItems.add(
                                                    SimpleListItem(
                                                        current.optString("name"),
                                                        (if (state.optBoolean("on")) resources.getString(
                                                            R.string.str_on
                                                        )
                                                        else resources.getString(R.string.str_off))
                                                                + " · "
                                                                + current.optString("productname"),
                                                        icon = if (state.optBoolean("reachable")) R.drawable.ic_device_lamp
                                                        else R.drawable.ic_warning
                                                    )
                                                )
                                            }
                                            items.add(
                                                SimpleListItem(summary = resources.getString(R.string.hue_lights))
                                            )
                                            lightItems.sortBy { it.title }
                                            items.addAll(lightItems)
                                            recyclerView.adapter = SimpleListAdapter(items, this)
                                        }, { }
                                    )
                                )
                            }, { }
                        )
                    )
                }, { }
            )
        )
    }

    override fun onItemClicked(view: View, position: Int) {}
}
