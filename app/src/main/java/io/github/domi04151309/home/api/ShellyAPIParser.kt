package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Global
import org.json.JSONArray
import org.json.JSONObject

class ShellyAPIParser(resources: Resources, private val version: Int) :
    UnifiedAPI.Parser(resources) {
    fun parseResponse(
        config: JSONObject,
        status: JSONObject,
    ): List<ListViewItem> =
        if (version == 1) {
            parseResponseV1(config, status)
        } else {
            parseResponseV2(config, status)
        }

    private fun parseResponseV1(
        settings: JSONObject,
        status: JSONObject,
    ): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()
        listItems.addAll(parseSwitchesAndMetersV1(settings, status))
        listItems.addAll(parseTemperatureSensorsV1(status))
        listItems.addAll(parseHumiditySensorsV1(status))
        return listItems
    }

    private fun parseSwitchesAndMetersV1(
        settings: JSONObject,
        status: JSONObject,
    ): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()

        // switches
        val relays = settings.optJSONArray("relays") ?: JSONArray()
        var currentRelay: JSONObject
        var currentState: Boolean
        var hideMeters = false
        for (relayId in 0 until relays.length()) {
            currentRelay = relays.getJSONObject(relayId)
            currentState = currentRelay.getBoolean("ison")

            listItems +=
                ListViewItem(
                    title =
                        nameOrDefault(
                            if (currentRelay.isNull("name")) "" else currentRelay.optString("name"),
                            relayId,
                        ),
                    summary =
                        resources.getString(
                            if (currentState) {
                                R.string.switch_summary_on
                            } else {
                                R.string.switch_summary_off
                            },
                        ),
                    hidden = relayId.toString(),
                    state = currentState,
                    icon = Global.getIcon(currentRelay.optString("appliance_type"), R.drawable.ic_do),
                )
            // Shelly1 has the "user power constant" setting, but no actual meter
            hideMeters = currentRelay.has("power")
        }

        // power meters
        val meters = if (hideMeters) JSONArray() else status.optJSONArray("meters") ?: JSONArray()
        var currentMeter: JSONObject
        for (meterId in 0 until meters.length()) {
            currentMeter = meters.getJSONObject(meterId)
            listItems +=
                ListViewItem(
                    title = "${currentMeter.getDouble("power")} W",
                    summary = resources.getString(R.string.shelly_powermeter_summary),
                    icon = R.drawable.ic_device_electricity,
                )
        }

        return listItems
    }

    private fun parseTemperatureSensorsV1(status: JSONObject): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()
        val tempSensors = status.optJSONObject("ext_temperature") ?: JSONObject()
        for (sensorId in tempSensors.keys()) {
            val currentSensor = tempSensors.getJSONObject(sensorId)
            listItems +=
                ListViewItem(
                    title = "${currentSensor.getDouble("tC")} °C",
                    summary = resources.getString(R.string.shelly_temperature_sensor_summary),
                    icon = R.drawable.ic_device_thermometer,
                )
        }
        return listItems
    }

    private fun parseHumiditySensorsV1(status: JSONObject): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()
        val humSensors = status.optJSONObject("ext_humidity") ?: JSONObject()
        for (sensorId in humSensors.keys()) {
            val currentSensor = humSensors.getJSONObject(sensorId)
            listItems +=
                ListViewItem(
                    title = "${currentSensor.getDouble("hum")}%",
                    summary = resources.getString(R.string.shelly_humidity_sensor_summary),
                    icon = R.drawable.ic_device_hygrometer,
                )
        }
        return listItems
    }

    private fun parseResponseV2(
        config: JSONObject,
        status: JSONObject,
    ): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()
        var currentId: Int
        var currentState: Boolean
        for (switchKey in config.keys()) {
            if (!switchKey.startsWith("switch:")) continue
            val properties = config.getJSONObject(switchKey)
            currentId = properties.getInt("id")
            currentState = status.getJSONObject(switchKey).getBoolean("output")

            listItems +=
                ListViewItem(
                    title =
                        nameOrDefault(
                            if (properties.isNull("name")) "" else properties.getString("name"),
                            currentId,
                        ),
                    summary =
                        resources.getString(
                            if (currentState) {
                                R.string.switch_summary_on
                            } else {
                                R.string.switch_summary_off
                            },
                        ),
                    hidden = currentId.toString(),
                    state = currentState,
                    icon =
                        Global.getIcon(
                            config.optJSONObject("sys")?.optJSONObject("ui_data")
                                ?.optJSONArray("consumption_types")
                                ?.getString(currentId)
                                ?: "",
                            R.drawable.ic_do,
                        ),
                )
        }
        return listItems
    }

    fun parseStates(
        config: JSONObject,
        status: JSONObject,
    ): List<Boolean?> = if (version == 1) parseStatesV1(config, status) else parseStatesV2(config, status)

    private fun parseStatesV1(
        settings: JSONObject,
        status: JSONObject,
    ): List<Boolean?> {
        val listItems = mutableListOf<Boolean?>()

        // switches
        val relays = settings.optJSONArray("relays") ?: JSONArray()
        var currentRelay: JSONObject
        var hideMeters = false
        for (relayId in 0 until relays.length()) {
            currentRelay = relays.getJSONObject(relayId)
            listItems += currentRelay.getBoolean("ison")
            // Shelly1 has the "user power constant" setting, but no actual meter
            hideMeters = currentRelay.has("power")
        }

        // power meters
        val meters = if (hideMeters) JSONArray() else status.optJSONArray("meters") ?: JSONArray()
        for (meterId in 0 until meters.length()) {
            listItems += null
        }

        // external temperature sensors
        val tempSensors = status.optJSONObject("ext_temperature") ?: JSONObject()
        for (sensorId in tempSensors.keys()) {
            listItems += null
        }

        // external humidity sensors
        val humSensors = status.optJSONObject("ext_humidity") ?: JSONObject()
        for (sensorId in humSensors.keys()) {
            listItems += null
        }

        return listItems
    }

    private fun parseStatesV2(
        config: JSONObject,
        status: JSONObject,
    ): List<Boolean?> {
        val listItems = mutableListOf<Boolean?>()
        for (switchKey in config.keys()) {
            if (!switchKey.startsWith("switch:")) continue
            listItems += status.getJSONObject(switchKey).getBoolean("output")
        }
        return listItems
    }

    private fun nameOrDefault(
        name: String,
        id: Int,
    ): String =
        if (name.trim().isEmpty()) {
            resources.getString(R.string.shelly_switch_title, id + 1)
        } else {
            name
        }
}
