package io.github.domi04151309.home.custom

import android.content.Context
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.helpers.DeviceSecrets
import io.github.domi04151309.home.helpers.Global
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ExecutionException
import javax.xml.bind.DatatypeConverter
import kotlin.random.Random

class JsonObjectRequestDigestAuth(
    url: String,
    private val secrets: DeviceSecrets,
    private val context: Context,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(Method.GET, url, null, listener, errorListener) {

    override fun parseNetworkError(volleyError: VolleyError): VolleyError {
        return if (volleyError.networkResponse.statusCode == 401) {
            Log.wtf(Global.LOG_TAG, "Try catching the error")
            parseNetworkResponse(volleyError.networkResponse)
            VolleyError()
        } else {
            super.parseNetworkError(volleyError)
        }
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONObject> {
        if (response.statusCode == 401) {
            val authData = response.headers?.get("WWW-Authenticate") ?: return super.parseNetworkResponse(response)
            Log.wtf(Global.LOG_TAG, authData)

            //TODO: get values from header
            var realm = ""
            var nonce = ""
            val nc = 1
            val authParameters = authData.split(", ")
            authParameters.forEach {
                if (it.contains("realm")) {
                    realm = it.substring(it.indexOf('"') + 1, it.lastIndexOf('"'))
                    Log.wtf(Global.LOG_TAG, realm)
                } else if (it.contains("nonce")) {
                    nonce = it.substring(it.indexOf('"') + 1, it.lastIndexOf('"'))
                    Log.wtf(Global.LOG_TAG, nonce)
                }
            }

            val cNonce = Random.nextInt(1, Integer.MAX_VALUE)
            val ha1 = DatatypeConverter.printHexBinary(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(("admin:$realm:${secrets.password}").toByteArray())
            )
            val ha2 = DatatypeConverter.printHexBinary(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(("dummy_method:dummy_uri").toByteArray())
            )
            val hash = DatatypeConverter.printHexBinary(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(("$ha1:$nonce:$nc:$cNonce:auth:$ha2").toByteArray())
            )

            //TODO: repeat request
            val future = RequestFuture.newFuture<JSONObject>()
            val requestBody = JSONObject().put("auth", JSONObject()
                .put("realm", realm)
                .put("username", "admin")
                .put("nonce", nonce/*.toInt()*/)
                .put("cnonce", cNonce)
                .put("response", hash)
                .put("algorithm", "SHA-256")
            )
            Log.wtf(Global.LOG_TAG, requestBody.toString())
            Volley.newRequestQueue(context).add(JsonObjectRequest(Method.POST, url, requestBody, future, future))
            return try {
                Response.success(future.get(), null)
            } catch (e: InterruptedException) {
                Response.error(AuthFailureError())
            } catch (e: ExecutionException) {
                Response.error(AuthFailureError())
            }
        } else {
            Log.wtf(Global.LOG_TAG, "No authentication needed")
            return super.parseNetworkResponse(response)
        }
    }
}
