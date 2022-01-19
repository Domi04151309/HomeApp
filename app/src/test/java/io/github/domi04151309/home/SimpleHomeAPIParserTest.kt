package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.SimpleHomeAPIParser
import org.junit.Assert
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SimpleHomeAPIParserTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().applicationContext.resources
    private val parser = SimpleHomeAPIParser(resources, null)

    @Test
    fun parseListItems_TemperatureSensor() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/temperature-sensor-commands.json"))

        val listItems = parser.parseResponse(commandsJson)
        Assert.assertEquals(2, listItems.size)

        var num = 0
        Assert.assertEquals("Temperature", listItems[num].title)
        Assert.assertEquals("It is currently 18.00°C in your room", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("none@temperature", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_device_thermometer, listItems[num].icon)

        num = 1
        Assert.assertEquals("Humidity", listItems[num].title)
        Assert.assertEquals("The humidity is 86.30%", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("none@humidity", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_device_hygrometer, listItems[num].icon)
    }

    @Test
    fun parseStates_TemperatureSensor() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/temperature-sensor-commands.json"))

        val states = parser.parseStates(commandsJson)
        Assert.assertEquals(arrayListOf(null, null), states)
    }

    @Test
    fun parseListItems_TestServer() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/test-server-commands.json"))

        val listItems = parser.parseResponse(commandsJson)
        Assert.assertEquals(5, listItems.size)

        var num = 0
        Assert.assertEquals("Title of the command", listItems[num].title)
        Assert.assertEquals("Summary of the command", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("action@example", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num = 1
        Assert.assertEquals("Title of the command", listItems[num].title)
        Assert.assertEquals("Mode: none", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("none@example2", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num = 2
        Assert.assertEquals("Title of the command", listItems[num].title)
        Assert.assertEquals("Mode: input", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("input@example3", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num = 3
        Assert.assertEquals("Title of the command", listItems[num].title)
        Assert.assertEquals("Mode: switch", listItems[num].summary)
        Assert.assertEquals(true, listItems[num].state)
        Assert.assertEquals("switch@example4", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num = 4
        Assert.assertEquals("1944518792", listItems[num].title)
        Assert.assertEquals("523219119", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("action@rand", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)
    }

    @Test
    fun parseStates_TestServer() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/test-server-commands.json"))

        val states = parser.parseStates(commandsJson)
        Assert.assertEquals(arrayListOf(null, null, null, true, null), states)
    }
}