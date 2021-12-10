package io.github.domi04151309.home.helpers

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.custom.JsonObjectRequestAuth
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.data.RequestCallbackObject
import kotlin.collections.ArrayList

class EspEasyAPI(private val c: Context, deviceId: String) {

    private val selectedDevice = deviceId
    private val url = Devices(c).getDeviceById(deviceId).address
    private val queue = Volley.newRequestQueue(c)

    interface RequestCallBack {
        fun onInfoLoaded(holder: RequestCallbackObject<ArrayList<ListViewItem>>)
    }

    fun loadInfo(callback: RequestCallBack) {
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url + "json", null,
            { infoResponse ->
                val parser = EspEasyAPIParser(url, c.resources)
                callback.onInfoLoaded(RequestCallbackObject(
                    c,
                    parser.parseInfo(infoResponse),
                    selectedDevice
                ))
            },
            { error ->
                callback.onInfoLoaded(RequestCallbackObject(c, null, selectedDevice, Global.volleyError(c, error)))
            }
        )
        queue.add(jsonObjectRequest)
    }

    fun changeSwitchState(gpioId: Int, newState: Boolean) {
        val switchUrl = url + "control?cmd=GPIO," + gpioId + "," + (if (newState) "1" else "0")
        val jsonObjectRequest = JsonObjectRequest(
            switchUrl,
            { },
            { e -> Log.e(Global.LOG_TAG, e.toString()) }
        )
        queue.add(jsonObjectRequest)
    }
}
