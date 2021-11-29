package io.github.domi04151309.home.helpers

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.data.RequestCallbackObject

class ShellyAPI(context: Context, deviceId: String) {

    private val c = context
    private val selectedDevice = deviceId
    private var url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)

    interface RequestCallBack {
        fun onResponse(holder: RequestCallbackObject)
    }

    fun getBasicDeviceInfo(callback: RequestCallBack) {
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url + "shelly", null,
            { response ->
                callback.onResponse(RequestCallbackObject(c, response, selectedDevice))
            },
            { error ->
                callback.onResponse(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
            }
        )
        queue.add(jsonObjectRequest)
    }
}