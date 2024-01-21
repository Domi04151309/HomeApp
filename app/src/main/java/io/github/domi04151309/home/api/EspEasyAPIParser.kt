package io.github.domi04151309.home.api

import android.content.res.Resources
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONArray
import org.json.JSONObject

class EspEasyAPIParser(resources: Resources, api: UnifiedAPI?) : UnifiedAPI.Parser(resources, api) {
    override fun parseResponse(response: JSONObject): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()

        // sensors
        val sensors = response.optJSONArray("Sensors") ?: JSONArray()
        for (sensorId in 0 until sensors.length()) {
            val currentSensor = sensors.getJSONObject(sensorId)
            if (currentSensor.optString("TaskEnabled", FALSE).equals(FALSE)) {
                continue
            }

            val type = currentSensor.optString("Type")
            if (type.startsWith("Environment")) {
                listItems.addAll(parseEnvironment(type, currentSensor))
            } else if (type.startsWith("Switch")) {
                listItems.addAll(parseSwitch(type, currentSensor))
            }
        }

        return listItems
    }

    private fun parseEnvironment(
        type: String,
        currentSensor: JSONObject,
    ): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()
        var taskIcons = intArrayOf()
        when (type) {
            "Environment - BMx280" -> {
                taskIcons += R.drawable.ic_device_thermometer
                taskIcons += R.drawable.ic_device_hygrometer
                taskIcons += R.drawable.ic_device_gauge
            }
            "Environment - DHT11/12/22  SONOFF2301/7021" -> {
                taskIcons += R.drawable.ic_device_thermometer
                taskIcons += R.drawable.ic_device_hygrometer
            }
            "Environment - DS18b20" -> {
                taskIcons += R.drawable.ic_device_thermometer
            }
        }

        val taskName = currentSensor.getString("TaskName")
        for (taskId in taskIcons.indices) {
            val currentTask = currentSensor.getJSONArray(TASK_VALUES).getJSONObject(taskId)
            val currentValue = currentTask.getString(VALUE)
            if (!currentValue.equals("nan")) {
                val suffix =
                    when (taskIcons[taskId]) {
                        R.drawable.ic_device_thermometer -> " °C"
                        R.drawable.ic_device_hygrometer -> " %"
                        R.drawable.ic_device_gauge -> " hPa"
                        else -> ""
                    }
                listItems +=
                    ListViewItem(
                        title = currentValue + suffix,
                        summary = taskName + ": " + currentTask.getString("Name"),
                        icon = taskIcons[taskId],
                    )
            }
        }
        return listItems
    }

    private fun parseSwitch(
        type: String,
        currentSensor: JSONObject,
    ): List<ListViewItem> {
        val listItems = mutableListOf<ListViewItem>()
        when (type) {
            "Switch input - Switch" -> {
                val currentState = currentSensor.getJSONArray(TASK_VALUES).getJSONObject(0).getInt(VALUE) > 0
                var taskName = currentSensor.getString("TaskName")
                var gpioId = ""
                val gpioFinder = Regex("~GPIO~([0-9]+)$")
                val matchResult = gpioFinder.find(taskName)
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    gpioId = matchResult.groupValues[1]
                    taskName = taskName.replace("~GPIO~$gpioId", "")
                }
                listItems +=
                    ListViewItem(
                        title = taskName,
                        summary =
                            resources.getString(
                                if (currentState) {
                                    R.string.switch_summary_on
                                } else {
                                    R.string.switch_summary_off
                                },
                            ),
                        hidden = gpioId,
                        state = currentState,
                        icon = R.drawable.ic_do,
                    )
                api?.needsRealTimeData = true
            }
        }
        return listItems
    }

    override fun parseStates(response: JSONObject): List<Boolean?> {
        val listItems = mutableListOf<Boolean?>()

        // sensors
        val sensors = response.optJSONArray("Sensors") ?: JSONArray()
        for (sensorId in 0 until sensors.length()) {
            val currentSensor = sensors.getJSONObject(sensorId)
            if (currentSensor.optString("TaskEnabled", FALSE).equals(FALSE)) {
                continue
            }

            val type = currentSensor.optString("Type")
            if (type.startsWith("Environment")) {
                listItems.addAll(parseEnvironmentStates(type, currentSensor))
            } else if (type.startsWith("Switch")) {
                listItems.addAll(parseSwitchStates(type, currentSensor))
            }
        }

        return listItems
    }

    private fun parseEnvironmentStates(
        type: String,
        currentSensor: JSONObject,
    ): List<Boolean?> {
        val listItems = mutableListOf<Boolean?>()
        var tasks = 0
        @Suppress("MagicNumber")
        when (type) {
            "Environment - BMx280" -> tasks += 3
            "Environment - DHT11/12/22  SONOFF2301/7021" -> tasks += 2
            "Environment - DS18b20" -> tasks++
        }

        for (taskId in 0 until tasks) {
            if (
                !currentSensor.getJSONArray(TASK_VALUES)
                    .getJSONObject(taskId)
                    .getString(VALUE)
                    .equals("nan")
            ) {
                listItems += null
            }
        }
        return listItems
    }

    private fun parseSwitchStates(
        type: String,
        currentSensor: JSONObject,
    ): List<Boolean?> {
        val listItems = mutableListOf<Boolean?>()
        when (type) {
            "Switch input - Switch" -> {
                listItems += currentSensor.getJSONArray(TASK_VALUES).getJSONObject(0).getInt(VALUE) > 0
            }
        }
        return listItems
    }

    companion object {
        private const val FALSE = "false"
        private const val TASK_VALUES = "TaskValues"
        private const val VALUE = "Value"
    }
}
