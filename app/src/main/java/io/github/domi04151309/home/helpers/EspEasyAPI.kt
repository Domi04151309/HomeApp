package io.github.domi04151309.home.helpers

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.data.UnifiedRequestCallback
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
                        EspEasyAPIParser(url, c.resources).parseInfo(infoResponse),
                        deviceId
                    ),
                    recyclerViewInterface
                )
            },
            { error ->
                callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId, Global.volleyError(c, error)), null)
            }
        )
        queue.add(jsonObjectRequest)
    }

    override fun changeSwitchState(id: String, newState: Boolean) {
        val switchUrl = url + "control?cmd=GPIO," + id + "," + (if (newState) "1" else "0")
        val jsonObjectRequest = JsonObjectRequest(
            switchUrl,
            { },
            { e -> Log.e(Global.LOG_TAG, e.toString()) }
        )
        queue.add(jsonObjectRequest)
    }
}
