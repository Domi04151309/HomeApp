package io.github.domi04151309.home.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.service.controls.DeviceTypes
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.android.volley.ClientError
import com.android.volley.NoConnectionError
import com.android.volley.ParseError
import com.android.volley.TimeoutError
import io.github.domi04151309.home.R
import io.github.domi04151309.home.api.*
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

internal object Global {

    const val LOG_TAG: String = "HomeApp"

    const val DEFAULT_JSON: String = "{\"devices\":{}}"
    val UNIFIED_MODES = arrayOf(
        "ESP Easy", "Hue API", "Shelly Gen 1", "Shelly Gen 2", "SimpleHome API", "Tasmota"
    )
    val POWER_MENU_MODES = arrayOf(
        "ESP Easy", "Hue API", "Shelly Gen 1", "Shelly Gen 2", "SimpleHome API"
    )

    fun getCorrectAPI(
        context: Context,
        identifier: String,
        deviceId: String,
        recyclerViewInterface: HomeRecyclerViewHelperInterface? = null,
        tasmotaHelperInterface: HomeRecyclerViewHelperInterface? = null
    ): UnifiedAPI {
        return when (identifier) {
            "ESP Easy" -> EspEasyAPI(context, deviceId, recyclerViewInterface)
            "Hue API" -> HueAPI(context, deviceId, recyclerViewInterface)
            "SimpleHome API" -> SimpleHomeAPI(context, deviceId, recyclerViewInterface)
            "Tasmota" -> Tasmota(context, deviceId, tasmotaHelperInterface ?: recyclerViewInterface)
            "Shelly Gen 1" -> ShellyAPI(context, deviceId, recyclerViewInterface, 1)
            "Shelly Gen 2" -> ShellyAPI(context, deviceId, recyclerViewInterface, 2)
            else -> UnifiedAPI(context, deviceId, recyclerViewInterface)
        }
    }

    fun getIcon(icon: String, default: Int = R.drawable.ic_warning): Int {
        return when (icon.lowercase()) {
            "christmas tree" -> R.drawable.ic_device_christmas_tree
            "clock" -> R.drawable.ic_device_clock
            "display" -> R.drawable.ic_device_display
            "display alt" -> R.drawable.ic_device_display_alt
            "electricity" -> R.drawable.ic_device_electricity
            "entertainment" -> R.drawable.ic_device_speaker
            "gauge" -> R.drawable.ic_device_gauge
            "heating" -> R.drawable.ic_device_thermometer
            "hygrometer" -> R.drawable.ic_device_hygrometer
            "lamp" -> R.drawable.ic_device_lamp
            "lights" -> R.drawable.ic_device_lamp
            "raspberry pi" -> R.drawable.ic_device_raspberry_pi
            "router" -> R.drawable.ic_device_router
            "speaker" -> R.drawable.ic_device_speaker
            "schwibbogen" -> R.drawable.ic_device_schwibbogen
            "stack" -> R.drawable.ic_device_stack
            "socket" -> R.drawable.ic_device_socket
            "thermometer" -> R.drawable.ic_device_thermometer
            "webcam" -> R.drawable.ic_device_webcam
            else -> default
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getDeviceType(icon: String): Int {
        return when (icon.lowercase()) {
            "christmas tree", "electricity", "schwibbogen", "socket" -> DeviceTypes.TYPE_OUTLET
            "display", "display alt" -> DeviceTypes.TYPE_DISPLAY
            "gauge", "heating", "thermometer" -> DeviceTypes.TYPE_AC_HEATER
            "hygrometer" -> DeviceTypes.TYPE_HUMIDIFIER
            "lamp", "lights" -> DeviceTypes.TYPE_LIGHT
            "webcam" -> DeviceTypes.TYPE_CAMERA
            else -> DeviceTypes.TYPE_UNKNOWN
        }
    }

    fun checkNetwork(context: Context) : Boolean {
        if (
            !PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("safety_checks", true)
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
