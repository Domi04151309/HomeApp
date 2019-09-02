package io.github.domi04151309.home.hue

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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
import io.github.domi04151309.home.ListViewAdapter
import io.github.domi04151309.home.data.ListViewItem
import org.json.JSONObject
import java.lang.Exception


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
        var listItems: Array<ListViewItem> = arrayOf()
        var colorArray: Array<Int> = arrayOf()
        val listView = findViewById<ListView>(R.id.listView)
        val nameTxt = findViewById<TextView>(R.id.nameTxt)
        val nameBox = findViewById<EditText>(R.id.nameBox)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        val roomDataRequest = JsonObjectRequest(Request.Method.GET, address + "api/" + hueAPI.getUsername() + "/groups/" + roomId, null,
                Response.Listener { response ->
                    val lights = response.getJSONArray("lights")
                    for (i in 0 until lights.length()) {
                        val lightDataRequest = JsonObjectRequest(Request.Method.GET,  address + "api/" + hueAPI.getUsername() + "/lights/" + lights.get(i).toString(), null,
                                Response.Listener { secondResponse ->
                                    val item = ListViewItem(secondResponse.getString("name"))
                                    val state = secondResponse.getJSONObject("state")
                                    if (state.has("bri")) {
                                        item.summary = resources.getString(R.string.hue_brightness) + ": " + HueUtils.briToPercent(state.getString("bri").toInt())
                                    } else {
                                        item.summary = resources.getString(R.string.hue_brightness) + ": 0%"
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
                                },
                                Response.ErrorListener { error ->
                                    Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
                                }
                        )
                        queue.add(lightDataRequest)
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, Global.volleyError(this, error), Toast.LENGTH_LONG).show()
                }
        )
        queue.add(roomDataRequest)

        try {
            Handler().postDelayed({
                val adapter = ListViewAdapter(this, listItems, false)
                listView.adapter = adapter
                Handler().postDelayed({
                    for (i in colorArray.indices)
                        ImageViewCompat.setImageTintList(listView.getChildAt(i).findViewById(R.id.drawable), ColorStateList.valueOf(colorArray[i]))
                }, 200)
            }, 200)
        } catch (e: Exception) { }

        nameBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val string = s.toString()
                if (string == "") nameTxt.text = resources.getString(R.string.hue_new_scene)
                else nameTxt.text = string
            }
        })

        fab.setOnClickListener {
            val name = nameBox.text.toString()
            if (name == "") {
                AlertDialog.Builder(this)
                        .setTitle(resources.getString(R.string.err_missing_name))
                        .setMessage(resources.getString(R.string.err_missing_name_summary))
                        .setPositiveButton(resources.getString(android.R.string.ok)) { _, _ -> }
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
