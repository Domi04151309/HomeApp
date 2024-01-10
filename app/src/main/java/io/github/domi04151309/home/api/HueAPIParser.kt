package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
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
                        Pair(i, currentObject.optJSONObject("state")?.optBoolean("any_on"))
                "Zone" ->
                    zones[currentObject.getString("name")] =
                        Pair(i, currentObject.optJSONObject("state")?.optBoolean("any_on"))
            }
        }
        for (i in rooms.keys) states.add(rooms[i]?.second)
        for (i in zones.keys) states.add(zones[i]?.second)
        return states
    }

    private fun parseGroupObj(
        pair: Pair<String, JSONObject>,
        isZone: Boolean,
    ): ListViewItem {
        return ListViewItem(
            title = pair.second.getString("name"),
            summary = resources.getString(R.string.hue_tap),
            hidden = pair.first,
            icon = if (isZone) R.drawable.ic_zone else R.drawable.ic_room,
            state = pair.second.optJSONObject("state")?.optBoolean("any_on"),
        )
    }
}
