package io.github.domi04151309.home

import android.content.Context
import android.util.Log
import com.android.volley.ClientError
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.TimeoutError

object Global {

    const val LOG_TAG = "HomeApp"

    const val DEFAULT_JSON = "{\"devices\":{}}"

    fun volleyError(c: Context, error: java.lang.Exception): String {
        Log.w(LOG_TAG, error)
        return if(error is TimeoutError || error is NoConnectionError)
            c.resources.getString(R.string.main_device_unavailable)
        else if(error is ParseError)
            c.resources.getString(R.string.main_parse_error)
        else if(error is ClientError)
            c.resources.getString(R.string.main_client_error)
        else
            c.resources.getString(R.string.main_device_unavailable)
    }

}
