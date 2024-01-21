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
    private val version: Int,
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {
    private val secrets = DeviceSecrets(c, deviceId)
    private val parser = ShellyAPIParser(c.resources, version)

    init {
        needsRealTimeData = true
    }

    override fun loadList(
        callback: CallbackInterface,
        extended: Boolean,
    ) {
        super.loadList(callback, extended)
        val jsonObjectRequest =
            when (version) {
                1 -> listRequestV1(callback)
                2 -> listRequestV2(callback)
                else -> null
            }
        queue.add(jsonObjectRequest)
    }

    private fun listRequestV1(callback: CallbackInterface) =
        JsonObjectRequestAuth(
            Request.Method.GET,
            url + SETTINGS,
            secrets,
            null,
            { settingsResponse ->
                queue.add(
                    JsonObjectRequestAuth(
                        Request.Method.GET,
                        url + "status",
                        secrets,
                        null,
                        { statusResponse ->
                            val listItems = parser.parseResponse(settingsResponse, statusResponse)
                            updateCache(listItems)
                            callback.onItemsLoaded(
                                UnifiedRequestCallback(
                                    listItems,
                                    deviceId,
                                ),
                                recyclerViewInterface,
                            )
                        },
                        { error ->
                            callback.onItemsLoaded(
                                UnifiedRequestCallback(
                                    null,
                                    deviceId,
                                    Global.volleyError(c, error),
                                ),
                                null,
                            )
                        },
                    ),
                )
            },
            { error ->
                callback.onItemsLoaded(
                    UnifiedRequestCallback(
                        null,
                        deviceId,
                        Global.volleyError(c, error),
                    ),
                    null,
                )
            },
        )

    private fun listRequestV2(callback: CallbackInterface) =
        JsonObjectRequest(
            Request.Method.GET,
            url + "rpc/Shelly.GetConfig",
            null,
            { configResponse ->
                queue.add(
                    JsonObjectRequest(
                        Request.Method.GET,
                        url + "rpc/Shelly.GetStatus",
                        null,
                        { statusResponse ->
                            val listItems = parser.parseResponse(configResponse, statusResponse)
                            updateCache(listItems)
                            callback.onItemsLoaded(
                                UnifiedRequestCallback(
                                    listItems,
                                    deviceId,
                                ),
                                recyclerViewInterface,
                            )
                        },
                        { error ->
                            callback.onItemsLoaded(
                                UnifiedRequestCallback(
                                    null,
                                    deviceId,
                                    Global.volleyError(c, error),
                                ),
                                null,
                            )
                        },
                    ),
                )
            },
            { error ->
                callback.onItemsLoaded(
                    UnifiedRequestCallback(
                        null,
                        deviceId,
                        Global.volleyError(c, error),
                    ),
                    null,
                )
            },
        )

    override fun loadStates(
        callback: RealTimeStatesCallback,
        offset: Int,
    ) {
        val jsonObjectRequest =
            when (version) {
                1 ->
                    JsonObjectRequestAuth(
                        Request.Method.GET,
                        url + SETTINGS,
                        secrets,
                        null,
                        { settingsResponse ->
                            queue.add(
                                JsonObjectRequestAuth(
                                    Request.Method.GET,
                                    url + "status",
                                    secrets,
                                    null,
                                    { statusResponse ->
                                        callback.onStatesLoaded(
                                            parser.parseStates(settingsResponse, statusResponse),
                                            offset,
                                            dynamicSummaries,
                                        )
                                    },
                                    { },
                                ),
                            )
                        },
                        { },
                    )
                2 ->
                    JsonObjectRequest(
                        Request.Method.GET,
                        url + "rpc/Shelly.GetConfig",
                        null,
                        { configResponse ->
                            queue.add(
                                JsonObjectRequest(
                                    Request.Method.GET,
                                    url + "rpc/Shelly.GetStatus",
                                    null,
                                    { statusResponse ->
                                        callback.onStatesLoaded(
                                            parser.parseStates(configResponse, statusResponse),
                                            offset,
                                            dynamicSummaries,
                                        )
                                    },
                                    { },
                                ),
                            )
                        },
                        { },
                    )
                else -> null
            }
        queue.add(jsonObjectRequest)
    }

    override fun changeSwitchState(
        id: String,
        state: Boolean,
    ) {
        val requestUrl = url + "relay/$id?turn=" + if (state) "on" else "off"
        val jsonObjectRequest =
            when (version) {
                1 ->
                    JsonObjectRequestAuth(
                        Request.Method.GET,
                        requestUrl,
                        secrets,
                        null,
                        { },
                        { e -> Log.e(Global.LOG_TAG, e.toString()) },
                    )
                2 ->
                    JsonObjectRequest(
                        Request.Method.GET,
                        requestUrl,
                        null,
                        { },
                        { e -> Log.e(Global.LOG_TAG, e.toString()) },
                    )
                else -> null
            }
        queue.add(jsonObjectRequest)
    }

    companion object {
        private const val SETTINGS = "settings"

        /**
         * Detect the name of the shelly device during discovery.
         */
        fun loadName(
            url: String,
            version: Int,
            listener: Response.Listener<String>,
        ): JsonObjectRequest =
            JsonObjectRequest(
                url + if (version == 1) SETTINGS else "shelly",
                { statusResponse ->
                    listener.onResponse(if (statusResponse.isNull("name")) "" else statusResponse.optString("name"))
                },
                {},
            )
    }
}
