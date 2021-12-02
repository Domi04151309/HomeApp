package io.github.domi04151309.home.custom

import android.util.Log
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import io.github.domi04151309.home.helpers.DeviceSecrets
import io.github.domi04151309.home.helpers.Global
import org.json.JSONObject
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

class JsonObjectRequestDigestAuth(
    method: Int,
    url: String,
    private val secrets: DeviceSecrets,
    jsonRequest: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(method, url, jsonRequest, listener, errorListener) {

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
        if (response.statusCode == 401) {
            val authData = response.headers?.get("WWW-Authenticate")
            Log.wtf(Global.LOG_TAG, authData ?: return super.parseNetworkResponse(response))

            //TODO: get values from header
            var realm = ""
            var nonce = ""
            val authParameters = authData.split(", ")
            authParameters.forEach {
                if (it.contains("realm")) {
                    realm = it.substring(it.indexOf('"'), it.lastIndexOf('"'))
                    Log.wtf(Global.LOG_TAG, it)
                } else if (it.contains("nonce")) {
                    nonce = it.substring(it.indexOf('"'), it.lastIndexOf('"'))
                    Log.wtf(Global.LOG_TAG, it)
                }
            }

            val hash = DatatypeConverter.printHexBinary(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(("admin:$realm:${secrets.password}").toByteArray())
            )
            Log.wtf(Global.LOG_TAG, hash)

            //TODO: repeat request
        }
        return super.parseNetworkResponse(response)
    }
}
