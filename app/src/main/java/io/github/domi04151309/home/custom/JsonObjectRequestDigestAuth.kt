package io.github.domi04151309.home.custom

import android.content.Context
import android.util.Log
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
import kotlin.random.Random

class JsonObjectRequestDigestAuth(
    url: String,
    private val secrets: DeviceSecrets,
    private val context: Context,
    private val listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) : JsonObjectRequest(Method.GET, url, null, listener, errorListener) {

    override fun parseNetworkError(volleyError: VolleyError): VolleyError? {
        return if (volleyError.networkResponse.statusCode == 401) {
            val authData = volleyError.networkResponse.headers?.get("WWW-Authenticate")
                ?: return super.parseNetworkError(volleyError)
            Log.wtf(Global.LOG_TAG, authData)

            // Get values from header
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
            val ha1 = hashToString(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(("admin:$realm:${secrets.password}").toByteArray())
            )
            val ha2 = hashToString(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(("dummy_method:dummy_uri").toByteArray())
            )
            val hash = hashToString(
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(("$ha1:$nonce:$nc:$cNonce:auth:$ha2").toByteArray())
            )

            // Repeat the request
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
                listener.onResponse(future.get())
                //TODO: This will likely still call the error listener
                null
            } catch (e: InterruptedException) {
                super.parseNetworkError(volleyError)
            } catch (e: ExecutionException) {
                super.parseNetworkError(volleyError)
            }
        } else {
            super.parseNetworkError(volleyError)
        }
    }

    private fun hashToString(hash: ByteArray) : String {
        return hash.joinToString("") { byte -> "%02x".format(byte) }
    }
}
