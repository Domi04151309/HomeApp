package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONArray
import org.json.JSONObject

class ShellyAPIParser(resources: Resources, private val version: Int): UnifiedAPI.Parser(resources) {

    fun parseResponse(config: JSONObject, status: JSONObject): ArrayList<ListViewItem> {
        return if (version == 1) parseResponseV1(config, status) else parseResponseV2(config, status)
    }

    private fun parseResponseV1(settings: JSONObject, status: JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()

        //switches
        val relays = settings.optJSONArray("relays") ?: JSONArray()
        var currentRelay: JSONObject
        var currentState: Boolean
        var currentName: String
        var currentIcon: Int
        var hideMeters = false
        for (relayId in 0 until relays.length()) {
            currentRelay = relays.getJSONObject(relayId)
            currentState = currentRelay.getBoolean("ison")
            currentName = if (currentRelay.isNull("name")) "" else currentRelay.optString("name", "")
            if (currentName.trim().isEmpty()) {
                currentName = resources.getString(R.string.shelly_switch_title, relayId + 1)
            }
            currentIcon = this.iconFromApplianceType(currentRelay.optString("appliance_type"))
            listItems += ListViewItem(
                title = currentName,
                summary = resources.getString(
                    if (currentState) R.string.switch_summary_on
                    else R.string.switch_summary_off
                ),
                hidden = relayId.toString(),
                state = currentState,
                icon = currentIcon
            )
            //Shelly1 has the "user power constant" setting, but no actual meter
            hideMeters = currentRelay.has("power")
        }

        //power meters
        val meters = if (hideMeters) JSONArray() else status.optJSONArray("meters") ?: JSONArray()
        var currentMeter: JSONObject
        for (meterId in 0 until meters.length()) {
            currentMeter = meters.getJSONObject(meterId)
            listItems += ListViewItem(
                title = "${currentMeter.getDouble("power")} W",
                summary = resources.getString(R.string.shelly_powermeter_summary),
                icon = R.drawable.ic_device_electricity
            )
        }

        //external temperature sensors
        val tempSensors = status.optJSONObject("ext_temperature")
        if (tempSensors != null) {
            for (sensorId in tempSensors.keys()) {
                val currentSensor = tempSensors.getJSONObject(sensorId)
                listItems += ListViewItem(
                    title = "${currentSensor.getDouble("tC")} °C",
                    summary = resources.getString(R.string.shelly_temperature_sensor_summary),
                    icon = R.drawable.ic_device_thermometer
                )
            }
        }

        //external humidity sensors
        val humSensors = status.optJSONObject("ext_humidity")
        if (humSensors != null) {
            for (sensorId in humSensors.keys()) {
                val currentSensor = humSensors.getJSONObject(sensorId)
                listItems += ListViewItem(
                    title = "${currentSensor.getDouble("hum")}%",
                    summary = resources.getString(R.string.shelly_humidity_sensor_summary),
                    icon = R.drawable.ic_device_hygrometer
                )
            }
        }

        return listItems
    }

    private fun parseResponseV2(config: JSONObject, status: JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()

        var currentId: Int
        var currentIcon: Int
        var currentState: Boolean
        var currentName: String
        for (switchKey in config.keys()) {
            if (!switchKey.startsWith("switch:")) {
                continue
            }
            val properties = config.getJSONObject(switchKey)
            currentId = properties.getInt("id")
            currentName = if (properties.isNull("name")) ""
                    else properties.getString("name")
            currentState = status.getJSONObject(switchKey).getBoolean("output")
            currentIcon = this.iconFromApplianceType(config.optJSONObject("sys")?.optJSONObject("ui_data")?.optJSONArray("consumption_types")?.getString(currentId))

            listItems += ListViewItem(
                    title = currentName,
                    summary = resources.getString(
                            if (currentState) R.string.switch_summary_on
                            else R.string.switch_summary_off
                    ),
                    hidden = currentId.toString(),
                    state = currentState,
                    icon = currentIcon
            )
        }

        return listItems
    }

    private fun iconFromApplianceType(appliance_type: String?): Int {
        return when (appliance_type?.lowercase()) {
            "christmas tree" -> R.drawable.ic_device_christmas_tree
            "entertainment" -> R.drawable.ic_device_speaker
            "heating" -> R.drawable.ic_device_thermometer
            "lights" -> R.drawable.ic_device_lamp
            "schwibbogen" -> R.drawable.ic_device_schwibbogen
            "socket" -> R.drawable.ic_device_socket
            else -> R.drawable.ic_do
        }
    }
}