package io.github.domi04151309.home.helpers

import android.content.res.Resources
import io.github.domi04151309.home.R
import org.junit.Assert
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ShellyAPIParserTest {
    var resources = RuntimeEnvironment.getApplication().applicationContext.resources

    @Test
    fun parseListItemsJsonV1_shellyplug1WithPowermeter() {
        val settingsJson = JSONObject(javaClass.getResource("/shelly/shellyplug1-settings.json").readText())
        val statusJson = JSONObject(javaClass.getResource("/shelly/shellyplug1-status.json").readText())

        val sa = ShellyAPIParser("http://shelly", resources)
        val listItems = sa.parseListItemsJsonV1(settingsJson, statusJson)
        Assert.assertEquals(2, listItems.size)

        Assert.assertEquals("Wohnzimmer Gartenfenster", listItems[0].title)
        Assert.assertEquals(resources.getString(R.string.shelly_switch_summary_on), listItems[0].summary)
        Assert.assertEquals(true, listItems[0].state)
        Assert.assertEquals("0", listItems[0].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[0].icon)

        Assert.assertEquals("27.95 W", listItems[1].title)
        Assert.assertEquals(resources.getString(R.string.shelly_powermeter_summary), listItems[1].summary)
        Assert.assertEquals(null, listItems[1].state)
        Assert.assertEquals("", listItems[1].hidden)
        Assert.assertEquals(R.drawable.ic_lightning, listItems[1].icon)
    }

    @Test
    fun parseListItemsJsonV1_shelly1WithTemperature() {
        val settingsJson = JSONObject(javaClass.getResource("/shelly/shellyplug1-settings.json").readText())//I didn't have a shelly1 settings file :(
        val statusJson = JSONObject(javaClass.getResource("/shelly/shelly1-status.json").readText())

        val sa = ShellyAPIParser("http://shelly", resources)
        val listItems = sa.parseListItemsJsonV1(settingsJson, statusJson)
        Assert.assertEquals(4, listItems.size)

        var num = 0
        Assert.assertEquals("Wohnzimmer Gartenfenster", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.shelly_switch_summary_on), listItems[num].summary)
        Assert.assertEquals(true, listItems[num].state)
        Assert.assertEquals("0", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num = 1
        Assert.assertEquals("0.0 W", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.shelly_powermeter_summary), listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_lightning, listItems[num].icon)

        num = 2
        Assert.assertEquals("23.0°C", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.shelly_temperature_sensor_summary), listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_device_thermometer, listItems[num].icon)

        num = 3
        Assert.assertEquals("52.3%", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.shelly_humidity_sensor_summary), listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_humidity, listItems[num].icon)
    }

    @Test
    fun parseListItemsJsonV2_shellyplus1() {
        val configJson = JSONObject(javaClass.getResource("/shelly/shelly-plus-1-Shelly.GetConfig.json").readText())
        val statusJson = JSONObject(javaClass.getResource("/shelly/shelly-plus-1-Shelly.GetStatus.json").readText())

        val sa = ShellyAPIParser("http://shelly", resources)
        val listItems = sa.parseListItemsJsonV2(configJson, statusJson)
        Assert.assertEquals(1, listItems.size)
        Assert.assertEquals("Kamin", listItems[0].title)
        Assert.assertEquals(resources.getString(R.string.shelly_switch_summary_on), listItems[0].summary)
        Assert.assertEquals(true, listItems[0].state)
        Assert.assertEquals("0", listItems[0].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[0].icon)
    }
}