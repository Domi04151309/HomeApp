package io.github.domi04151309.home.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface
import org.json.JSONObject

class OpenHABAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    private val parser = OpenHABAPIParser(c.resources)
    init {
        dynamicSummaries = false
    }

    override fun loadList(callback: CallbackInterface) {
        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url + "rest/items", null,
            { response ->
                callback.onItemsLoaded(
                    UnifiedRequestCallback(
                        parser.parseResponse(JSONObject("{\"items\":${response}}")),
                        deviceId
                    ),
                    recyclerViewInterface
                )
            },
            { error ->
                callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId, Global.volleyError(c, error)), null)
            }
        )
        queue.add(jsonArrayRequest)
    }

    override fun loadStates(callback: RealTimeStatesCallback, offset: Int) {
        //TODO: implement
        callback.onStatesLoaded(
            arrayListOf(null),
            offset,
            dynamicSummaries
        )
    }

    override fun execute(path: String, callback: CallbackInterface) {
        //TODO: implement
        callback.onExecuted(
            "Not yet implemented!",
            false
        )
    }

    override fun changeSwitchState(id: String, state: Boolean) {
        //TODO: implement
    }
}