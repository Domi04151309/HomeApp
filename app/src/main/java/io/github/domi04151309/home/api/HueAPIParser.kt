package io.github.domi04151309.home.api

import android.content.Context
import android.content.res.Resources
import android.text.format.DateFormat
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.helpers.HueUtils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TreeMap

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

    private fun parseGroupObj(
        pair: Pair<String, JSONObject>,
        isZone: Boolean,
    ): ListViewItem {
        val state = pair.second.optJSONObject(STATE)?.optBoolean(ANY_ON)
        val value =
            pair.second.optJSONObject(ACTION)?.optInt(
                BRI,
                HueUtils.MAX_BRIGHTNESS,
            ) ?: HueUtils.MAX_BRIGHTNESS
        return ListViewItem(
            title = pair.second.getString("name"),
            summary =
                resources.getString(R.string.hue_brightness) +
                    ": " + if (state == true) HueUtils.briToPercent(value) else "0 %",
            hidden = pair.first,
            icon = if (isZone) R.drawable.ic_zone else R.drawable.ic_room,
            state = state,
            percentage = (value / HueUtils.MAX_BRIGHTNESS.toFloat() * 100).toInt(),
        )
    }

    companion object {
        private const val CONFIG = "config"
        private const val STATE = "state"
        private const val ANY_ON = "any_on"

        private const val ACTION = "action"
        private const val BRI = "bri"

        private const val MODEL_ID = "modelid"
        private const val SW_VERSION = "swversion"

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
                    response.optString(MODEL_ID),
                    resources.getString(R.string.hue_bridge_model),
                    icon = R.drawable.ic_about_info,
                ),
                SimpleListItem(
                    response.optString("bridgeid"),
                    resources.getString(R.string.hue_bridge_id),
                    icon = R.drawable.ic_about_info,
                ),
                SimpleListItem(
                    response.optString(SW_VERSION),
                    resources.getString(R.string.hue_bridge_software),
                    icon = R.drawable.ic_about_info,
                ),
                SimpleListItem(
                    response.optString("zigbeechannel"),
                    resources.getString(R.string.hue_bridge_zigbee),
                    icon = R.drawable.ic_about_info,
                ),
                SimpleListItem(
                    resources.getString(
                        if (response.optBoolean("dhcp")) {
                            R.string.str_yes
                        } else {
                            R.string.str_no
                        },
                    ),
                    resources.getString(R.string.hue_bridge_dhcp),
                    icon = R.drawable.ic_about_info,
                ),
                SimpleListItem(
                    response.optString("timezone"),
                    resources.getString(R.string.hue_bridge_time_zone),
                    icon = R.drawable.ic_about_info,
                ),
            )

        fun parseHueUsers(
            context: Context,
            resources: Resources,
            response: JSONObject,
        ): List<SimpleListItem> {
            val whitelist = response.optJSONObject("whitelist") ?: JSONObject()
            val configItems = mutableListOf<SimpleListItem>()
            for (i in whitelist.keys()) {
                val current = whitelist.optJSONObject(i) ?: JSONObject()
                configItems.add(
                    SimpleListItem(
                        current.optString("name"),
                        parseLastUpdated(context, current.optString("last use date")),
                        icon = R.drawable.ic_about_contributor,
                    ),
                )
            }
            val items =
                mutableListOf(SimpleListItem(summary = resources.getString(R.string.hue_users)))
            items.addAll(configItems.sortedBy { it.title })
            return items
        }

        fun parseHueSensors(
            context: Context,
            resources: Resources,
            response: JSONObject,
        ): List<SimpleListItem> {
            val sensorItems = mutableListOf<SimpleListItem>()
            for (i in response.keys()) {
                val current = response.optJSONObject(i) ?: JSONObject()
                val config = current.optJSONObject(CONFIG) ?: JSONObject()
                val state = current.optJSONObject(STATE) ?: JSONObject()
                if (config.has("battery")) {
                    sensorItems.add(
                        SimpleListItem(
                            current.optString("name"),
                            current.optString("productname") + " · " +
                                current.optString(MODEL_ID) + " · " +
                                current.optString(SW_VERSION) + "\n" +
                                config.optString("battery") + " % · " +
                                parseLastUpdated(context, state.optString("lastupdated")),
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
            val items =
                mutableListOf(SimpleListItem(summary = resources.getString(R.string.hue_controls)))
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
                val config =
                    current.optJSONObject(CONFIG) ?: JSONObject()
                val startup =
                    config.optJSONObject("startup") ?: JSONObject()
                val state =
                    current.optJSONObject(STATE) ?: JSONObject()
                lightItems.add(
                    SimpleListItem(
                        current.optString("name"),
                        current.optString("productname") + " · " +
                            current.optString(MODEL_ID) + " · " +
                            current.optString(SW_VERSION) + "\n" +
                            config.optString("function") + " · " +
                            startup.optString("mode", "none"),
                        icon =
                            if (state.optBoolean("reachable")) {
                                R.drawable.ic_device_lamp
                            } else {
                                R.drawable.ic_warning
                            },
                    ),
                )
            }
            val items =
                mutableListOf(SimpleListItem(summary = resources.getString(R.string.hue_lights)))
            items.addAll(lightItems.sortedBy { it.title })
            return items
        }

        private fun parseLastUpdated(
            context: Context,
            lastUpdated: String,
        ): String {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(lastUpdated)
                return if (parsed == null) {
                    "never"
                } else {
                    DateFormat.getMediumDateFormat(context).format(parsed) + ", " +
                        DateFormat.getTimeFormat(context).format(parsed)
                }
            } catch (exception: java.text.ParseException) {
                return "never"
            }
        }
    }
}
