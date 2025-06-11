package io.github.domi04151309.home.activities

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.LightStates
import io.github.domi04151309.home.helpers.HueLightListener
import io.github.domi04151309.home.helpers.HueUtils.MIN_COLOR_TEMPERATURE
import io.github.domi04151309.home.helpers.UpdateHandler
import io.github.domi04151309.home.interfaces.HueRoomInterface
import org.json.JSONArray

class ControlInfoActivityHueRoom : HueRoomInterface {
    override var lights: JSONArray?
    override var lampData: HueLightListener
    override var id: String
    override var device: DeviceItem
    override var addressPrefix: String
    override var canReceiveRequest: Boolean

    private var updateDataRequest: JsonObjectRequest? = null
    private var updateHandler: UpdateHandler = UpdateHandler()

    constructor(context: Context, device: DeviceItem, id: String) {
        val hueApi = HueAPI(context, device.id)
        val queue = Volley.newRequestQueue(context)

        this.lights = null
        this.lampData = HueLightListener()
        this.id = id
        this.device = device
        this.addressPrefix = device.address + "api/" + hueApi.getUsername()
        this.canReceiveRequest = false

        updateDataRequest = getUpdateRequest()
        updateHandler.setUpdateFunction {
            if (canReceiveRequest && hueApi.readyForRequest) {
                queue.add(updateDataRequest)
            }
        }

        onStart()
    }

    fun onStart() {
        canReceiveRequest = true
    }

    fun onStop() {
        canReceiveRequest = false
    }

    fun onDestroy() {
        updateHandler.stop()
    }

    override fun onColorChanged(color: Int) {
        // Do nothing.
    }

    private fun getUpdateRequest() =
        JsonObjectRequest(
            Request.Method.GET,
            "$addressPrefix/groups/$id",
            null,
            { response ->
                lights = response.getJSONArray("lights")
                val action = response.getJSONObject("action")
                val light = LightStates.Light()

                light.ct =
                    if (action.has("ct")) {
                        action.getInt("ct") - MIN_COLOR_TEMPERATURE
                    } else {
                        -1
                    }

                if (action.has("hue") && action.has("sat")) {
                    light.hue = action.getInt("hue")
                    light.sat = action.getInt("sat")
                } else {
                    light.hue = -1
                    light.sat = -1
                }

                light.on = response.getJSONObject("state").getBoolean("any_on")

                lampData.state = light
            },
            {
                canReceiveRequest = false
            },
        )
}
