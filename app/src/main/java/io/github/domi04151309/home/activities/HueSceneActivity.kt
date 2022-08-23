package io.github.domi04151309.home.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import io.github.domi04151309.home.*
import io.github.domi04151309.home.adapters.HueSceneLampListAdapter
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.fragments.HueScenesFragment
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.LightStates
import io.github.domi04151309.home.data.SceneListItem
import io.github.domi04151309.home.fragments.HueColorSheet
import io.github.domi04151309.home.helpers.*
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme
import io.github.domi04151309.home.interfaces.HueAdvancedLampInterface
import io.github.domi04151309.home.interfaces.SceneRecyclerViewHelperInterface
import org.json.JSONArray
import org.json.JSONObject

class HueSceneActivity : AppCompatActivity(), SceneRecyclerViewHelperInterface, HueAdvancedLampInterface {

    private var editing: Boolean = false
    private val lightStates: LightStates = LightStates()
    private lateinit var hueAPI: HueAPI
    private lateinit var adapter: HueSceneLampListAdapter

    override var id: String = ""
    override var canReceiveRequest: Boolean = true
    override lateinit var device: DeviceItem
    override lateinit var addressPrefix: String

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_scene)

        device = Devices(this).getDeviceById(intent.getStringExtra("deviceId") ?: "")
        hueAPI = HueAPI(this, device.id)
        addressPrefix = device.address +
                "api/" + hueAPI.getUsername()
        val queue = Volley.newRequestQueue(this)
        val listItems: ArrayList<SceneListItem> = arrayListOf()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<TextInputLayout>(R.id.nameBox)
        val briBar = findViewById<Slider>(R.id.briBar)

        editing = intent.hasExtra("scene")
        adapter = HueSceneLampListAdapter(listItems, this)
        val groupId = intent.getStringExtra("room") ?: "0"
        val sceneId = intent.getStringExtra("scene") ?: ""
        lateinit var defaultText: String
        lateinit var lightIDs: JSONArray

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        briBar.setLabelFormatter { value: Float ->
            HueUtils.briToPercent(value.toInt())
        }

        if (editing) {
            supportActionBar?.setTitle(R.string.hue_edit_scene)
            defaultText = resources.getString(R.string.hue_scene)
            hueAPI.activateSceneOfGroup(groupId, sceneId)
            queue.add(
                JsonObjectRequest(
                    Request.Method.GET,
                    "$addressPrefix/scenes/$sceneId",
                    null,
                    { response ->
                        queue.add(
                            JsonObjectRequest(
                                Request.Method.GET,
                                "$addressPrefix/lights",
                                null,
                                { secondResponse ->
                                    nameBox.editText?.setText(response.optString("name"))
                                    lightIDs = response.optJSONArray("lights") ?: JSONArray()
                                    val lights =
                                        response.optJSONObject("lightstates") ?: JSONObject()
                                    var lightObj: JSONObject
                                    val brightness = Array(2) { 0 }
                                    for (i in lights.keys()) {
                                        lightObj = lights.getJSONObject(i)
                                        lightStates.addLight(i, lightObj)
                                        listItems += generateListItem(
                                            i,
                                            (secondResponse.optJSONObject(i) ?: JSONObject())
                                                .optString("name"),
                                            lightObj
                                        )
                                        if (lightObj.has("bri")) {
                                            brightness[0] += lightObj.getInt("bri")
                                            brightness[1]++
                                        }
                                    }

                                    SliderUtils.setProgress(
                                        briBar,
                                        if (brightness[1] > 0) brightness[0] / brightness[1] else 0
                                    )
                                    listItems.sortBy { it.title }
                                    adapter.notifyDataSetChanged()
                                },
                                ::onError
                            )
                        )
                    },
                    ::onError
                )
            )
        } else {
            supportActionBar?.setTitle(R.string.hue_add_scene)
            defaultText = resources.getString(R.string.hue_new_scene)
            queue.add(
                JsonObjectRequest(
                    Request.Method.GET,
                    "$addressPrefix/groups/$groupId",
                    null,
                    { response ->
                        lightIDs = response.getJSONArray("lights")
                        SliderUtils.setProgress(
                            briBar,
                            (response.optJSONObject("action") ?: JSONObject()).optInt("bri")
                        )
                        queue.add(
                            JsonObjectRequest(
                                Request.Method.GET,
                                "$addressPrefix/lights",
                                null,
                                { secondResponse ->
                                    var lightObj: JSONObject
                                    for (i in 0 until lightIDs.length()) {
                                        lightObj =
                                            secondResponse.getJSONObject(lightIDs.getString(i))
                                        val state = lightObj.getJSONObject("state")
                                        listItems += generateListItem(
                                            lightIDs.getString(i),
                                            lightObj.getString("name"),
                                            state
                                        )
                                    }

                                    listItems.sortBy { it.title }
                                    adapter.notifyDataSetChanged()
                                },
                                ::onError
                            )
                        )
                    },
                    ::onError
                )
            )
        }

        nameBox.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val string = s.toString()
                if (string == "") nameTxt.text = defaultText
                else nameTxt.text = string
            }
        })

        briBar.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}
            override fun onStopTrackingTouch(slider: Slider) {
                hueAPI.changeBrightnessOfGroup(groupId, slider.value.toInt())
                adapter.changeSceneBrightness(HueUtils.briToPercent(slider.value.toInt()))
                lightStates.setSceneBrightness(slider.value.toInt())
            }
        })

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val name = nameBox.editText?.text.toString()
            if (name == "") {
                AlertDialog.Builder(this)
                    .setTitle(R.string.err_missing_name)
                    .setMessage(R.string.err_missing_name_summary)
                    .setPositiveButton(android.R.string.ok) { _, _ -> }
                    .show()
                return@setOnClickListener
            }
            queue.add(
                if (editing) {
                    CustomJsonArrayRequest(
                        Request.Method.PUT,
                        "$addressPrefix/scenes/$sceneId",
                        JSONObject("{\"name\":\"$name\",\"lightstates\":$lightStates}"),
                        ::onSuccess,
                        ::onError
                    )
                } else {
                    CustomJsonArrayRequest(
                        Request.Method.POST,
                        "$addressPrefix/scenes",
                        JSONObject("{\"name\":\"$name\",\"recycle\":false,\"group\":\"$groupId\",\"type\":\"GroupScene\"}"),
                        ::onSuccess,
                        ::onError
                    )
                }
            )
        }
    }

    private fun generateListItem(id: String, title: String, state: JSONObject): SceneListItem {
        val item = SceneListItem(title)
        item.state = state.optBoolean("on")
        item.hidden = id
        item.brightness = HueUtils.briToPercent(state.optInt("bri", 255))
        item.color = if (state.has("xy")) {
            val xyArray = state.getJSONArray("xy")
            ColorUtils.xyToRGB(
                xyArray.getDouble(0),
                xyArray.getDouble(1)
            )
        } else if (state.has("hue") && state.has("sat")) {
            HueUtils.hueSatToRGB(state.getInt("hue"), state.getInt("sat"))
        } else if (state.has("ct")) {
            HueUtils.ctToRGB(state.getInt("ct"))
        } else {
            Color.parseColor("#FFFFFF")
        }
        return item
    }

    private fun onSuccess(response: JSONArray) {
        HueScenesFragment.scenesChanged = true
        finish()
    }

    private fun onError(error: VolleyError) {
        Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
        Log.e(Global.LOG_TAG, error.toString())
    }

    override fun onItemClicked(view: View, data: SceneListItem) {
        id = data.hidden
        HueColorSheet(this).show(supportFragmentManager, HueColorSheet::class.simpleName)
    }

    override fun onStateChanged(view: View, data: SceneListItem, state: Boolean) {
        hueAPI.switchLightByID(data.hidden, state)
        lightStates.switchLight(data.hidden, state)
    }

    override fun onColorChanged(color: Int) {
        adapter.updateColor(id, color)
    }

    override fun onBrightnessChanged(brightness: Int) {
        lightStates.setLightBrightness(id, brightness)
        adapter.updateBrightness(id, HueUtils.briToPercent(brightness))
    }

    override fun onHueSatChanged(hue: Int, sat: Int) {
        lightStates.setLightHue(id, hue)
        lightStates.setLightSat(id, sat)
    }

    override fun onCtChanged(ct: Int) {
        lightStates.setLightCt(id, ct)
    }
}
