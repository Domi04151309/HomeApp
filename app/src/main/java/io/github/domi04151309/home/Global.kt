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

    fun getIcon(icon: String): Int {
        return when (icon) {
            "Lamp" -> R.drawable.ic_device_lamp
            "Laptop" -> R.drawable.ic_device_laptop
            "Phone" -> R.drawable.ic_device_phone
            "Raspberry Pi" -> R.drawable.ic_device_raspberry_pi
            "Router" -> R.drawable.ic_device_router
            "Speaker" -> R.drawable.ic_device_speaker
            "Stack" -> R.drawable.ic_device_stack
            "Tablet" -> R.drawable.ic_device_tablet
            "Thermometer" -> R.drawable.ic_device_thermometer
            "TV" -> R.drawable.ic_device_tv
            else -> {
                R.drawable.ic_warning
            }
        }
    }

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
