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
    fun parseListItemsJsonV1_shellyplug1() {
        val fileContent = javaClass.getResource("/shelly/shellyplug1-settings.json").readText()
        val settings = JSONObject(fileContent)

        val sa = ShellyAPIParser("http://shelly", resources)
        val listItems = sa.parseListItemsJsonV1(settings)
        Assert.assertEquals(1, listItems.size)
        Assert.assertEquals("Wohnzimmer Gartenfenster", listItems[0].title)
        Assert.assertEquals(resources.getString(R.string.shelly_switch_summary_on), listItems[0].summary)
        Assert.assertEquals(true, listItems[0].state)
        Assert.assertEquals(R.drawable.ic_do, listItems[0].icon)
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
        Assert.assertEquals(R.drawable.ic_do, listItems[0].icon)
    }
}