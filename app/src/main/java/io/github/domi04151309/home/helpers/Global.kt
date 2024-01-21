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
import io.github.domi04151309.home.api.EspEasyAPI
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.api.ShellyAPI
import io.github.domi04151309.home.api.SimpleHomeAPI
import io.github.domi04151309.home.api.Tasmota
import io.github.domi04151309.home.api.UnifiedAPI
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

internal object Global {
    const val LOG_TAG: String = "HomeApp"

    const val DEFAULT_JSON: String = "{\"devices\":{}}"
    const val ESP_EASY = "ESP Easy"
    const val HUE_API = "Hue API"
    const val SHELLY_GEN_1 = "Shelly Gen 1"
    const val SHELLY_GEN_2 = "Shelly Gen 2"
    const val SIMPLE_HOME_API = "SimpleHome API"
    const val TASMOTA = "Tasmota"
    const val NODE_RED = "Node-RED"
    const val WEBSITE = "Website"
    const val FRITZ_AUTO_LOGIN = "Fritz! Auto-Login"
    val UNIFIED_MODES =
        arrayOf(
            ESP_EASY,
            HUE_API,
            SHELLY_GEN_1,
            SHELLY_GEN_2,
            SIMPLE_HOME_API,
            TASMOTA,
        )
    val POWER_MENU_MODES =
        arrayOf(
            ESP_EASY,
            HUE_API,
            SHELLY_GEN_1,
            SHELLY_GEN_2,
            SIMPLE_HOME_API,
            TASMOTA,
        )

    fun getCorrectAPI(
        context: Context,
        identifier: String,
        deviceId: String,
        recyclerViewInterface: HomeRecyclerViewHelperInterface? = null,
        tasmotaHelperInterface: HomeRecyclerViewHelperInterface? = null,
    ): UnifiedAPI =
        when (identifier) {
            ESP_EASY -> EspEasyAPI(context, deviceId, recyclerViewInterface)
            HUE_API -> HueAPI(context, deviceId, recyclerViewInterface)
            SIMPLE_HOME_API -> SimpleHomeAPI(context, deviceId, recyclerViewInterface)
            TASMOTA -> Tasmota(context, deviceId, tasmotaHelperInterface ?: recyclerViewInterface)
            SHELLY_GEN_1 -> ShellyAPI(context, deviceId, recyclerViewInterface, 1)
            SHELLY_GEN_2 -> ShellyAPI(context, deviceId, recyclerViewInterface, 2)
            else -> UnifiedAPI(context, deviceId, recyclerViewInterface)
        }

    @Suppress("CyclomaticComplexMethod")
    fun getIcon(
        icon: String,
        default: Int = R.drawable.ic_warning,
    ): Int =
        when (icon.lowercase()) {
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

    @RequiresApi(Build.VERSION_CODES.R)
    fun getDeviceType(icon: String): Int =
        when (icon.lowercase()) {
            "christmas tree", "electricity", "schwibbogen", "socket" -> DeviceTypes.TYPE_OUTLET
            "display", "display alt" -> DeviceTypes.TYPE_DISPLAY
            "gauge", "heating", "thermometer" -> DeviceTypes.TYPE_AC_HEATER
            "hygrometer" -> DeviceTypes.TYPE_HUMIDIFIER
            "lamp", "lights" -> DeviceTypes.TYPE_LIGHT
            "webcam" -> DeviceTypes.TYPE_CAMERA
            else -> DeviceTypes.TYPE_UNKNOWN
        }

    fun checkNetwork(context: Context): Boolean {
        if (
            !PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("safety_checks", true)
        ) {
            return true
        }

        val connectivityManager =
            context.getSystemService(
                AppCompatActivity.CONNECTIVITY_SERVICE,
            ) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return if (capabilities != null) {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else {
            true
        }
    }

    fun volleyError(
        c: Context,
        error: java.lang.Exception,
    ): String {
        Log.w(LOG_TAG, error)
        return when (error) {
            is TimeoutError, is NoConnectionError -> c.resources.getString(R.string.main_device_unavailable)
            is ParseError -> c.resources.getString(R.string.main_parse_error)
            is ClientError -> c.resources.getString(R.string.main_client_error)
            else -> c.resources.getString(R.string.main_device_unavailable)
        }
    }
}
