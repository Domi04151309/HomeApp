package io.github.domi04151309.home.custom

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class CustomJsonArrayRequest(
    method: Int,
    url: String,
    jsonRequest: JSONObject?,
    listener: Response.Listener<JSONArray>,
    errorListener: Response.ErrorListener,
) : JsonRequest<JSONArray>(method, url, jsonRequest?.toString(), listener, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse): Response<JSONArray> =
        try {
            val jsonString = String(response.data, Charset.forName(HttpHeaderParser.parseCharset(response.headers)))
            Response.success(
                JSONArray(jsonString),
                HttpHeaderParser.parseCacheHeaders(response),
            )
        } catch (e: UnsupportedEncodingException) {
            Response.error(ParseError(e))
        } catch (e: JSONException) {
            Response.error(ParseError(e))
        }
}
