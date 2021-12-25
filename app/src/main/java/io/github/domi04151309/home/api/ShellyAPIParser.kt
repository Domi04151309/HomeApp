package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONArray
import org.json.JSONObject

class ShellyAPIParser(val url: String, val resources: Resources) {

    fun parseListItemsJsonV1(settings: JSONObject, status: JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()

        //switches
        val relays = settings.optJSONArray("relays") ?: JSONArray()
        var currentRelay: JSONObject
        var currentState: Boolean
        var currentName: String
        var hideMeters = false
        for (relayId in 0 until relays.length()) {
            currentRelay = relays.getJSONObject(relayId)
            currentState = currentRelay.getBoolean("ison")
            currentName = if (currentRelay.isNull("name")) "" else currentRelay.optString("name", "")
            if (currentName.trim().isEmpty()) {
                currentName = resources.getString(R.string.shelly_switch_title, relayId + 1)
            }
            listItems += ListViewItem(
                title = currentName,
                summary = resources.getString(
                    if (currentState) R.string.switch_summary_on
                    else R.string.switch_summary_off
                ),
                hidden = relayId.toString(),
                state = currentState,
                icon = R.drawable.ic_do
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
                icon = R.drawable.ic_lightning
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
                    icon = R.drawable.ic_humidity
                )
            }
        }

        return listItems
    }

    fun parseListItemsJsonV2(config: JSONObject, status: JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()

        var currentId: Int
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

            listItems += ListViewItem(
                    title = currentName,
                    summary = resources.getString(
                            if (currentState) R.string.switch_summary_on
                            else R.string.switch_summary_off
                    ),
                    hidden = currentId.toString(),
                    state = currentState,
                    icon = R.drawable.ic_do
            )
        }

        return listItems
    }
}