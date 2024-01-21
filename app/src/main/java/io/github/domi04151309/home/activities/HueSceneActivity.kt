package io.github.domi04151309.home.activities

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import io.github.domi04151309.home.R
import io.github.domi04151309.home.adapters.HueSceneLampListAdapter
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.data.LightStates
import io.github.domi04151309.home.data.SceneListItem
import io.github.domi04151309.home.fragments.HueColorSheet
import io.github.domi04151309.home.fragments.HueScenesFragment
import io.github.domi04151309.home.helpers.ColorUtils
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.HueUtils.MAX_BRIGHTNESS
import io.github.domi04151309.home.helpers.SliderUtils
import io.github.domi04151309.home.interfaces.HueAdvancedLampInterface
import io.github.domi04151309.home.interfaces.SceneRecyclerViewHelperInterface
import org.json.JSONArray
import org.json.JSONObject

@Suppress("TooManyFunctions")
class HueSceneActivity :
    BaseActivity(),
    SceneRecyclerViewHelperInterface,
    HueAdvancedLampInterface,
    Response.Listener<JSONArray>,
    Response.ErrorListener {
    private var editing = false
    private val lightStates = LightStates()
    private val listItems = mutableListOf<SceneListItem>()
    private var groupId = "0"
    private var sceneId = ""
    private var defaultText = ""
    private lateinit var hueAPI: HueAPI
    private lateinit var adapter: HueSceneLampListAdapter
    private lateinit var queue: RequestQueue
    private lateinit var nameBox: TextInputLayout
    private lateinit var briBar: Slider

    override var id: String = ""
    override var canReceiveRequest: Boolean = true
    override lateinit var device: DeviceItem
    override lateinit var addressPrefix: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_scene)

        val nameTxt = findViewById<TextView>(R.id.nameTxt)

        device = Devices(this).getDeviceById(intent.getStringExtra("deviceId") ?: "")
        hueAPI = HueAPI(this, device.id)
        addressPrefix = device.address +
            "api/" + hueAPI.getUsername()
        queue = Volley.newRequestQueue(this)
        nameBox = findViewById(R.id.nameBox)
        briBar = findViewById(R.id.briBar)

        editing = intent.hasExtra("scene")
        adapter = HueSceneLampListAdapter(listItems, this)
        groupId = intent.getStringExtra("room") ?: "0"
        sceneId = intent.getStringExtra("scene") ?: ""

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@HueSceneActivity)
            adapter = this@HueSceneActivity.adapter
        }
        briBar.setLabelFormatter { value: Float ->
            HueUtils.briToPercent(value.toInt())
        }

        if (editing) {
            onEditScene()
        } else {
            onCreateScene()
        }

        nameBox.editText?.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable) {
                    // Do nothing.
                }

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                    // Do nothing.
                }

                override fun onTextChanged(
                    s: CharSequence,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                    val string = s.toString()
                    if (string == "") {
                        nameTxt.text = defaultText
                    } else {
                        nameTxt.text = string
                    }
                }
            },
        )

        briBar.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    // Do nothing.
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    hueAPI.changeBrightnessOfGroup(groupId, slider.value.toInt())
                    adapter.changeSceneBrightness(HueUtils.briToPercent(slider.value.toInt()))
                    lightStates.setSceneBrightness(slider.value.toInt())
                }
            },
        )

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            onFloatingActionButtnClicked()
        }
    }

    private fun onEditScene() {
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
                                val lights =
                                    response.optJSONObject("lightstates") ?: JSONObject()
                                var lightObj: JSONObject
                                val brightness = Array(2) { 0 }
                                for (i in lights.keys()) {
                                    lightObj = lights.getJSONObject(i)
                                    lightStates.addLight(i, lightObj)
                                    listItems +=
                                        generateListItem(
                                            i,
                                            (secondResponse.optJSONObject(i) ?: JSONObject())
                                                .optString("name"),
                                            lightObj,
                                        )
                                    if (lightObj.has("bri")) {
                                        brightness[0] += lightObj.getInt("bri")
                                        brightness[1]++
                                    }
                                }

                                SliderUtils.setProgress(
                                    briBar,
                                    if (brightness[1] > 0) brightness[0] / brightness[1] else 0,
                                )
                                listItems.sortBy { it.title }
                                adapter.notifyDataSetChanged()
                            },
                            this,
                        ),
                    )
                },
                this,
            ),
        )
    }

    private fun onCreateScene() {
        supportActionBar?.setTitle(R.string.hue_add_scene)
        defaultText = resources.getString(R.string.hue_new_scene)
        queue.add(
            JsonObjectRequest(
                Request.Method.GET,
                "$addressPrefix/groups/$groupId",
                null,
                { response ->
                    val lightIds = response.getJSONArray("lights")
                    SliderUtils.setProgress(
                        briBar,
                        (response.optJSONObject("action") ?: JSONObject()).optInt("bri"),
                    )
                    queue.add(
                        JsonObjectRequest(
                            Request.Method.GET,
                            "$addressPrefix/lights",
                            null,
                            { secondResponse ->
                                var lightObj: JSONObject
                                for (i in 0 until lightIds.length()) {
                                    lightObj =
                                        secondResponse.getJSONObject(lightIds.getString(i))
                                    val state = lightObj.getJSONObject("state")
                                    listItems +=
                                        generateListItem(
                                            lightIds.getString(i),
                                            lightObj.getString("name"),
                                            state,
                                        )
                                }

                                listItems.sortBy { it.title }
                                adapter.notifyDataSetChanged()
                            },
                            this,
                        ),
                    )
                },
                this,
            ),
        )
    }

    private fun generateListItem(
        id: String,
        title: String,
        state: JSONObject,
    ): SceneListItem =
        SceneListItem(
            title,
            id,
            state.optBoolean("on"),
            HueUtils.briToPercent(state.optInt("bri", MAX_BRIGHTNESS)),
            if (state.has("xy")) {
                val xyArray = state.getJSONArray("xy")
                ColorUtils.xyToRGB(
                    xyArray.getDouble(0),
                    xyArray.getDouble(1),
                )
            } else if (state.has("hue") && state.has("sat")) {
                HueUtils.hueSatToRGB(state.getInt("hue"), state.getInt("sat"))
            } else if (state.has("ct")) {
                HueUtils.ctToRGB(state.getInt("ct"))
            } else {
                Color.parseColor("#FFFFFF")
            },
        )

    private fun onFloatingActionButtnClicked() {
        val name = nameBox.editText?.text.toString()
        if (name == "") {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.err_missing_name)
                .setMessage(R.string.err_missing_name_summary)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .show()
            return
        }
        queue.add(
            if (editing) {
                CustomJsonArrayRequest(
                    Request.Method.PUT,
                    "$addressPrefix/scenes/$sceneId",
                    JSONObject("""{ "name": "$name", "lightstates": $lightStates }"""),
                    this,
                    this,
                )
            } else {
                CustomJsonArrayRequest(
                    Request.Method.POST,
                    "$addressPrefix/scenes",
                    JSONObject(
                        """{ "name": "$name", "recycle": false, "group": "$groupId", "type": "GroupScene" }""",
                    ),
                    this,
                    this,
                )
            },
        )
    }

    override fun onResponse(response: JSONArray) {
        HueScenesFragment.scenesChanged = true
        finish()
    }

    override fun onErrorResponse(error: VolleyError) {
        Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
        Log.e(Global.LOG_TAG, error.toString())
    }

    override fun onItemClicked(
        view: View,
        data: SceneListItem,
    ) {
        id = data.hidden
        HueColorSheet(this).show(supportFragmentManager, HueColorSheet::class.simpleName)
    }

    override fun onStateChanged(
        view: View,
        data: SceneListItem,
        state: Boolean,
    ) {
        hueAPI.switchLightById(data.hidden, state)
        lightStates.switchLight(data.hidden, state)
    }

    override fun onColorChanged(color: Int) {
        adapter.updateColor(id, color)
    }

    override fun onBrightnessChanged(brightness: Int) {
        lightStates.setLightBrightness(id, brightness)
        adapter.updateBrightness(id, HueUtils.briToPercent(brightness))
    }

    override fun onHueSatChanged(
        hue: Int,
        sat: Int,
    ) {
        lightStates.setLightHue(id, hue)
        lightStates.setLightSat(id, sat)
    }

    override fun onCtChanged(ct: Int) {
        lightStates.setLightCt(id, ct)
    }
}
