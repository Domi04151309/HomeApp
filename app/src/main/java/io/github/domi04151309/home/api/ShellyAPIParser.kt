package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.Global
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

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
                                R.string.str_on
                            } else {
                                R.string.str_off
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
        for (switchKey in config.keys()) {
            val switchKeyTrim = switchKey.split(":", limit = 2)[0]
            when(switchKeyTrim) {
                "switch", "pm1" -> {
                    listItems.addAll(parseV2(switchKey, config, status))
                }
                else -> {}
            }
        }
        return listItems
    }

    private fun parseV2(
        deviceKey: String,
        config: JSONObject,
        status: JSONObject,
    ): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()
        val format = DecimalFormat("#.###")
        val deviceConfig = config.getJSONObject(deviceKey)
        val deviceStatus = status.getJSONObject(deviceKey)
        val currentId = deviceConfig.getInt("id")

        for (statusKey in deviceStatus.keys()) {
            when(statusKey){
                // Switch/Light
                "output" -> {
                    val currentState = deviceStatus.getBoolean("output")
                    listItems +=
                        ListViewItem(
                            title =
                                nameOrDefault(
                                    if (deviceConfig.isNull("name")) "" else deviceConfig.getString("name"),
                                    currentId,
                                ),
                            summary =
                                resources.getString(
                                    if (currentState) {
                                        R.string.str_on
                                    } else {
                                        R.string.str_off
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
                // Power meter
                "apower" -> {
                    listItems +=
                        ListViewItem(
                            title = "${format.format(deviceStatus.getDouble("apower"))} W",
                            summary = resources.getString(R.string.shelly_powermeter_summary),
                            hidden = currentId.toString(),
                            icon = R.drawable.ic_device_electricity,
                        )
                }
                "current" -> {
                    listItems +=
                        ListViewItem(
                            title = "${format.format(deviceStatus.getDouble("current"))} A",
                            summary = resources.getString(R.string.shelly_powermeter_current),
                            hidden = currentId.toString() + "c",
                        )
                }
                "voltage" -> {
                    listItems +=
                        ListViewItem(
                            title = "${format.format(deviceStatus.getDouble("voltage"))} V",
                            summary = resources.getString(R.string.shelly_powermeter_voltage),
                            hidden = currentId.toString() + "v",
                        )
                }
                "aenergy" -> {
                    listItems +=
                        ListViewItem(
                            title = "${format.format(deviceStatus.getJSONObject("aenergy").getDouble("total") / KILO)} kWh",
                            summary = resources.getString(R.string.shelly_powermeter_energy),
                            hidden = currentId.toString() + "e",
                        )
                }
                "ret_aenergy" -> {
                    listItems +=
                        ListViewItem(
                            title = "${format.format(deviceStatus.getJSONObject("ret_aenergy").getDouble("total") / KILO)} kWh",
                            summary = resources.getString(R.string.shelly_powermeter_return_energy),
                            hidden = currentId.toString() + "rete",
                        )
                }
                else -> {}
            }
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

    companion object {
        private const val KILO = 1000
    }
}
