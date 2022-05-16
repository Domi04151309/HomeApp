package io.github.domi04151309.home.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.google.android.material.textfield.TextInputLayout
import io.github.domi04151309.home.*
import io.github.domi04151309.home.adapters.HueSceneLampListAdapter
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.fragments.HueScenesFragment
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.helpers.*
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme
import org.json.JSONArray
import org.json.JSONObject

class HueSceneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_scene)

        val deviceId = intent.getStringExtra("deviceId") ?: ""
        val addressPrefix = Devices(this).getDeviceById(deviceId).address +
                "api/" + HueAPI(this, deviceId).getUsername()
        val queue = Volley.newRequestQueue(this)
        val listItems: ArrayList<Pair<SimpleListItem, Int>> = arrayListOf()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<TextInputLayout>(R.id.nameBox)

        val editing = intent.hasExtra("scene")
        val id = intent.getStringExtra(if (editing) "scene" else "room")
        lateinit var defaultText: String

        recyclerView.layoutManager = LinearLayoutManager(this)

        if (editing) {
            supportActionBar?.setTitle(R.string.hue_edit_scene)
            defaultText = resources.getString(R.string.hue_scene)
            queue.add(
                JsonObjectRequest(
                    Request.Method.GET,
                    "$addressPrefix/scenes/$id",
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
                                    for (i in lights.keys()) {
                                        lightObj = lights.getJSONObject(i)
                                        listItems += Pair(
                                            generateListItem(
                                                (secondResponse.optJSONObject(i) ?: JSONObject())
                                                    .optString("name"), lightObj
                                            ),
                                            generateColor(lightObj)
                                        )
                                    }

                                    listItems.sortBy { it.first.title }
                                    recyclerView.adapter =
                                        HueSceneLampListAdapter(listItems)
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
                    "$addressPrefix/groups/$id",
                    null,
                    { response ->
                        val lights = response.getJSONArray("lights")
                        queue.add(
                            JsonObjectRequest(
                                Request.Method.GET,
                                "$addressPrefix/lights",
                                null,
                                { secondResponse ->
                                    var lightObj: JSONObject
                                    for (i in 0 until lights.length()) {
                                        lightObj =
                                            secondResponse.getJSONObject(lights.get(i).toString())
                                        val state = lightObj.getJSONObject("state")
                                        listItems += Pair(
                                            generateListItem(
                                                lightObj.getString("name"),
                                                state
                                            ),
                                            generateColor(state)
                                        )
                                    }

                                    listItems.sortBy { it.first.title }
                                    recyclerView.adapter =
                                        HueSceneLampListAdapter(listItems)
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
                        "$addressPrefix/scenes/$id",
                        JSONObject("{\"name\":\"$name\"}"),
                        ::onSuccess,
                        ::onError
                    )
                } else {
                    CustomJsonArrayRequest(
                        Request.Method.POST,
                        "$addressPrefix/scenes",
                        JSONObject("{\"name\":\"$name\",\"recycle\":false,\"group\":\"$id\",\"type\":\"GroupScene\"}"),
                        ::onSuccess,
                        ::onError
                    )
                }
            )
        }
    }

    private fun generateListItem(title: String, state: JSONObject): SimpleListItem {
        val item = SimpleListItem(title)
        item.summary =
            resources.getString(if (state.optBoolean("on")) R.string.str_on else R.string.str_off)
        item.summary += " · " + resources.getString(R.string.hue_brightness) + ": "
        if (state.has("bri"))
            item.summary += HueUtils.briToPercent(state.getInt("bri"))
        else
            item.summary += "100%"
        item.icon = R.drawable.ic_circle
        return item
    }

    private fun generateColor(state: JSONObject): Int {
        return if (state.has("xy")) {
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
    }

    private fun onSuccess(response: JSONArray) {
        HueScenesFragment.scenesChanged = true
        finish()
    }

    private fun onError(error: VolleyError) {
        Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
        Log.e(Global.LOG_TAG, error.toString())
    }
}
