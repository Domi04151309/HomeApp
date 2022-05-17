package io.github.domi04151309.home.data

import org.json.JSONArray
import org.json.JSONObject

class LightStates {

    private val lights: MutableMap<String, Light> = mutableMapOf()

    fun addLight(id: String, state: JSONObject) {
        lights[id] = Light(
            state.optBoolean("on"),
            if (state.has("bri")) state.getInt("bri") else null,
            if (state.has("xy")) state.getJSONArray("xy") else null,
            if (state.has("ct")) state.getInt("ct") else null
        )
    }

    fun setSceneBrightness(bri: Int) {
        for (i in lights) {
            if (i.value.bri != null) i.value.bri = bri
        }
    }

    fun switchLight(id: String, on: Boolean) {
        lights[id]?.on = on
    }

    override fun toString(): String {
        val json = JSONObject()
        for ((key, value) in lights) {
            val light = JSONObject()
            light.put("on", value.on)
            if (value.bri != null) light.put("bri", value.bri)
            if (value.xy != null) light.put("xy", value.xy)
            else if (value.ct != null) light.put("ct", value.ct)
            json.put(key, light)
        }
        return json.toString()
    }

    data class Light(var on: Boolean, var bri: Int?, var xy: JSONArray?, var ct: Int?)
}