package io.github.domi04151309.home.helpers

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONArray
import org.json.JSONObject

class EspEasyAPIParser(val url: String, val resources: Resources) {

    fun parseInfo(info: JSONObject): ArrayList<ListViewItem> {
        val listItems = arrayListOf<ListViewItem>()

        //sensors
        val sensors = info.optJSONArray("Sensors") ?: JSONArray()
        for (sensorId in 0 until sensors.length()) {
            val currentSensor = sensors.getJSONObject(sensorId)
            if (currentSensor.optString("TaskEnabled", "false").equals("false")) {
                continue
            }

            val type = currentSensor.optString("Type")
            if (type.startsWith("Environment")) {
                parseEnvironment(listItems, type, currentSensor)
            } else if (type.startsWith("Switch")) {
                parseSwitch(listItems, type, currentSensor)
            }
        }

        //web interface
        listItems += ListViewItem(
            title = resources.getString(R.string.espeasy_web_configuration_title),
            summary = resources.getString(R.string.espeasy_web_configuration_summary),
            hidden = url,
            icon = R.drawable.ic_nav_settings
        )

        return listItems
    }

    private fun parseEnvironment(listItems: ArrayList<ListViewItem>, type: String, currentSensor: JSONObject)  {
        var taskIcons = intArrayOf()
        when (type) {
            "Environment - BMx280" -> {
                taskIcons += R.drawable.ic_device_thermometer
                taskIcons += R.drawable.ic_humidity
                taskIcons += R.drawable.ic_info
            }
            "Environment - DHT11/12/22  SONOFF2301/7021" -> {
                taskIcons += R.drawable.ic_device_thermometer
                taskIcons += R.drawable.ic_humidity
            }
            "Environment - DS18b20" -> {
                taskIcons += R.drawable.ic_device_thermometer
            }
        }

        val taskName = currentSensor.getString("TaskName")
        for (taskId in 0 until taskIcons.size) {
            val currentTask = currentSensor.getJSONArray("TaskValues").getJSONObject(taskId)
            val currentValue = currentTask.getString("Value")
            if (!currentValue.equals("nan")) {
                val suffix = when (taskIcons[taskId]) {
                    R.drawable.ic_device_thermometer -> " °C"
                    R.drawable.ic_humidity -> " %"
                    R.drawable.ic_info -> " hPa"
                    else -> ""
                }
                listItems += ListViewItem(
                    title = currentValue + suffix,
                    summary = taskName + ": " + currentTask.getString("Name"),
                    icon = taskIcons[taskId]
                )
            }
        }
    }

    private fun parseSwitch(listItems: ArrayList<ListViewItem>, type: String, currentSensor: JSONObject) {
        when (type) {
            "Switch input - Switch" -> {
                val currentState = currentSensor.getJSONArray("TaskValues").getJSONObject(0).getInt("Value") > 0
                var taskName =currentSensor.getString("TaskName")
                var gpioId = ""
                val gpioFinder = Regex("~GPIO~([0-9]+)$")
                val matchResult = gpioFinder.find(taskName)
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    gpioId = matchResult.groupValues[1]
                    taskName = taskName.replace("~GPIO~" + gpioId, "")
                }
                listItems += ListViewItem(
                    title = taskName,
                    summary = resources.getString(
                        if (currentState) R.string.shelly_switch_summary_on
                        else R.string.shelly_switch_summary_off
                    ),
                    hidden = gpioId,
                    state = currentState,
                    icon = R.drawable.ic_do
                )
            }
        }
    }
}