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
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import io.github.domi04151309.home.*
import io.github.domi04151309.home.adapters.HueSceneLampListAdapter
import io.github.domi04151309.home.custom.CustomJsonArrayRequest
import io.github.domi04151309.home.data.SimpleListItem
import io.github.domi04151309.home.fragments.HueScenesFragment
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.HueAPI
import io.github.domi04151309.home.helpers.HueUtils
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.helpers.Theme
import org.json.JSONObject

class HueSceneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.set(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hue_scene)

        val deviceId = intent.getStringExtra("deviceId") ?: ""
        val roomId = intent.getStringExtra("room") ?: "0"
        val hueAPI = HueAPI(this, deviceId)
        val address = Devices(this).getDeviceById(deviceId).address
        val queue = Volley.newRequestQueue(this)
        val listItems: ArrayList<SimpleListItem> = arrayListOf()
        val colorArray: ArrayList<Int> = arrayListOf()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<TextInputLayout>(R.id.nameBox)

        val roomDataRequest = JsonObjectRequest(Request.Method.GET, address + "api/" + hueAPI.getUsername() + "/groups/" + roomId, null,
                { response ->
                    val lights = response.getJSONArray("lights")
                    val lightsDataRequest = JsonObjectRequest(Request.Method.GET,  address + "api/" + hueAPI.getUsername() + "/lights", null,
                            { secondResponse ->
                                var lightObj: JSONObject
                                for (i in 0 until lights.length()) {
                                    lightObj = secondResponse.getJSONObject(lights.get(i).toString())
                                    val item = SimpleListItem(lightObj.getString("name"))
                                    val state = lightObj.getJSONObject("state")
                                    if (state.has("bri")) {
                                        item.summary = resources.getString(R.string.hue_brightness) + ": " + HueUtils.briToPercent(state.getInt("bri"))
                                    } else {
                                        item.summary = resources.getString(R.string.hue_brightness) + ": 100%"
                                    }
                                    colorArray += if (state.has("hue") && state.has("sat")) {
                                        HueUtils.hueSatToRGB(state.getInt("hue"), state.getInt("sat"))
                                    } else if (state.has("ct")) {
                                        HueUtils.ctToRGB(state.getInt("ct"))
                                    } else {
                                        Color.parseColor("#FFFFFF")
                                    }
                                    item.icon = R.drawable.ic_circle
                                    listItems += item
                                }

                                recyclerView.layoutManager = LinearLayoutManager(this)
                                recyclerView.adapter = HueSceneLampListAdapter(listItems, colorArray)
                            },
                            { error ->
                                Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
                            }
                    )
                    queue.add(lightsDataRequest)
                },
                { error ->
                    Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
                }
        )
        queue.add(roomDataRequest)

        nameBox.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val string = s.toString()
                if (string == "") nameTxt.text = resources.getString(R.string.hue_new_scene)
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
            val jsonRequestObject = JSONObject("{\"name\":\"$name\",\"recycle\":false,\"group\":\"$roomId\",\"type\":\"GroupScene\"}")
            val addSceneRequest = CustomJsonArrayRequest(Request.Method.POST, address + "api/" + hueAPI.getUsername() + "/scenes", jsonRequestObject,
                    {
                        HueScenesFragment.scenesChanged = true
                        finish()
                    },
                    { error ->
                        Toast.makeText(this, R.string.err, Toast.LENGTH_LONG).show()
                        Log.e(Global.LOG_TAG, error.toString())
                    }
            )
            queue.add(addSceneRequest)
        }
    }
}
