package io.github.domi04151309.home.custom

import android.util.Base64
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.helpers.DeviceSecrets
import org.json.JSONObject

class JsonObjectRequestAuth(
    method: Int,
    url: String,
    private val secrets: DeviceSecrets,
    jsonRequest: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener,
) : JsonObjectRequest(method, url, jsonRequest, listener, errorListener) {
    override fun getHeaders(): MutableMap<String, String> {
        val params = HashMap<String, String>()
        params["Authorization"] = "Basic " +
            Base64.encodeToString(
                "${secrets.username}:${secrets.password}".toByteArray(),
                Base64.NO_WRAP,
            )
        return params
    }
}
