package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.EspEasyAPIParser
import org.junit.Assert
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EspEasyAPIParserTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().applicationContext.resources
    private val parser = EspEasyAPIParser(resources)

    @Test
    fun parseInfo1() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-1.json"))

        val listItems = parser.parseResponse(infoJson)
        Assert.assertEquals(4, listItems.size)

        //sensor 1: Temperature + Humidity
        var num = 0
        Assert.assertEquals("24.7 °C", listItems[num].title)
        Assert.assertEquals("DHT: Temperatur", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_device_thermometer, listItems[num].icon)

        num++
        Assert.assertEquals("39.5 %", listItems[num].title)
        Assert.assertEquals("DHT: Feuchte", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_humidity, listItems[num].icon)

        //sensor 2: Temperature only
        num++
        Assert.assertEquals("0 °C", listItems[num].title)
        Assert.assertEquals("DS: Temperatur", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_device_thermometer, listItems[num].icon)

        //sensor 3: Switch in off mode
        num++
        Assert.assertEquals("Relais", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.switch_summary_off), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("12", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)
    }

    @Test
    fun parseInfoDisabledTasks() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-disabledtasks.json"))

        val listItems = parser.parseResponse(infoJson)

        Assert.assertEquals(0, listItems.size)
    }

    @Test
    fun parseInfoHideNanSensorValues() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-nan.json"))

        val listItems = parser.parseResponse(infoJson)

        Assert.assertEquals(1, listItems.size)

        //first sensor value is humidity since temperature returns a "NaN" that we hidd
        val num = 0
        Assert.assertEquals("39.5 %", listItems[num].title)
        Assert.assertEquals("DHT: Feuchte", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_humidity, listItems[num].icon)
    }

    @Test
    fun parseInfoPressure() {
        val infoJson = JSONObject(Helpers.getFileContents("/espeasy/espeasy-pressure.json"))

        val listItems = parser.parseResponse(infoJson)

        Assert.assertEquals(3, listItems.size)
        //sensor 1: Temperature + Humidity + Pressure
        var num = 0
        Assert.assertEquals("30 °C", listItems[num].title)
        Assert.assertEquals("BMP_HWR: Temp_BMP", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_device_thermometer, listItems[num].icon)

        num++
        Assert.assertEquals("0 %", listItems[num].title)
        Assert.assertEquals("BMP_HWR: Feuchte_BMP", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_humidity, listItems[num].icon)

        num++
        Assert.assertEquals("1004 hPa", listItems[num].title)
        Assert.assertEquals("BMP_HWR: Druck_BMP", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_gauge, listItems[num].icon)
    }
}
