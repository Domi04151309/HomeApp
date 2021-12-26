package io.github.domi04151309.home.api

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.custom.JsonObjectRequestAuth
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.DeviceSecrets
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class ShellyAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?,
    private val version: Int
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    private val secrets = DeviceSecrets(c, deviceId)
    private val parser = ShellyAPIParser(c.resources, version)

    override fun loadList(callback: CallbackInterface) {
        val jsonObjectRequest = when (version) {
            1 -> JsonObjectRequestAuth(
                Request.Method.GET, url + "settings", secrets, null,
                { settingsResponse ->
                    queue.add(JsonObjectRequest(
                        Request.Method.GET, url + "status", null,
                        { statusResponse ->
                            callback.onItemsLoaded(
                                UnifiedRequestCallback(
                                    parser.parseResponse(settingsResponse, statusResponse),
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
                    ))
                },
                { error ->
                    callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId,
                        Global.volleyError(c, error)
                    ), null)
                }
            )
            2 -> JsonObjectRequest(
                Request.Method.GET, url + "rpc/Shelly.GetConfig", null,
                { configResponse ->
                    queue.add(JsonObjectRequest(
                        Request.Method.GET, url + "rpc/Shelly.GetStatus", null,
                        { statusResponse ->
                            callback.onItemsLoaded(
                                UnifiedRequestCallback(
                                    parser.parseResponse(configResponse, statusResponse),
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
                    ))
                },
                { error ->
                    callback.onItemsLoaded(UnifiedRequestCallback(null, deviceId,
                        Global.volleyError(c, error)
                    ), null)
                }
            )
            else -> null
        }
        queue.add(jsonObjectRequest)
    }

    override fun changeSwitchState(id: String, state: Boolean) {
        val requestUrl = url + "relay/$id?turn=" + (if (state) "on" else "off")
        val jsonObjectRequest = when (version) {
            1 -> JsonObjectRequestAuth(
                Request.Method.GET, requestUrl, secrets, null,
                { },
                { e -> Log.e(Global.LOG_TAG, e.toString()) }
            )
            2 -> JsonObjectRequest(
                Request.Method.GET, requestUrl, null,
                { },
                { e -> Log.e(Global.LOG_TAG, e.toString()) }
            )
            else -> null
        }
        queue.add(jsonObjectRequest)
    }

    companion object {
        /**
         * Detect the name of the shelly device during discovery
         */
        fun loadName(url: String, version: Int, listener: Response.Listener<String>): JsonObjectRequest {
            return JsonObjectRequest(
                url + (if (version == 1) "settings" else "shelly"),
                { statusResponse ->
                    listener.onResponse(if (statusResponse.isNull("name")) "" else statusResponse.optString("name", ""))
                }, {}
            )
        }
    }
}