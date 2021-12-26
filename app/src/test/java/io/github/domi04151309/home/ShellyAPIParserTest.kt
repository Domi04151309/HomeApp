package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.ShellyAPIParser
import org.junit.Assert
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ShellyAPIParserTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().applicationContext.resources
    private val parserV1 = ShellyAPIParser(resources, 1)
    private val parserV2 = ShellyAPIParser(resources, 2)

    @Test
    fun parseListItemsJsonV1_shellyPlug1WithPowerMeter() {
        val settingsJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-settings.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shellyplug1-status.json"))

        val listItems = parserV1.parseResponse(settingsJson, statusJson)
        Assert.assertEquals(2, listItems.size)

        var num = 0
        Assert.assertEquals("Wohnzimmer Gartenfenster", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.switch_summary_on), listItems[num].summary)
        Assert.assertEquals(true, listItems[num].state)
        Assert.assertEquals("0", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num = 1
        Assert.assertEquals("27.95 W", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.shelly_powermeter_summary), listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_lightning, listItems[num].icon)
    }

    @Test
    fun parseListItemsJsonV1_shelly1WithTemperatureNoRelayName() {
        val settingsJson = JSONObject(Helpers.getFileContents("/shelly/shelly1-settings.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shelly1-status.json"))

        val listItems = parserV1.parseResponse(settingsJson, statusJson)
        Assert.assertEquals(3, listItems.size)

        var num = 0
        Assert.assertEquals(resources.getString(R.string.shelly_switch_title, 1), listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.switch_summary_off), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("0", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num++
        Assert.assertEquals("23.0 °C", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.shelly_temperature_sensor_summary), listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_device_thermometer, listItems[num].icon)

        num++
        Assert.assertEquals("52.3%", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.shelly_humidity_sensor_summary), listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_humidity, listItems[num].icon)
    }

    @Test
    fun parseListItemsJsonV2_shellyPlus1() {
        val configJson = JSONObject(Helpers.getFileContents("/shelly/shelly-plus-1-Shelly.GetConfig.json"))
        val statusJson = JSONObject(Helpers.getFileContents("/shelly/shelly-plus-1-Shelly.GetStatus.json"))

        val listItems = parserV2.parseResponse(configJson, statusJson)
        Assert.assertEquals(1, listItems.size)

        val num = 0
        Assert.assertEquals("Kamin", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.switch_summary_on), listItems[num].summary)
        Assert.assertEquals(true, listItems[num].state)
        Assert.assertEquals("0", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)
    }
}