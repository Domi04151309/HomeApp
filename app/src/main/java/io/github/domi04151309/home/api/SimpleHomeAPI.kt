package io.github.domi04151309.home.api

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.helpers.Global.volleyError
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class SimpleHomeAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    override fun loadList(callback: CallbackInterface) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + "commands", null,
            { response ->
                callback.onItemsLoaded(
                    UnifiedRequestCallback(
                        SimpleHomeAPIParser(c.resources).parseResponse(response),
                        deviceId
                    ),
                    recyclerViewInterface
                )
            },
            { error ->
                callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId, volleyError(c, error)), null)
            }
        )
        queue.add(jsonObjectRequest)
    }

    override fun execute(path: String, callback: CallbackInterface) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url + path, null,
            { response ->
                callback.onExecuted(
                    response.optString("toast", c.resources.getString(R.string.main_execution_completed)),
                    deviceId,
                    response.optBoolean("refresh", false)
                )
            },
            { error ->
                callback.onExecuted(volleyError(c, error))
            }
        )
        queue.add(jsonObjectRequest)
    }
}