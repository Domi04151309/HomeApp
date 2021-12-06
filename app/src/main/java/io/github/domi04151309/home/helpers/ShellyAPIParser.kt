package io.github.domi04151309.home.helpers

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONArray
import org.json.JSONObject

class ShellyAPIParser(val url: String, val resources: Resources) {

    fun parseListItemsJsonV1(response: JSONObject): ArrayList<ListViewItem> {
        val relays = response.optJSONArray("relays") ?: JSONArray()
        var currentItem: JSONObject
        for (i in 0 until relays.length()) {
            currentItem = relays.getJSONObject(i)
            if (!currentItem.has("name") || currentItem.isNull("name"))
                currentItem.put("name", "")
        }
        return parseItems(relays.toJSONObject(JSONArray(IntArray(relays.length()) { it })))
    }

    private fun parseItems(response : JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()
        var currentState: Boolean
        var currentName: String
        for (i in response.keys()) {
            currentState = response.getJSONObject(i).getBoolean("ison")
            currentName = response.getJSONObject(i).optString("name", "")
            if (currentName.trim() == "") {
                currentName = resources.getString(R.string.shelly_switch_title, i.toInt() + 1)
            }
            listItems += ListViewItem(
                    title = currentName,
                    summary = resources.getString(
                            if (currentState) R.string.shelly_switch_summary_on
                            else R.string.shelly_switch_summary_off
                    ),
                    hidden = i,
                    state = currentState,
                    icon = R.drawable.ic_do
            )
        }
        return listItems
    }
}