package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Global
import org.json.JSONObject

class SimpleHomeAPIParser(resources: Resources, api: UnifiedAPI?) : UnifiedAPI.Parser(resources, api) {
    override fun parseResponse(response: JSONObject): List<ListViewItem> {
        val listItems: ArrayList<ListViewItem> = ArrayList(response.length())
        val commands = response.optJSONObject("commands") ?: return listItems
        var currentObject: JSONObject
        var currentMode: String
        for (i in commands.keys()) {
            currentObject = commands.getJSONObject(i)
            currentMode = currentObject.optString("mode", "action")
            listItems +=
                ListViewItem(
                    title = currentObject.optString("title"),
                    summary = currentObject.optString("summary"),
                    hidden = "$currentMode@$i",
                    icon = Global.getIcon(currentObject.optString("icon"), R.drawable.ic_do),
                    state = if (currentMode == SWITCH) currentObject.optBoolean("data", false) else null,
                )
            if (currentMode == SWITCH) api?.needsRealTimeData = true
        }
        return listItems
    }

    override fun parseStates(response: JSONObject): List<Boolean?> {
        val listItems: ArrayList<Boolean?> = ArrayList(response.length())
        val commands = response.optJSONObject("commands") ?: return listItems
        var currentObject: JSONObject
        for (i in commands.keys()) {
            currentObject = commands.getJSONObject(i)
            listItems +=
                if (currentObject.optString("mode", "action") == SWITCH) {
                    currentObject.optBoolean("data", false)
                } else {
                    null
                }
        }
        return listItems
    }

    companion object {
        private const val SWITCH = "switch"
    }
}
