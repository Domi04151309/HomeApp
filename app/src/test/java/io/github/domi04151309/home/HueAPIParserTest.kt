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
    fun parseListItems() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/groups.json"))

        val listItems = parser.parseResponse(groupsJson)
        Assert.assertEquals(0, listItems.size)
    }

    @Test
    fun parseStates() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/groups.json"))

        val states = parser.parseStates(groupsJson)
        Assert.assertEquals(arrayListOf<Boolean?>(), states)
    }
}