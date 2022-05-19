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
            null,
            null,
            if (state.has("ct")) state.getInt("ct") else null
        )
    }

    fun setSceneBrightness(bri: Int) {
        for (i in lights) {
            if (i.value.bri != null) i.value.bri = bri
        }
    }

    fun setLightBrightness(id: String, bri: Int) {
        lights[id]?.bri = bri
    }

    fun setLightHue(id: String, hue: Int) {
        lights[id]?.xy = null
        lights[id]?.hue = hue
    }

    fun setLightSat(id: String, sat: Int) {
        lights[id]?.xy = null
        lights[id]?.sat = sat
    }

    fun setLightCt(id: String, ct: Int) {
        lights[id]?.xy = null
        lights[id]?.ct = ct
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
            if (value.hue != null && value.sat != null) {
                light.put("hue", value.hue)
                light.put("sat", value.sat)
            }
            else if (value.ct != null) light.put("ct", value.ct)
            json.put(key, light)
        }
        return json.toString()
    }

    data class Light(
        var on: Boolean,
        var bri: Int?,
        var xy: JSONArray?,
        var hue: Int?,
        var sat: Int?,
        var ct: Int?
    )
}