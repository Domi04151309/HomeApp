package io.github.domi04151309.home.hue

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.ImageViewCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.domi04151309.home.*
import io.github.domi04151309.home.helpers.ListViewAdapter
import io.github.domi04151309.home.data.ListViewItem
import io.github.domi04151309.home.helpers.CustomJsonArrayRequest
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.objects.Global
import io.github.domi04151309.home.objects.Theme
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
        val listItems: ArrayList<ListViewItem> = arrayListOf()
        val colorArray: ArrayList<Int> = arrayListOf()
        val listView = findViewById<ListView>(R.id.listView)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<EditText>(R.id.nameBox)

        val roomDataRequest = JsonObjectRequest(Request.Method.GET, address + "api/" + hueAPI.getUsername() + "/groups/" + roomId, null,
                Response.Listener { response ->
                    val lights = response.getJSONArray("lights")
                    val lightsDataRequest = JsonObjectRequest(Request.Method.GET,  address + "api/" + hueAPI.getUsername() + "/lights", null,
                            Response.Listener { secondResponse ->
                                var lightObj: JSONObject
                                for (i in 0 until lights.length()) {
                                    lightObj = secondResponse.getJSONObject(lights.get(i).toString())
                                    val item = ListViewItem(lightObj.getString("name"))
                                    val state = lightObj.getJSONObject("state")
                                    if (state.has("bri")) {
                                        item.summary = resources.getString(R.string.hue_brightness) + ": " + HueUtils.briToPercent(state.getString("bri").toInt())
                                    } else {
                                        item.summary = resources.getString(R.string.hue_brightness) + ": 100%"
                                    }
                                    colorArray += if (state.has("hue") && state.has("sat")) {
                                        HueUtils.hueSatToRGB(state.getString("hue").toInt(), state.getString("sat").toInt())
                                    } else if (state.has("ct")) {
                                        HueUtils.ctToRGB(state.getString("ct").toInt())
                                    } else {
                                        Color.parseColor("#FFFFFF")
                                    }
                                    item.icon = R.drawable.ic_circle
                                    listItems += item
                                }
                                listView.adapter = ListViewAdapter(this, listItems, false)
                            },
                            Response.ErrorListener { error ->
                                Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
                            }
                    )
                    queue.add(lightsDataRequest)
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
                }
        )
        queue.add(roomDataRequest)

        listView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            for (i in 0 until listView.count)
                ImageViewCompat.setImageTintList(listView.getChildAt(i).findViewById(R.id.drawable), ColorStateList.valueOf(colorArray[i]))
        }

        nameBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val string = s.toString()
                if (string == "") nameTxt.text = resources.getString(R.string.hue_new_scene)
                else nameTxt.text = string
            }
        })

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val name = nameBox.text.toString()
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
                    Response.Listener { finish() },
                    Response.ErrorListener { error ->
                        Toast.makeText(this, R.string.err, Toast.LENGTH_LONG).show()
                        Log.e(Global.LOG_TAG, error.toString())
                    }
            )
            queue.add(addSceneRequest)
        }
    }
}
