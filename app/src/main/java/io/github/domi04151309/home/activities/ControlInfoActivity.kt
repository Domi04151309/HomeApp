package io.github.domi04151309.home.activities

import android.os.Bundle
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.github.domi04151309.home.R
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.LightStates
import io.github.domi04151309.home.fragments.ControlInfoFragment
import io.github.domi04151309.home.fragments.HueColorFragment
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueLightListener
import io.github.domi04151309.home.helpers.HueUtils.MIN_COLOR_TEMPERATURE
import io.github.domi04151309.home.helpers.UpdateHandler
import io.github.domi04151309.home.interfaces.HueRoomInterface
import org.json.JSONArray

class ControlInfoActivity : BaseActivity(), HueRoomInterface {
    override var lights: JSONArray? = null
    override var lampData: HueLightListener = HueLightListener()

    override lateinit var id: String
    override lateinit var device: DeviceItem
    override lateinit var addressPrefix: String

    override var canReceiveRequest: Boolean = false

    private var updateDataRequest: JsonObjectRequest? = null
    private var updateHandler: UpdateHandler = UpdateHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val id = intent.getStringExtra(EXTRA_ID)
        if (id === null) {
            return
        }

        val device = Devices(this).getDeviceById(id.substring(0, id.indexOf('@')))

        if (device.mode == Global.HUE_API) {
            showHueFragment(id, device)
            return
        }

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.settings,
                ControlInfoFragment(
                    intent.getIntExtra(EXTRA_ICON, R.drawable.ic_warning),
                    intent.getStringExtra(EXTRA_TITLE) ?: "",
                    intent.getStringExtra(EXTRA_SUMMARY) ?: "",
                ),
            )
            .commit()
    }

    override fun onStart() {
        super.onStart()
        canReceiveRequest = true
    }

    override fun onStop() {
        super.onStop()
        canReceiveRequest = false
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHandler.stop()
    }

    override fun onColorChanged(color: Int) {
        // Do nothing.
    }

    private fun showHueFragment(
        id: String,
        device: DeviceItem,
    ) {
        val hueApi = HueAPI(this, device.id)
        val queue = Volley.newRequestQueue(this)

        this.id = id.substring(id.indexOf('@') + 1)
        this.device = device
        this.addressPrefix = device.address + "api/" + hueApi.getUsername()

        updateDataRequest = getUpdateRequest()
        updateHandler.setUpdateFunction {
            if (canReceiveRequest && hueApi.readyForRequest) {
                queue.add(updateDataRequest)
            }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, HueColorFragment())
            .commit()
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

    companion object {
        const val EXTRA_ID: String = "EXTRA_ID"
        const val EXTRA_ICON: String = "EXTRA_ICON"
        const val EXTRA_TITLE: String = "EXTRA_TITLE"
        const val EXTRA_SUMMARY: String = "EXTRA_SUMMARY"
    }
}
