package io.github.domi04151309.home.helpers

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
        for (relayId in 0 until relays.length()) {
            currentRelay = relays.getJSONObject(relayId)
            currentState = currentRelay.getBoolean("ison")
            currentName = currentRelay.optString("name", "")
            if (currentName.trim().isEmpty()) {
                currentName = resources.getString(R.string.shelly_switch_title, relayId.toInt() + 1)
            }
            listItems += ListViewItem(
                    title = currentName,
                    summary = resources.getString(
                            if (currentState) R.string.shelly_switch_summary_on
                            else R.string.shelly_switch_summary_off
                    ),
                    hidden = relayId.toString(),
                    state = currentState,
                    icon = R.drawable.ic_do
            )
        }

        //power meters
        val meters = status.optJSONArray("meters") ?: JSONArray()
        for (meterId in 0 until meters.length()) {
            var currentMeter = meters.getJSONObject(meterId)
            var currentWatts = currentMeter.getDouble("power")
            listItems += ListViewItem(
                    title = currentWatts.toString() + " W",
                    summary = resources.getString(R.string.shelly_powermeter_summary),
                    icon = R.drawable.ic_lightning
            )
        }

        //external temperature sensors
        val tempSensors = status.optJSONObject("ext_temperature")
        if (tempSensors != null) {
            val tempSensorKeys = tempSensors.names()
            for (sensorId in 0 until tempSensorKeys.length()) {
                val currentSensor = tempSensors.getJSONObject(sensorId.toString())
                val currentValue = currentSensor.getDouble("tC")
                listItems += ListViewItem(
                        title = currentValue.toString() + "°C",
                        summary = resources.getString(R.string.shelly_temperature_sensor_summary),
                        icon = R.drawable.ic_device_thermometer
                )
            }
        }

        //external humidity sensors
        val humSensors = status.optJSONObject("ext_humidity")
        if (humSensors != null) {
            val humSensorKeys = humSensors.names()
            for (sensorId in 0 until humSensorKeys.length()) {
                val currentSensor = humSensors.getJSONObject(sensorId.toString())
                val currentValue = currentSensor.getDouble("hum")
                listItems += ListViewItem(
                        title = currentValue.toString() + "%",
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
                            if (currentState) R.string.shelly_switch_summary_on
                            else R.string.shelly_switch_summary_off
                    ),
                    hidden = currentId.toString(),
                    state = currentState,
                    icon = R.drawable.ic_do
            )
        }

        return listItems
    }
}