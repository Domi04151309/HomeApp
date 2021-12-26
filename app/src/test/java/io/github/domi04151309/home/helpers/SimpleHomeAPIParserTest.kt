package io.github.domi04151309.home.helpers

import android.content.res.Resources
import io.github.domi04151309.home.R
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
    private val parser = SimpleHomeAPIParser(resources)

    @Test
    fun parseListItems() {
        val commandsJson = JSONObject(Helpers.getFileContents("/simplehome/commands.json"))

        val listItems = parser.parseResponse(commandsJson)
        Assert.assertEquals(2, listItems.size)

        var num = 0
        Assert.assertEquals("Temperature", listItems[num].title)
        Assert.assertEquals("It is currently 18.00°C in your room", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("temperature", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)

        num = 1
        Assert.assertEquals("Humidity", listItems[num].title)
        Assert.assertEquals("The humidity is 86.30%", listItems[num].summary)
        Assert.assertEquals(null, listItems[num].state)
        Assert.assertEquals("humidity", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_do, listItems[num].icon)
    }
}