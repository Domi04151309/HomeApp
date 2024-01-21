package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.SimpleHomeAPIParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Suppress("FunctionMaxLength")
class SimpleHomeAPIParserTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().applicationContext.resources
    private val parser = SimpleHomeAPIParser(resources, null)

    @Test
    fun parseListItems_TemperatureSensor() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/temperature-sensor-commands.json"))

        val listItems = parser.parseResponse(commandsJson)
        assertThat(listItems.size, `is`(2))

        var num = 0
        assertThat(listItems[num].title, `is`("Temperature"))
        assertThat(listItems[num].summary, `is`("It is currently 18.00°C in your room"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`("none@temperature"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_thermometer))

        num = 1
        assertThat(listItems[num].title, `is`("Humidity"))
        assertThat(listItems[num].summary, `is`("The humidity is 86.30%"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`("none@humidity"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_device_hygrometer))
    }

    @Test
    fun parseStates_TemperatureSensor() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/temperature-sensor-commands.json"))

        val states = parser.parseStates(commandsJson)
        assertThat(states, `is`(listOf(null, null)))
    }

    @Test
    fun parseListItems_TestServer() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/test-server-commands.json"))

        val listItems = parser.parseResponse(commandsJson)
        assertThat(listItems.size, `is`(5))

        var num = 0
        assertThat(listItems[num].title, `is`("Title of the command"))
        assertThat(listItems[num].summary, `is`("Summary of the command"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`("action@example"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))

        num = 1
        assertThat(listItems[num].title, `is`("Title of the command"))
        assertThat(listItems[num].summary, `is`("Mode: none"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`("none@example2"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))

        num = 2
        assertThat(listItems[num].title, `is`("Title of the command"))
        assertThat(listItems[num].summary, `is`("Mode: input"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`("input@example3"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))

        num = 3
        assertThat(listItems[num].title, `is`("Title of the command"))
        assertThat(listItems[num].summary, `is`("Mode: switch"))
        assertThat(listItems[num].state, `is`(true))
        assertThat(listItems[num].hidden, `is`("switch@example4"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))

        num = 4
        assertThat(listItems[num].title, `is`("1944518792"))
        assertThat(listItems[num].summary, `is`("523219119"))
        assertThat(listItems[num].state, `is`(null as Boolean?))
        assertThat(listItems[num].hidden, `is`("action@rand"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_do))
    }

    @Test
    fun parseStates_TestServer() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/test-server-commands.json"))

        val states = parser.parseStates(commandsJson)
        assertThat(states, `is`(listOf(null, null, null, true, null)))
    }
}
