package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONObject
import kotlin.collections.ArrayList

class OpenHABAPIParser(resources: Resources) : UnifiedAPI.Parser(resources) {

    override fun parseResponse(response: JSONObject): ArrayList<ListViewItem> {
        val listItems: ArrayList<ListViewItem> = ArrayList(response.length())
        return listItems
    }

    override fun parseStates(response: JSONObject): ArrayList<Boolean?> {
        val states: ArrayList<Boolean?> = ArrayList(response.length())
        return states
    }
}