package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONArray
import org.json.JSONObject
import kotlin.collections.ArrayList

class OpenHABAPIParser(resources: Resources) : UnifiedAPI.Parser(resources) {

    override fun parseResponse(response: JSONObject): ArrayList<ListViewItem> {
        val listItems: ArrayList<ListViewItem> = ArrayList(response.length())
        val items = response.getJSONArray("items")
        var currentObject: JSONObject
        var tags: JSONArray
        for (i in 0 until items.length()) {
            currentObject = items.getJSONObject(i)
            tags = currentObject.getJSONArray("tags")
            if (
                currentObject.optString("type") == "Group"
                && currentObject.getJSONArray("groupNames").length() == 0
                && tags.length() > 0
                && !tags.includes("Equipment")
                && currentObject.optString("label").isNotEmpty()
            ) listItems += ListViewItem(
                title = currentObject.optString("label"),
                summary = currentObject.optString("tags"),
                hidden = currentObject.optString("name") + '@' + currentObject.optString("tags"),
                icon = R.drawable.ic_zone,
                state = null
            )
        }

        listItems.sortBy { it.hidden.substring(it.hidden.indexOf('@')) }
        return listItems
    }

    override fun parseStates(response: JSONObject): ArrayList<Boolean?> {
        val states: ArrayList<Boolean?> = ArrayList(response.length())
        return states
    }

    private fun JSONArray.includes(item: String): Boolean {
        for (i in 0 until length()) {
            if (getString(i) == item) return true
        }
        return false
    }
}