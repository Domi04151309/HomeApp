package io.github.domi04151309.home.api

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class EspEasyAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    override fun loadList(callback: CallbackInterface) {
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url + "json", null,
            { infoResponse ->
                callback.onItemsLoaded(
                    UnifiedRequestCallback(
                        EspEasyAPIParser(c.resources).parseResponse(infoResponse),
                        deviceId
                    ),
                    recyclerViewInterface
                )
            },
            { error ->
                callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId,
                    Global.volleyError(c, error)
                ), null)
            }
        )
        queue.add(jsonObjectRequest)
    }

    override fun changeSwitchState(id: String, state: Boolean) {
        val switchUrl = url + "control?cmd=GPIO," + id + "," + (if (state) "1" else "0")
        val jsonObjectRequest = JsonObjectRequest(
            switchUrl,
            { },
            { e -> Log.e(Global.LOG_TAG, e.toString()) }
        )
        queue.add(jsonObjectRequest)
    }
}
