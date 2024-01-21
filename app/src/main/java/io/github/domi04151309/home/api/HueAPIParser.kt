package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.SimpleListItem
import org.json.JSONObject
import java.util.TreeMap
import kotlin.collections.ArrayList

class HueAPIParser(resources: Resources) : UnifiedAPI.Parser(resources) {
    override fun parseResponse(response: JSONObject): List<ListViewItem> {
        val listItems: ArrayList<ListViewItem> = ArrayList(response.length())
        val rooms: TreeMap<String, Pair<String, JSONObject>> = TreeMap()
        val zones: TreeMap<String, Pair<String, JSONObject>> = TreeMap()
        var currentObject: JSONObject
        for (i in response.keys()) {
            currentObject = response.getJSONObject(i)
            when (currentObject.getString("type")) {
                "Room" -> rooms[currentObject.getString("name")] = Pair(i, currentObject)
                "Zone" -> zones[currentObject.getString("name")] = Pair(i, currentObject)
            }
        }
        for (i in rooms.keys) listItems.add(
            parseGroupObj(
                rooms[i] ?: error("Room $i does not exist."),
                false,
            ),
        )
        for (i in zones.keys) listItems.add(
            parseGroupObj(
                zones[i] ?: error("Zone $i does not exist."),
                true,
            ),
        )
        return listItems
    }

    override fun parseStates(response: JSONObject): List<Boolean?> {
        val states: ArrayList<Boolean?> = ArrayList(response.length())
        val rooms: TreeMap<String, Pair<String, Boolean?>> = TreeMap()
        val zones: TreeMap<String, Pair<String, Boolean?>> = TreeMap()
        var currentObject: JSONObject
        for (i in response.keys()) {
            currentObject = response.getJSONObject(i)
            when (currentObject.getString("type")) {
                "Room" ->
                    rooms[currentObject.getString("name")] =
                        Pair(i, currentObject.optJSONObject(STATE)?.optBoolean(ANY_ON))
                "Zone" ->
                    zones[currentObject.getString("name")] =
                        Pair(i, currentObject.optJSONObject(STATE)?.optBoolean(ANY_ON))
            }
        }
        for (i in rooms.keys) states.add(rooms[i]?.second)
        for (i in zones.keys) states.add(zones[i]?.second)
        return states
    }

    private fun parseGroupObj(
        pair: Pair<String, JSONObject>,
        isZone: Boolean,
    ): ListViewItem =
        ListViewItem(
            title = pair.second.getString("name"),
            summary = resources.getString(R.string.hue_tap),
            hidden = pair.first,
            icon = if (isZone) R.drawable.ic_zone else R.drawable.ic_room,
            state = pair.second.optJSONObject(STATE)?.optBoolean(ANY_ON),
        )

    companion object {
        private const val STATE = "state"
        private const val ANY_ON = "any_on"

        fun parseHueConfig(
            resources: Resources,
            response: JSONObject,
        ): List<SimpleListItem> =
            listOf(
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
            )

        fun parseHueSensors(
            resources: Resources,
            response: JSONObject,
        ): List<SimpleListItem> {
            val sensorItems = mutableListOf<SimpleListItem>()
            for (i in response.keys()) {
                val current = response.optJSONObject(i) ?: JSONObject()
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
            val items = mutableListOf(SimpleListItem(summary = resources.getString(R.string.hue_controls)))
            items.addAll(sensorItems.sortedBy { it.title })
            return items
        }

        fun parseHueLights(
            resources: Resources,
            response: JSONObject,
        ): List<SimpleListItem> {
            val lightItems = mutableListOf<SimpleListItem>()
            for (i in response.keys()) {
                val current =
                    response.optJSONObject(i)
                        ?: JSONObject()
                val state =
                    current.optJSONObject(STATE) ?: JSONObject()
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
            val items = mutableListOf(SimpleListItem(summary = resources.getString(R.string.hue_lights)))
            items.addAll(lightItems.sortedBy { it.title })
            return items
        }
    }
}
