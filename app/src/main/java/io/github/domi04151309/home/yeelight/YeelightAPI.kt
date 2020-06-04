package io.github.domi04151309.home.yeelight

import android.content.Context
import com.murielkamgang.network.SocketConfig
import com.murielkamgang.network.TCPMessenger
import io.github.domi04151309.home.helpers.Devices

class YeelightAPI (context: Context, deviceId: String) {

    companion object {
        private const val PORT = 55443
        private const val TIMEOUT = 3000
    }

    private val tcp: TCPMessenger
    private val c: Context
    private val selectedDevice: String
    private val url: String
    private var hueCache: Int = 0
    private var satCache: Int = 0

    init {
        tcp = TCPMessenger.getInstance(SocketConfig(PORT, TIMEOUT))
        c = context
        selectedDevice = deviceId
        url = Devices(c).getDeviceById(deviceId).address
    }

    fun turnOnLight() {
        putObject("{\"id\":1,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",400]}")
    }

    fun turnOffLight() {
        putObject("{\"id\":1,\"method\":\"set_power\",\"params\":[\"off\",\"smooth\",400]}")
    }

    fun changeBrightness(bri: Int) {
        putObject("{\"id\":1,\"method\":\"set_bright\",\"params\":[$bri,\"smooth\",400]}")
    }

    fun changeColorTemperature(ct: Int) {
        putObject("{\"id\":1,\"method\":\"set_ct_abx\",\"params\":[" + (ct + YeelightUtils.CT_SHIFT).toString() + ",\"smooth\",400]}")
    }

    fun changeHue(hue: Int) {
        hueCache = hue
        putObject("{\"id\":1,\"method\":\"set_hsv\",\"params\":[$hueCache,$satCache,\"smooth\",400]}")
    }

    fun changeSaturation(sat: Int) {
        satCache = sat
        putObject("{\"id\":1,\"method\":\"set_hsv\",\"params\":[$hueCache,$satCache,\"smooth\",400]}")
    }

    private fun putObject(obj: String) {
        tcp.sendCommand(TCPMessenger.Request(url, "$obj\\r\\n"), String::class.java, null)
    }
}