package io.github.domi04151309.home.custom

import android.util.Base64
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import io.github.domi04151309.home.helpers.DeviceSecrets

class StringRequestAuth(
    method: Int,
    url: String,
    private val secrets: DeviceSecrets,
    listener: Response.Listener<String>,
    errorListener: Response.ErrorListener,
) : StringRequest(method, url, listener, errorListener) {
    override fun getHeaders(): MutableMap<String, String> {
        val params = HashMap<String, String>()
        if (secrets.username.isNotEmpty() || secrets.password.isNotEmpty()) {
            params["Authorization"] = "Basic " +
                Base64.encodeToString(
                    "${secrets.username}:${secrets.password}".toByteArray(),
                    Base64.NO_WRAP,
                )
        }
        return params
    }
}
