package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.EspEasyAPIParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EspEasyAPIParserTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().applicationContext.resources
    private val parser = EspEasyAPIParser(resources, null)

    @Test
    fun parseInfo1() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-1.json"))

        val listItems = parser.parseResponse(infoJson)
        assertThat(listItems.size, `is`(4))

        // sensor 1: Temperature + Humidity
        var num = 0
        assertThat(listItems[num].title, `is`("24.7 °C"))
        assertThat(listItems[num].summary, `is`("DHT: Temperatur"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_thermometer))

        num++
        assertThat(listItems[num].title, `is`("39.5 %"))
        assertThat(listItems[num].summary, `is`("DHT: Feuchte"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_hygrometer))

        // sensor 2: Temperature only
        num++
        assertThat(listItems[num].title, `is`("0 °C"))
        assertThat(listItems[num].summary, `is`("DS: Temperatur"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_thermometer))

        // sensor 3: Switch in off mode
        num++
        assertThat(listItems[num].title, `is`("Relais"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.switch_summary_off)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("12"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))
    }

    @Test
    fun parseStates1() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-1.json"))

        val states = parser.parseStates(infoJson)
        assertThat(states, `is`(listOf(null, null, null, false)))
    }

    @Test
    fun parseInfoDisabledTasks() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-disabledtasks.json"))

        val listItems = parser.parseResponse(infoJson)
        assertThat(listItems.size, `is`(0))
    }

    @Test
    fun parseStatesDisabledTasks() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-disabledtasks.json"))

        val states = parser.parseStates(infoJson)
        assertThat(states, `is`(listOf()))
    }

    @Test
    fun parseInfoHideNanSensorValues() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-nan.json"))

        val listItems = parser.parseResponse(infoJson)

        assertThat(listItems.size, `is`(1))

        // first sensor value is humidity since temperature returns a "NaN" that we hidd
        val num = 0
        assertThat(listItems[num].title, `is`("39.5 %"))
        assertThat(listItems[num].summary, `is`("DHT: Feuchte"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_hygrometer))
    }

    @Test
    fun parseStatesHideNanSensorValues() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-nan.json"))

        val states = parser.parseStates(infoJson)
        assertThat(states, `is`(listOf(null)))
    }

    @Test
    fun parseInfoPressure() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-pressure.json"))

        val listItems = parser.parseResponse(infoJson)

        assertThat(listItems.size, `is`(3))
        // sensor 1: Temperature + Humidity + Pressure
        var num = 0
        assertThat(listItems[num].title, `is`("30 °C"))
        assertThat(listItems[num].summary, `is`("BMP_HWR: Temp_BMP"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_thermometer))

        num++
        assertThat(listItems[num].title, `is`("0 %"))
        assertThat(listItems[num].summary, `is`("BMP_HWR: Feuchte_BMP"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_hygrometer))

        num++
        assertThat(listItems[num].title, `is`("1004 hPa"))
        assertThat(listItems[num].summary, `is`("BMP_HWR: Druck_BMP"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_gauge))
    }

    @Test
    fun parseStatesPressure() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-pressure.json"))

        val states = parser.parseStates(infoJson)
        assertThat(states, `is`(listOf(null, null, null)))
    }
}
