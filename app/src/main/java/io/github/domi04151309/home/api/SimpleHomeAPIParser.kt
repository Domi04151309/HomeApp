package io.github.domi04151309.home.api

import android.content.res.Resources
import android.util.Log
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Global
import org.json.JSONException
import org.json.JSONObject

class SimpleHomeAPIParser(resources: Resources) : UnifiedAPI.Parser(resources) {

    override fun parseResponse(response: JSONObject): ArrayList<ListViewItem> {
        val listItems: ArrayList<ListViewItem> = ArrayList(response.length())
        val commands = response.getJSONObject("commands")
        var currentObject: JSONObject
        for (i in commands.keys()) {
            try {
                currentObject = commands.getJSONObject(i)
                listItems += ListViewItem(
                    title = currentObject.getString("title"),
                    summary = currentObject.getString("summary"),
                    hidden = i,
                    icon = R.drawable.ic_do
                )
            } catch (e: JSONException) {
                Log.e(Global.LOG_TAG, e.toString())
            }
        }
        return listItems
    }
}