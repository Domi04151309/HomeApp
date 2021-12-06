package io.github.domi04151309.home.helpers

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONArray
import org.json.JSONObject

class ShellyAPIParser(val url: String, val resources: Resources) {

    fun parseListItemsJsonV1(settings: JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()

        val relays = settings.optJSONArray("relays") ?: JSONArray()
        var currentRelay: JSONObject
        var currentState: Boolean
        var currentName: String
        for (relayId in 0 until relays.length()) {
            currentRelay = relays.getJSONObject(relayId)
            currentState = currentRelay.getBoolean("ison")
            currentName = currentRelay.optString("name", "")
            if (currentName.trim().isEmpty()) {
                currentName = resources.getString(R.string.shelly_switch_title, relayId.toInt() + 1)
            }
            listItems += ListViewItem(
                    title = currentName,
                    summary = resources.getString(
                            if (currentState) R.string.shelly_switch_summary_on
                            else R.string.shelly_switch_summary_off
                    ),
                    hidden = relayId.toString(),
                    state = currentState,
                    icon = R.drawable.ic_do
            )
        }
        return listItems
    }

    fun parseListItemsJsonV2(config: JSONObject, status: JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()

        var currentId: Int
        var currentState: Boolean
        var currentName: String
        for (switchKey in config.keys()) {
            if (!switchKey.startsWith("switch:")) {
                continue
            }
            val properties = config.getJSONObject(switchKey)
            currentId = properties.getInt("id")
            currentName = if (properties.isNull("name")) ""
                    else properties.getString("name")
            currentState = status.getJSONObject(switchKey).getBoolean("output")

            listItems += ListViewItem(
                    title = currentName,
                    summary = resources.getString(
                            if (currentState) R.string.shelly_switch_summary_on
                            else R.string.shelly_switch_summary_off
                    ),
                    hidden = currentId.toString(),
                    state = currentState,
                    icon = R.drawable.ic_do
            )
        }

        return listItems
    }
}