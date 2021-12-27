package io.github.domi04151309.home.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.android.volley.ClientError
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.TimeoutError
import io.github.domi04151309.home.R

internal object Global {

    const val LOG_TAG: String = "HomeApp"

    const val DEFAULT_JSON: String = "{\"devices\":{}}"

    fun getIcon(icon: String): Int {
        return when (icon) {
            "Display" -> R.drawable.ic_device_display
            "Electricity" -> R.drawable.ic_device_electricity
            "Gauge" -> R.drawable.ic_device_gauge
            "Hygrometer" -> R.drawable.ic_device_hygrometer
            "Lamp" -> R.drawable.ic_device_lamp
            "Raspberry Pi" -> R.drawable.ic_device_raspberry_pi
            "Router" -> R.drawable.ic_device_router
            "Speaker" -> R.drawable.ic_device_speaker
            "Stack" -> R.drawable.ic_device_stack
            "Thermometer" -> R.drawable.ic_device_thermometer
            else -> {
                R.drawable.ic_warning
            }
        }
    }

    fun checkNetwork(context: Context) : Boolean {
        if (
            !PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("local_only", true)
        ) return true

        val connectivityManager = context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return if (capabilities != null) {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else true
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
