package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.ShellyAPIParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Suppress("FunctionMaxLength")
class ShellyAPIParserTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().applicationContext.resources
    private val parserV1 = ShellyAPIParser(resources, 1)
    private val parserV2 = ShellyAPIParser(resources, 2)

    @Test
    fun parseListItemsJsonV1_shellyPlug1WithPowerMeter() {
        val settingsJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-settings.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-status.json"))

        val listItems = parserV1.parseResponse(settingsJson, statusJson)
        assertThat(listItems.size, `is`(2))

        var num = 0
        assertThat(listItems[num].title, `is`("Wohnzimmer Gartenfenster"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.switch_summary_on)))
        assertThat(listItems[num].state, `is`(true))
        assertThat(listItems[num].hidden, `is`("0"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))

        num = 1
        assertThat(listItems[num].title, `is`("27.95 W"))
        assertThat(
            listItems[num].summary,
            `is`(resources.getString(R.string.shelly_powermeter_summary)),
        )
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_electricity))
    }

    @Test
    fun parseStatesJsonV1_shellyPlug1WithPowerMeter() {
        val settingsJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-settings.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-status.json"))

        val states = parserV1.parseStates(settingsJson, statusJson)
        assertThat(states, `is`(listOf(true, null)))
    }

    @Test
    fun parseListItemsJsonV1_shellyPlug1ApplianceTypesForIcons() {
        val settingsJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-icons-settings.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-icons-status.json"))

        val listItems = parserV1.parseResponse(settingsJson, statusJson)
        assertThat(listItems.size, `is`(6))

        var num = 0
        assertThat(listItems[num].title, `is`("Deckenlampe"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_lamp))

        num = 1
        assertThat(listItems[num].title, `is`("Steckdose"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_socket))

        num = 2
        assertThat(listItems[num].title, `is`("Radiator"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_thermometer))

        num = 3
        assertThat(listItems[num].title, `is`("Stereoanlage"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_speaker))

        num = 4
        assertThat(listItems[num].title, `is`("Tannenbaum (en)"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_christmas_tree))

        num = 5
        assertThat(listItems[num].title, `is`("Schwibbogen"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_schwibbogen))
    }

    @Test
    fun parseListItemsJsonV1_shelly1WithTemperatureNoRelayName() {
        val settingsJson = JSONObject(Helpers.getFileContents("/shelly/shelly1-settings.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shelly1-status.json"))

        val listItems = parserV1.parseResponse(settingsJson, statusJson)
        assertThat(listItems.size, `is`(3))

        var num = 0
        assertThat(listItems[num].title, `is`(resources.getString(R.string.shelly_switch_title, 1)))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.switch_summary_off)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("0"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))

        num++
        assertThat(listItems[num].title, `is`("23.0 °C"))
        assertThat(
            listItems[num].summary,
            `is`(resources.getString(R.string.shelly_temperature_sensor_summary)),
        )
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_thermometer))

        num++
        assertThat(listItems[num].title, `is`("52.3%"))
        assertThat(
            listItems[num].summary,
            `is`(resources.getString(R.string.shelly_humidity_sensor_summary)),
        )
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`(""))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_hygrometer))
    }

    @Test
    fun parseStatesJsonV1_shelly1WithTemperatureNoRelayName() {
        val settingsJson = JSONObject(Helpers.getFileContents("/shelly/shelly1-settings.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shelly1-status.json"))

        val states = parserV1.parseStates(settingsJson, statusJson)
        assertThat(states, `is`(listOf(false, null, null)))
    }

    @Test
    fun parseListItemsJsonV2_shellyPlus1() {
        val configJson = JSONObject(Helpers.getFileContents("/shelly/shelly-plus-1-Shelly.GetConfig.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shelly-plus-1-Shelly.GetStatus.json"))

        val listItems = parserV2.parseResponse(configJson, statusJson)
        assertThat(listItems.size, `is`(1))

        val num = 0
        assertThat(listItems[num].title, `is`("Kamin"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.switch_summary_on)))
        assertThat(listItems[num].state, `is`(true))
        assertThat(listItems[num].hidden, `is`("0"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_lamp))
    }

    @Test
    fun parseStatesJsonV2_shellyPlus1() {
        val configJson = JSONObject(Helpers.getFileContents("/shelly/shelly-plus-1-Shelly.GetConfig.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shelly-plus-1-Shelly.GetStatus.json"))

        val states = parserV2.parseStates(configJson, statusJson)
        assertThat(states, `is`(listOf(true)))
    }
}
