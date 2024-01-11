package io.github.domi04151309.home

import android.content.res.Resources
import io.github.domi04151309.home.api.HueAPIParser
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
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
        assertThat(listItems.size, `is`(0))
    }

    @Test
    fun parseStates_docs() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/docs-groups.json"))

        val states = parser.parseStates(groupsJson)
        assertThat(states, `is`(listOf()))
    }

    @Test
    fun parseListItems_home() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/home-groups.json"))

        val listItems = parser.parseResponse(groupsJson)
        assertThat(listItems.size, `is`(9))

        var num = 0
        assertThat(listItems[num].title, `is`("Bedroom"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("1"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_room))

        num = 1
        assertThat(listItems[num].title, `is`("Hallway"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("5"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_room))

        num = 2
        assertThat(listItems[num].title, `is`("Kitchen"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("7"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_room))

        num = 3
        assertThat(listItems[num].title, `is`("Living Room"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("2"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_room))

        num = 4
        assertThat(listItems[num].title, `is`("Office"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("6"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_room))

        num = 5
        assertThat(listItems[num].title, `is`("Unused"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("8"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_room))

        num = 6
        assertThat(listItems[num].title, `is`("Kitchen Cabinets"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("9"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_zone))

        num = 7
        assertThat(listItems[num].title, `is`("Kitchen Ceiling"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("3"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_zone))

        num = 8
        assertThat(listItems[num].title, `is`("Living Room Ambient"))
        assertThat(listItems[num].summary, `is`(resources.getString(R.string.hue_tap)))
        assertThat(listItems[num].state, `is`(false))
        assertThat(listItems[num].hidden, `is`("4"))
        assertThat(listItems[num].icon, `is`(R.drawable.ic_zone))
    }

    @Test
    fun parseStates_home() {
        val groupsJson = JSONObject(Helpers.getFileContents("/hue/home-groups.json"))

        val states = parser.parseStates(groupsJson)
        assertThat(
            states,
            `is`(
                listOf(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                ),
            ),
        )
    }
}
