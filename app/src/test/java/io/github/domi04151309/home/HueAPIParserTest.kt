package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.HueAPIParser
import org.junit.Assert
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HueAPIParserTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().applicationContext.resources
    private val parser = HueAPIParser(resources)

    @Test
    fun parseListItems_docs() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/docs-groups.json"))

        val listItems = parser.parseResponse(groupsJson)
        Assert.assertEquals(0, listItems.size)
    }

    @Test
    fun parseStates_docs() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/docs-groups.json"))

        val states = parser.parseStates(groupsJson)
        Assert.assertEquals(arrayListOf<Boolean?>(), states)
    }

    @Test
    fun parseListItems_home() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/home-groups.json"))

        val listItems = parser.parseResponse(groupsJson)
        Assert.assertEquals(9, listItems.size)

        var num = 0
        Assert.assertEquals("Bedroom", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("room#1", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_room, listItems[num].icon)

        num = 1
        Assert.assertEquals("Hallway", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("room#5", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_room, listItems[num].icon)

        num = 2
        Assert.assertEquals("Kitchen", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("room#7", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_room, listItems[num].icon)

        num = 3
        Assert.assertEquals("Living Room", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("room#2", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_room, listItems[num].icon)

        num = 4
        Assert.assertEquals("Office", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("room#6", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_room, listItems[num].icon)

        num = 5
        Assert.assertEquals("Unused", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("room#8", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_room, listItems[num].icon)

        num = 6
        Assert.assertEquals("Kitchen Cabinets", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("zone#9", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_zone, listItems[num].icon)

        num = 7
        Assert.assertEquals("Kitchen Ceiling", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("zone#3", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_zone, listItems[num].icon)

        num = 8
        Assert.assertEquals("Living Room Ambient", listItems[num].title)
        Assert.assertEquals(resources.getString(R.string.hue_tap), listItems[num].summary)
        Assert.assertEquals(false, listItems[num].state)
        Assert.assertEquals("zone#4", listItems[num].hidden)
        Assert.assertEquals(R.drawable.ic_zone, listItems[num].icon)
    }

    @Test
    fun parseStates_home() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/home-groups.json"))

        val states = parser.parseStates(groupsJson)
        Assert.assertEquals(arrayListOf(false, false, false, false, false, false, false, false, false), states)
    }
}